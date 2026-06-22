package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.settings.AppIconManager
import io.github.moxisuki.blockprint.cat.data.settings.TermsAcceptance
import dagger.hilt.android.EntryPointAccessors

/**
 * 冷启动全屏门。未同意使用条款时，挡住主界面。
 * - 滚动到底才可点"我已阅读并同意"
 * - "退出 App" → finishAffinity()
 */
@Composable
fun TermsGate(onAccepted: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TermsGateEntryPoint::class.java
        )
    }
    val termsAcceptance = entryPoint.termsAcceptance()
    val appIconManager = entryPoint.appIconManager()
    val appIconCurrent by appIconManager.current.collectAsState()
    val appIconVariant = appIconManager.variants.firstOrNull { it.id == appIconCurrent } ?: appIconManager.variants.first()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏 — 没有返回按钮，用户不能跳过
            Text(
                "使用条款",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )

            // 居中标识 — 显示当前 app icon variant
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(appIconVariant.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("BlockPrint Cat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.terms_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 条款内容 — LazyColumn，检测滚动到底
            val listState = rememberLazyListState()
            val isAtBottom by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val total = info.totalItemsCount
                    if (total == 0) {
                        false
                    } else {
                        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                        // 真正到底：可见最后一项 >= 末项索引，并且没有"还能继续往下"的余量
                        lastVisible >= total - 1 && !listState.canScrollForward
                    }
                }
            }

            // 在 @Composable 外层 resolve，LazyColumn 内只能 item/items 创建 @Composable scope
            val resolvedIntro = termsIntro()
            val resolvedSections = termsSections()

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                ) {
                    item {
                        Text(
                            resolvedIntro,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    for (index in resolvedSections.indices) {
                        val section = resolvedSections[index]
                        if (index > 0) {
                            item { TermsSectionDivider() }
                        }
                        item { TermsSectionTitle(section.title) }
                        item { TermsSectionBody(section.body) }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.terms_at_bottom),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAtBottom) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // 底部按钮区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
            ) {
                Button(
                    onClick = {
                        termsAcceptance.accept()
                        onAccepted()
                    },
                    enabled = isAtBottom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isAtBottom) stringResource(R.string.terms_accept) else stringResource(R.string.terms_scroll_to_bottom))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.terms_decline_exit))
                }
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface TermsGateEntryPoint {
    fun termsAcceptance(): TermsAcceptance
    fun appIconManager(): AppIconManager
}
