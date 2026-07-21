package io.github.moxisuki.blockprint.cat.app.feature.home

sealed interface HomeAction {
    data object Opened : HomeAction

    data class SourceSelected(val source: HomeBlueprintSource) : HomeAction

    data class SafDirectorySelected(
        val treeUri: String,
        val treeDocumentId: String?,
    ) : HomeAction

    data object SearchToggled : HomeAction

    data class SearchQueryChanged(val query: String) : HomeAction

    data object SearchCleared : HomeAction

    data object RefreshClicked : HomeAction

    data class ImportFileSelected(val uri: String) : HomeAction

    data object ImportConfirmed : HomeAction

    data object ImportDismissed : HomeAction

    data object CategoryBarToggled : HomeAction

    data class CategorySelected(val categoryId: String) : HomeAction

    data class CategoryManageVisibilityChanged(val visible: Boolean) : HomeAction

    data class CategoryCreated(val name: String) : HomeAction

    data class CategoryRenamed(val oldName: String, val newName: String) : HomeAction

    data class CategoryDeleted(val name: String) : HomeAction

    data class BlueprintLongPressed(val blueprintId: String) : HomeAction

    data class BlueprintSelectionToggled(val blueprintId: String) : HomeAction

    data object BlueprintSelectionCleared : HomeAction

    data class MoveCategoryRequested(val blueprintIds: Set<String>) : HomeAction

    data class MoveCategorySelected(val categoryId: String) : HomeAction

    data object MoveCategoryDismissed : HomeAction

    data class RenameRequested(val blueprintId: String) : HomeAction

    data class RenameConfirmed(val blueprintId: String, val fileName: String) : HomeAction

    data object RenameDismissed : HomeAction

    data class DeleteRequested(val blueprintId: String) : HomeAction

    data class DeleteConfirmed(val blueprintId: String) : HomeAction

    data object DeleteDismissed : HomeAction
}
