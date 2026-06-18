package io.github.moxisuki.blockprint.cat.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppIconManagerTest {
    private fun newManager(): Pair<Context, AppIconManager> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("app_icon", Context.MODE_PRIVATE)
            .edit().clear().commit()
        return ctx to AppIconManager(ctx)
    }

    @Test
    fun `default current is v1 variant`() {
        val (_, mgr) = newManager()
        assertEquals("v1", mgr.current.value)
    }

    @Test
    fun `variants contains v1 v2 v3 and v4`() {
        val (_, mgr) = newManager()
        val ids = mgr.variants.map { it.id }
        assertTrue("v1" in ids)
        assertTrue("v2" in ids)
        assertTrue("v3" in ids)
        assertTrue("v4" in ids)
    }

    @Test
    fun `apply persists selected variant`() {
        val (ctx, mgr) = newManager()
        mgr.apply("v2")
        assertEquals("v2", mgr.current.value)
        val restored = AppIconManager(ctx)
        assertEquals("v2", restored.current.value)
    }

    @Test
    fun `stored legacy id migrates to v1v2v3`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("app_icon", Context.MODE_PRIVATE)
            .edit().putString("variant", "blue").commit()
        val mgr = AppIconManager(ctx)
        assertEquals("v2", mgr.current.value)
        // After read, the persisted value should be the new id, not the legacy one
        val stored = ctx.getSharedPreferences("app_icon", Context.MODE_PRIVATE)
            .getString("variant", null)
        assertEquals("v2", stored)
    }

    @Test
    fun `apply unknown variant throws`() {
        val (_, mgr) = newManager()
        try {
            mgr.apply("nope")
            assert(false) { "expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // ok
        }
    }
}