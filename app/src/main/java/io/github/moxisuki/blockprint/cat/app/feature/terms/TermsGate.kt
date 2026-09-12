package io.github.moxisuki.blockprint.cat.app.feature.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun TermsGate(
    onAccepted: () -> Unit,
    onExit: () -> Unit,
) {
    val listState = rememberLazyListState()
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            layoutInfo.totalItemsCount > 0 &&
                layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1 &&
                !listState.canScrollForward
        }
    }
    val sections = remember {
        listOf(
            R.string.terms_section_1_title to R.string.terms_section_1_body,
            R.string.terms_section_2_title to R.string.terms_section_2_body,
            R.string.terms_section_3_title to R.string.terms_section_3_body,
            R.string.terms_section_4_title to R.string.terms_section_4_body,
            R.string.terms_section_5_title to R.string.terms_section_5_body,
            R.string.terms_section_6_title to R.string.terms_section_6_body,
            R.string.terms_section_7_title to R.string.terms_section_7_body,
            R.string.terms_section_8_title to R.string.terms_section_8_body,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.terms_gate_title),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.terms_intro),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.Center,
            )
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cornerRadius = 18.dp,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                item(key = "terms-intro") {
                    Text(
                        text = stringResource(R.string.terms_last_updated),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }
                sections.forEachIndexed { index, (titleRes, bodyRes) ->
                    item(key = "terms-$index") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(titleRes),
                                color = MiuixTheme.colorScheme.onSurface,
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(bodyRes),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    }
                }
                item(key = "terms-bottom") {
                    Text(
                        text = stringResource(R.string.terms_at_bottom),
                        color = if (isAtBottom) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.55f)
                        },
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = isAtBottom,
                onClick = onAccepted,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = stringResource(
                        if (isAtBottom) R.string.terms_accept else R.string.terms_scroll_to_bottom,
                    ),
                )
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.terms_decline_exit),
                onClick = onExit,
            )
        }
    }
}
