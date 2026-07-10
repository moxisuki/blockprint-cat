package io.github.moxisuki.blockprint.cat.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow

@Composable
fun CategoryRail(
    rows: List<CategoryRow>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryRow) -> Unit,
    onCategoryLongClick: (CategoryRow) -> Unit = {},
    onAddClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_category_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_category_add),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        // "全部" 自身就是有效分类, 不强求用户创建更多. 顶部的 + 按钮即足够.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rows, key = { row ->
                when (row) {
                    is CategoryRow.All -> "cat-all"
                    is CategoryRow.Real -> "cat-${row.entity.id}"
                }
            }) { row ->
                val isSelected = when (row) {
                    is CategoryRow.All -> selectedId == null
                    is CategoryRow.Real -> selectedId == row.entity.id
                }
                CategoryCard(
                    row = row,
                    selected = isSelected,
                    onClick = { onCategoryClick(row) },
                    onLongClick = { onCategoryLongClick(row) },
                )
            }
        }
    }
}
