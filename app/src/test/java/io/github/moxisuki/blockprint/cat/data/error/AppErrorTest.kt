package io.github.moxisuki.blockprint.cat.data.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import io.github.moxisuki.blockprint.cat.data.community.CmsCloudflareException

class AppErrorTest {
    @Test
    fun `toAppError maps UnknownHostException to Network`() {
        val err = AppError.toAppError(UnknownHostException("nope"))
        assertTrue(err is AppError.Network)
        assertTrue((err as AppError.Network).cause is UnknownHostException)
    }

    @Test
    fun `toAppError maps SocketTimeoutException to Timeout`() {
        val err = AppError.toAppError(SocketTimeoutException("slow"))
        assertTrue(err is AppError.Timeout)
    }

    @Test
    fun `toAppError maps CmsCloudflareException to CmsCloudflare`() {
        val err = AppError.toAppError(CmsCloudflareException("blocked"))
        assertTrue(err is AppError.CmsCloudflare)
    }

    @Test
    fun `toAppError maps unknown exception to Unknown with message`() {
        val err = AppError.toAppError(IllegalStateException("oops"))
        assertTrue(err is AppError.Unknown)
        assertEquals("oops", (err as AppError.Unknown).message)
    }
}
