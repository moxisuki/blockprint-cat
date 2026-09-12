package io.github.moxisuki.blockprint.cat.app

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport
import dagger.hilt.android.HiltAndroidApp
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import io.github.moxisuki.blockprint.cat.app.core.privacy.TermsAcceptance
import io.github.moxisuki.blockprint.cat.BuildConfig
import javax.inject.Inject

@HiltAndroidApp
class BlockPrintCatApplication : Application() {

    @Inject
    lateinit var languageManager: AppLanguageManager

    @Inject
    lateinit var termsAcceptance: TermsAcceptance

    override fun onCreate() {
        super.onCreate()
        languageManager.applyLanguage()
        initBuglyIfConsented()
    }

    /**
     * Bugly must not start collecting diagnostics before the user accepts the terms.
     */
    fun initBuglyIfConsented() {
        if (BuildConfig.BUGLY_APP_ID.isNotBlank() && termsAcceptance.isAccepted()) {
            CrashReport.initCrashReport(this, BuildConfig.BUGLY_APP_ID, BuildConfig.DEBUG)
        }
    }
}
