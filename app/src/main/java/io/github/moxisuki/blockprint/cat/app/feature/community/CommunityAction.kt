package io.github.moxisuki.blockprint.cat.app.feature.community

internal sealed interface CommunityAction {
    data class SourceSelected(val source: CommunitySourceUi) : CommunityAction
    data object RefreshRequested : CommunityAction
    data object LoadMoreRequested : CommunityAction
    data object SearchToggled : CommunityAction
    data class SearchQueryChanged(val query: String) : CommunityAction
    data object SearchSubmitted : CommunityAction
    data object SearchCleared : CommunityAction
    data class TopicToggled(val topic: String) : CommunityAction
    data class OverviewDismissed(val source: CommunitySourceUi) : CommunityAction
}
