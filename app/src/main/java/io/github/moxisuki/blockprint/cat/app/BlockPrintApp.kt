package io.github.moxisuki.blockprint.cat.app

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.moxisuki.blockprint.cat.app.core.design.AppTheme
import io.github.moxisuki.blockprint.cat.app.feature.terms.TermsGate
import io.github.moxisuki.blockprint.cat.app.shell.AppShell

@Composable
fun BlockPrintApp() {
    val context = LocalContext.current
    val application = context.applicationContext as BlockPrintCatApplication
    var termsAccepted by remember {
        mutableStateOf(application.termsAcceptance.isAccepted())
    }

    AppTheme {
        if (termsAccepted) {
            AppShell()
        } else {
            TermsGate(
                onAccepted = {
                    application.termsAcceptance.accept()
                    application.initBuglyIfConsented()
                    termsAccepted = true
                },
                onExit = {
                    (context as? Activity)?.finishAffinity()
                },
            )
        }
    }
}
