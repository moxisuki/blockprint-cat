package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * 关于 → 更多 → 使用条款 的展示页。
 * 文案与 TermsGate 共享 TermsContent resource IDs。
 */
@Composable
fun TermsScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                termsIntro(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            termsSections().forEachIndexed { index, section ->
                if (index > 0) TermsSectionDivider()
                TermsSectionTitle(section.title)
                TermsSectionBody(section.body)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                termsLastUpdated(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
