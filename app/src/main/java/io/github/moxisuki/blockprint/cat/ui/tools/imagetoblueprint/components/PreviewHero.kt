package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.PreviewAnimations
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.PreviewMode
import kotlinx.coroutines.delay

/**
 * 主预览区域：根据 [previewMode] 在 Source/Result 之间 Crossfade
 * 显示来源图或结果图。
 *
 * 特性：
 *  - Crossfade 切换 Source ↔ Result（动画规格 [PreviewAnimations.MEDIUM]）
 *  - "刷一下"高亮：[lastUpdatedAt] 变化时边框 alpha 短暂拉到 1 再 fade 回 0（[PreviewAnimations.SHORT]）
 *  - "更新中…" pill：在 [isUpdating] 为 true 时显示在右上角
 */
@Composable
internal fun PreviewHero(
    sourceUri: Uri?,
    sourceWidth: Int,
    sourceHeight: Int,
    resultBitmap: Bitmap?,
    resultWidth: Int,
    resultHeight: Int,
    previewMode: PreviewMode,
    isUpdating: Boolean,
    lastUpdatedAt: Long,
    onReselect: () -> Unit,
    onEnlarge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)

    // 刷一下高亮：lastUpdatedAt 变化时把 highlight 置 true 一帧，再 fade 回
    var highlight by remember(lastUpdatedAt) { mutableStateOf(false) }
    LaunchedEffect(lastUpdatedAt) {
        if (lastUpdatedAt > 0L) {
            highlight = true
            delay(PreviewAnimations.SHORT.toLong())
            highlight = false
        }
    }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlight) 1f else 0f,
        animationSpec = tween(PreviewAnimations.SHORT, easing = PreviewAnimations.EasingEmphasized),
        label = "highlight",
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 320.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)),
                    shape,
                )
                .clickable { onEnlarge() },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = previewMode,
                animationSpec = tween(PreviewAnimations.MEDIUM, easing = PreviewAnimations.EasingStandard),
                label = "preview-mode",
            ) { mode ->
                when (mode) {
                    PreviewMode.Source -> SourceContent(
                        uri = sourceUri,
                        sourceWidth = sourceWidth,
                        sourceHeight = sourceHeight,
                        onReselect = onReselect,
                    )
                    PreviewMode.Result -> ResultContent(
                        bitmap = resultBitmap,
                        resultWidth = resultWidth,
                        resultHeight = resultHeight,
                    )
                }
            }
            if (isUpdating) {
                UpdatingPill()
            }
        }
        if (sourceUri != null) {
            ReselectButton(onClick = onReselect)
        }
    }
}

/** Crossfade 切换到 Source 时显示的内容：用户选的原始图（带尺寸角标）或空状态。 */
@Composable
private fun SourceContent(
    uri: Uri?,
    sourceWidth: Int,
    sourceHeight: Int,
    onReselect: () -> Unit,
) {
    when {
        uri != null -> {
            Box {
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.itb_select_image),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    contentScale = ContentScale.Fit,
                )
                if (sourceWidth > 0 && sourceHeight > 0) {
                    Text(
                        stringResource(R.string.itb_image_size, sourceWidth, sourceHeight),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        else -> EmptyHero(onClick = onReselect)
    }
}

/** Crossfade 切换到 Result 时显示的内容：转换后的方块预览图（带尺寸角标）或空状态。 */
@Composable
private fun ResultContent(
    bitmap: Bitmap?,
    resultWidth: Int,
    resultHeight: Int,
) {
    if (bitmap == null) {
        EmptyHero(onClick = {}, hint = stringResource(R.string.itb_no_result))
        return
    }
    Box {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.itb_enlarge_hint),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
        )
        if (resultWidth > 0 && resultHeight > 0) {
            Text(
                stringResource(R.string.itb_result_size, resultWidth, resultHeight),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** 空状态：图标 + 提示文字。可点击触发选择图片。 */
@Composable
private fun EmptyHero(
    onClick: () -> Unit,
    hint: String = stringResource(R.string.itb_select_image_hint),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/** 转换进行中浮在 Hero 右上角的小药丸：转圈 + "更新中…"。 */
@Composable
private fun UpdatingPill() {
    Box(
        modifier = Modifier
            .padding(12.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.itb_updating),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 重新选择图片按钮（右对齐，含图标 + 文字）。 */
@Composable
private fun ReselectButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onClick) {
            Icon(
                Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.itb_reselect))
        }
    }
}
