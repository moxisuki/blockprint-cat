package io.github.moxisuki.blockprint.cat.data.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrPayloadTest {

    @Test
    fun `valid URI parses to QrConnection`() {
        val result = parseQrPayload("blockprintcat://192.168.1.10:18080/ws?token=abc123")
        assertNotNull(result)
        assertEquals(QrConnection("192.168.1.10", 18080, "abc123"), result)
    }

    @Test
    fun `valid URI with leading and trailing whitespace trims`() {
        val result = parseQrPayload("  blockprintcat://10.0.0.1:51234/ws?token=tok  ")
        assertNotNull(result)
        assertEquals("10.0.0.1", result!!.host)
        assertEquals(51234, result.port)
        assertEquals("tok", result.token)
    }

    @Test
    fun `hostname instead of IP is accepted`() {
        val result = parseQrPayload("blockprintcat://my-pc.local:18080/ws?token=tok")
        assertNotNull(result)
        assertEquals("my-pc.local", result!!.host)
    }

    @Test
    fun `long token is accepted`() {
        val result = parseQrPayload("blockprintcat://h:1/ws?token=abcdefghijklmnopqrstuvwxyz0123456789")
        assertNotNull(result)
        assertEquals("abcdefghijklmnopqrstuvwxyz0123456789", result!!.token)
    }

    @Test
    fun `wrong scheme returns null`() {
        assertNull(parseQrPayload("https://example.com/ws?token=tok"))
    }

    @Test
    fun `missing port returns null`() {
        assertNull(parseQrPayload("blockprintcat://192.168.1.10/ws?token=tok"))
    }

    @Test
    fun `non-numeric port returns null`() {
        assertNull(parseQrPayload("blockprintcat://192.168.1.10:abc/ws?token=tok"))
    }

    @Test
    fun `port out of range returns null`() {
        assertNull(parseQrPayload("blockprintcat://h:99999/ws?token=tok"))
        assertNull(parseQrPayload("blockprintcat://h:0/ws?token=tok"))
    }

    @Test
    fun `missing token returns null`() {
        assertNull(parseQrPayload("blockprintcat://192.168.1.10:18080/ws"))
    }

    @Test
    fun `empty host returns null`() {
        assertNull(parseQrPayload("blockprintcat://:18080/ws?token=tok"))
    }

    @Test
    fun `empty token returns null`() {
        assertNull(parseQrPayload("blockprintcat://h:1/ws?token="))
    }

    @Test
    fun `empty input returns null`() {
        assertNull(parseQrPayload(""))
    }

    @Test
    fun `arbitrary text returns null`() {
        assertNull(parseQrPayload("hello world"))
    }
}