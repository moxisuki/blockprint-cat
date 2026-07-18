package io.github.moxisuki.blockprint.cat.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.moxisuki.blockprint.cat.app.core.design.AppTheme
import io.github.moxisuki.blockprint.cat.app.shell.AppShell

@Composable
fun BlockPrintApp() {
    AppTheme {
        AppShell()
    }
}

