package io.github.moxisuki.blockprint.cat.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec

/**
 * Top bar shared by every destination in [io.github.moxisuki.blockprint.cat.ui.navigation.AppNavGraph].
 *
 * Behaviour is identical to the original `private fun AppTopBar` that lived
 * inside MainActivity.kt before the MainActivity split; only the visibility
 * changed (`private` → `internal`) so the surrounding navigation graph can
 * call it without keeping MainActivity as the host.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    title: String,
    showBackButton: Boolean,
    showCommunityActions: Boolean,
    showLogout: Boolean,
    onCommunity: Boolean,
    onToggleFilter: () -> Unit,
    onToggleHeatSort: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    isHeatSort: Boolean,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(AnimSpec.title) togetherWith fadeOut(AnimSpec.title) using SizeTransform(clip = false) },
                label = "topBarTitle",
            ) { t ->
                Text(
                    t,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = showBackButton,
                enter = fadeIn(AnimSpec.title),
                exit = fadeOut(AnimSpec.title),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
        actions = {
            if (onCommunity && showCommunityActions) {
                IconButton(onClick = onToggleFilter) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                }
                IconButton(onClick = onToggleHeatSort) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = if (isHeatSort) stringResource(R.string.cd_sort_time) else stringResource(R.string.cd_sort_hot),
                        tint = if (isHeatSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                }
                if (showLogout) {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = stringResource(R.string.cd_logout))
                    }
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}