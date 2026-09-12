package io.github.moxisuki.blockprint.cat.app.feature.community.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommunityTopicStrip(
    topics: List<String>,
    selectedTopics: List<String>,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(topics, selectedTopics) {
        val selectedIndex = selectedTopics
            .asReversed()
            .firstNotNullOfOrNull { selectedTopic ->
                topics.indexOfFirst { it.equals(selectedTopic, ignoreCase = true) }
                    .takeIf { it >= 0 }
            }
        if (selectedIndex != null) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 1.dp),
    ) {
        items(
            items = topics,
            key = { it },
        ) { topic ->
            CommunityTopicChip(
                text = topic,
                selected = selectedTopics.any { it.equals(topic, ignoreCase = true) },
                onClick = { onTopicClick(topic) },
            )
        }
    }
}

@Composable
private fun CommunityTopicChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        },
        label = "communityTopicChipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.onPrimary
        } else {
            MiuixTheme.colorScheme.onSurfaceContainer
        },
        label = "communityTopicChipContent",
    )

    Text(
        modifier = modifier
            .heightIn(min = 34.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        text = text,
        color = contentColor,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun CommunitySectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        text = text,
        color = MiuixTheme.colorScheme.primary,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.SemiBold,
    )
}

@Preview(showBackground = true)
@Composable
private fun CommunityTopicStripPreview() {
    PreviewAppTheme {
        CommunityTopicStrip(
            topics = listOf("litematic", "farm", "factory", "survival"),
            selectedTopics = listOf("farm", "survival"),
            onTopicClick = {},
        )
    }
}
