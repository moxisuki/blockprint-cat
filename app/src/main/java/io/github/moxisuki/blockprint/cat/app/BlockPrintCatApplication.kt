package io.github.moxisuki.blockprint.cat.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import javax.inject.Inject

@HiltAndroidApp
class BlockPrintCatApplication : Application() {

    @Inject
    lateinit var languageManager: AppLanguageManager

    override fun onCreate() {
        super.onCreate()
        languageManager.applyLanguage()
    }
}
