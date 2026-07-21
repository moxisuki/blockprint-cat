package io.github.moxisuki.blockprint.cat.app

import androidx.compose.runtime.Composable
import io.github.moxisuki.blockprint.cat.app.core.design.AppTheme
import io.github.moxisuki.blockprint.cat.app.shell.AppShell

@Composable
fun BlockPrintApp() {
    AppTheme {
        AppShell()
    }
}
