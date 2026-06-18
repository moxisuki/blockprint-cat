package io.github.moxisuki.blockprint.cat.ui.community

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.community.CommunitySource
import io.github.moxisuki.blockprint.cat.ui.util.appErrorMessage
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes

@Composable
fun CommunityScreen(
    navController: NavController,
    viewModel: CommunityViewModel,
    onSchematicSelected: ((CommunitySource, String) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    val active = state.active

    // 免责声明 — Room 持久化，首次接受后不再显示
    val ctx = LocalContext.current
    val disclaimerDao = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            ctx.applicationContext, DisclaimerEntryPoint::class.java
        ).disclaimerStatusDao()
    }
    var disclaimerLoaded by remember { mutableStateOf(false) }
    var disclaimerAccepted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        disclaimerAccepted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            disclaimerDao.get()?.accepted == true
        }
        disclaimerLoaded = true
    }
    val scope = rememberCoroutineScope()
    var showDisclaimer by remember { mutableStateOf(true) }
    var canDismiss by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(10) }
    if (showDisclaimer && disclaimerLoaded && !disclaimerAccepted) {
        LaunchedEffect(Unit) {
            while (countdown > 0) { kotlinx.coroutines.delay(1000); countdown-- }
            canDismiss = true
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.disclaimer_intro))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.disclaimer_no_store), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.disclaimer_copyright), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.disclaimer_license), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.disclaimer_dmca), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.disclaimer_consent), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisclaimer = false
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        disclaimerDao.upsert(io.github.moxisuki.blockprint.cat.data.community.DisclaimerStatusEntity(accepted = true, acceptedAt = System.currentTimeMillis()))
                    }
                }, enabled = canDismiss) {
                    Text(if (canDismiss) "我知道了" else "请阅读 ($countdown s)")
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLoginState()
        // Auto-load on first enter if source is saved
        if (state.currentSource == CommunitySource.CMS && !state.cms.ready) {
            viewModel.switchSource(state.currentSource)
        }
    }

    LaunchedEffect(state.currentSource) {
        if (state.currentSource == CommunitySource.CMS && !state.cms.ready && !state.cms.loading) {
            viewModel.switchSource(state.currentSource)
        }
    }

    LaunchedEffect(active.ready, state.currentSource) {
        if (active.ready && active.schematics.isEmpty() && !active.loading) {
            viewModel.refresh()
        }
    }

    var showBypass by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {

        SourceCapsuleRow(
            current = state.currentSource,
            onSelect = { viewModel.switchSource(it) },
        )

        // 内容区:切换源时用 AnimatedContent 做左右滑动动画
        AnimatedContent(
            targetState = state.currentSource,
            transitionSpec = {
                val dir = if (CommunitySource.values().indexOf(targetState)
                    > CommunitySource.values().indexOf(initialState)) 1 else -1
                (slideInHorizontally(AnimSpec.slide) { it * dir } + fadeIn(AnimSpec.content)) togetherWith
                    (slideOutHorizontally(AnimSpec.slideExit) { -it * dir } + fadeOut(AnimSpec.fadeExit)) using SizeTransform(clip = false)
            },
            label = "communitySource",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { activeSource ->
            val activeForSource = when (activeSource) {
                CommunitySource.MCS -> state.mcs
                CommunitySource.CMS -> state.cms
            }

            Column(modifier = Modifier.fillMaxSize()) {

                if (activeForSource.loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (state.isDownloading && state.downloadBytes >= 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                AnimatedVisibility(
                    visible = activeForSource.showFilter && activeForSource.ready,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val hasInput = activeForSource.filterDraft.isNotBlank()
                    val canSubmit = hasInput && activeForSource.filterDraft.trim() != activeForSource.filter
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = activeForSource.filterDraft,
                            onValueChange = { viewModel.setFilterDraft(it) },
                            placeholder = { Text(stringResource(R.string.community_search_hint)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search, contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = if (hasInput) {{
                                IconButton(
                                    onClick = { viewModel.setFilterDraft("") },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close, contentDescription = "清除输入",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }} else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { viewModel.applyFilter(activeForSource.filterDraft.trim()) },
                            ),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { viewModel.applyFilter(activeForSource.filterDraft.trim()) },
                            enabled = canSubmit,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "应用", modifier = Modifier.size(22.dp))
                        }
                    }
                }

                AnimatedVisibility(
                    visible = activeForSource.filter.isNotEmpty() && activeForSource.ready,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = { viewModel.clearFilter() },
                            label = {
                                Text(
                                    "过滤: ${activeForSource.filter}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.Close, "清除过滤", Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }

                if (!activeForSource.ready && activeSource == CommunitySource.MCS) {
                    NotLoggedInPane(onLogin = { navController.navigate(NavRoutes.COMMUNITY_LOGIN) })
                } else if (activeForSource.schematics.isEmpty() && !activeForSource.loading) {
                    val isCMS = activeSource == CommunitySource.CMS && !activeForSource.ready
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isCMS) stringResource(R.string.community_cms_need_verify)
                                else activeForSource.error?.let { appErrorMessage(it) } ?: stringResource(R.string.community_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isCMS) {
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { showBypass = true }) {
                                    Text(stringResource(R.string.community_cms_open_verify))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.community_cms_verify_hint), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, activeSource) {
                        snapshotFlow {
                            val info = listState.layoutInfo
                            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                            val total = info.totalItemsCount
                            if (total > 0) last to total else null
                        }.collect { pair ->
                            pair?.let { (last, total) ->
                                if (last >= total - 3) viewModel.loadMore()
                            }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            val countText = when {
                                activeForSource.total > 0 -> "共 ${activeForSource.total} 个蓝图"
                                activeForSource.schematics.isNotEmpty() -> "已加载 ${activeForSource.schematics.size} 个"
                                else -> null
                            }
                            if (countText != null) {
                                Text(
                                    countText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        items(activeForSource.schematics, key = { "${it.source.name}/${it.id}" }, contentType = { "schematic" }) { s ->
                            SchematicCard(
                                name = s.name,
                                nickName = s.author,
                                size = s.size,
                                heat = s.heat ?: s.downloads ?: 0,
                                tags = s.tags,
                                onClick = {
                                    if (onSchematicSelected != null) {
                                        onSchematicSelected(s.source, s.id)
                                    } else {
                                        navController.navigate(NavRoutes.communityDetailRoute(s.source, s.id))
                                    }
                                },
                            )
                        }
                        if (activeForSource.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
    if (showBypass) {
        CmsBypassOverlay(onClose = {
            showBypass = false
            viewModel.switchSource(CommunitySource.CMS)
        })
    }
    } // Box
}

@Composable
private fun SourceCapsuleRow(
    current: CommunitySource,
    onSelect: (CommunitySource) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CommunitySource.values().forEach { src ->
                val selected = current == src
                Box(
                    modifier = Modifier
                        .then(
                            if (selected)
                                Modifier
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            else Modifier.padding(3.dp)
                        )
                        .clickable { if (!selected) onSelect(src) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = src.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotLoggedInPane(onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Person, contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.community_login_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.community_login_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogin) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.community_login_qq))
        }
    }
}

@Composable
private fun SchematicCard(
    name: String,
    nickName: String,
    size: Triple<Int, Int, Int>?,
    heat: Int,
    tags: List<String>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.LocalFireDepartment, contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(2.dp))
                Text(heat.toString(),
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nickName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (size != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${size.first}×${size.second}×${size.third}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.take(4).forEach { tag ->
                        Text(
                            tag.removePrefix("minecraft:"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DisclaimerEntryPoint {
    fun disclaimerStatusDao(): io.github.moxisuki.blockprint.cat.data.community.DisclaimerStatusDao
}
