package io.github.moxisuki.blockprint.cat.data.community

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
class CommunityConfigManagerTest {
    private fun newManager(): Pair<Context, CommunityConfigManager> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("community_config", Context.MODE_PRIVATE)
            .edit().clear().commit()
        return ctx to CommunityConfigManager(ctx)
    }

    @Test
    fun `default enabled is true`() {
        val (_, mgr) = newManager()
        assertTrue(mgr.enabled.value)
    }

    @Test
    fun `setEnabled false updates flow and persists`() {
        val (ctx, mgr) = newManager()
        mgr.setEnabled(false)
        assertEquals(false, mgr.enabled.value)
        val restored = CommunityConfigManager(ctx)
        assertEquals(false, restored.enabled.value)
    }

    @Test
    fun `setEnabled true after false toggles back`() {
        val (ctx, mgr) = newManager()
        mgr.setEnabled(false)
        mgr.setEnabled(true)
        assertEquals(true, mgr.enabled.value)
        val restored = CommunityConfigManager(ctx)
        assertEquals(true, restored.enabled.value)
    }
}
