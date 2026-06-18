package io.github.moxisuki.blockprint.cat.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LanguageManagerTest {
    private fun newManager(): Pair<Context, LanguageManager> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("language_pref", Context.MODE_PRIVATE)
            .edit().clear().commit()
        return ctx to LanguageManager(ctx)
    }

    @Test
    fun `default mode is SYSTEM`() {
        val (_, lm) = newManager()
        assertEquals(LanguageManager.Mode.SYSTEM, lm.mode.value)
    }

    @Test
    fun `setMode EN persists and updates flow`() {
        val (ctx, lm) = newManager()
        lm.setMode(LanguageManager.Mode.EN)
        assertEquals(LanguageManager.Mode.EN, lm.mode.value)
        val restored = LanguageManager(ctx)
        assertEquals(LanguageManager.Mode.EN, restored.mode.value)
    }

    @Test
    fun `setMode ZH_CN persists`() {
        val (ctx, lm) = newManager()
        lm.setMode(LanguageManager.Mode.ZH_CN)
        val restored = LanguageManager(ctx)
        assertEquals(LanguageManager.Mode.ZH_CN, restored.mode.value)
    }
}
