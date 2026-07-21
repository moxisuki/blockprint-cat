package io.github.moxisuki.blockprint.cat.app.feature.home.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
internal fun CategoryFilterBar(
    categories: List<BlueprintCategoryFilter>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    manageLabel: String,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = categories,
            key = { it.id },
        ) { category ->
            CategoryFilterChip(
                category = category,
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
            )
        }
        item(key = "manage") {
            CategoryManageChip(
                text = manageLabel,
                onClick = onManageClick,
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    category: BlueprintCategoryFilter,
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
        label = "categoryChipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.onPrimary
        } else {
            MiuixTheme.colorScheme.onSurfaceContainer
        },
        label = "categoryChipContent",
    )
    val countColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
        } else {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        },
        label = "categoryChipCount",
    )

    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.label,
                color = contentColor,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = category.count.toString(),
                color = countColor,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CategoryManageChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.primary,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryFilterBarPreview() {
    PreviewAppTheme {
        CategoryFilterBar(
            categories = listOf(
                BlueprintCategoryFilter(HomeCategoryId.All, "全部", 8),
                BlueprintCategoryFilter("Survival", "Survival", 3),
                BlueprintCategoryFilter("Machine", "Machine", 2),
                BlueprintCategoryFilter(HomeCategoryId.Uncategorized, "未分类", 0),
            ),
            selectedCategoryId = "Survival",
            onCategorySelected = {},
            manageLabel = "管理分类",
            onManageClick = {},
        )
    }
}
