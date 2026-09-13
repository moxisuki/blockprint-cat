package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.feature.community.data.CmsCommunityRepository
import io.github.moxisuki.blockprint.cat.app.feature.community.data.cmsHasMore
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsCommunityRepository
import io.github.moxisuki.blockprint.cat.app.feature.community.data.mcsHasMore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CommunityViewModel @Inject constructor(
    private val mcsRepository: McsCommunityRepository,
    private val cmsRepository: CmsCommunityRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        CommunityState(visibleOverviewSources = initialOverviewSources()),
    )
    val state: StateFlow<CommunityState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadMcsCategories()
            refreshMcs()
        }
    }

    fun onAction(action: CommunityAction) {
        when (action) {
            is CommunityAction.SourceSelected -> {
                _state.update { state ->
                    val overviewSources = if (consumeOverviewVisibility(action.source)) {
                        state.visibleOverviewSources + action.source
                    } else {
                        state.visibleOverviewSources
                    }
                    state.copy(
                        selectedSource = action.source,
                        visibleOverviewSources = overviewSources,
                    )
                }
                when (action.source) {
                    CommunitySourceUi.MCS -> {
                        if (_state.value.mcs.items.isEmpty()) {
                            loadMcsCategories()
                            refreshMcs()
                        }
                    }
                    CommunitySourceUi.CMS -> {
                        if (_state.value.cms.items.isEmpty()) refreshCms()
                    }
                }
            }
            CommunityAction.RefreshRequested -> when (_state.value.selectedSource) {
                CommunitySourceUi.MCS -> {
                    loadMcsCategories(force = true)
                    refreshMcs()
                }
                CommunitySourceUi.CMS -> refreshCms()
            }
            CommunityAction.LoadMoreRequested -> when (_state.value.selectedSource) {
                CommunitySourceUi.MCS -> loadMoreMcs()
                CommunitySourceUi.CMS -> loadMoreCms()
            }
            CommunityAction.SearchToggled -> updateSearchExpanded()
            is CommunityAction.SearchQueryChanged -> updateSearchDraft(action.query)
            CommunityAction.SearchSubmitted -> submitSearch()
            CommunityAction.SearchCleared -> clearSearch()
            is CommunityAction.TopicToggled -> toggleTopic(action.topic)
            is CommunityAction.OverviewDismissed -> {
                _state.update {
                    it.copy(visibleOverviewSources = it.visibleOverviewSources - action.source)
                }
            }
        }
    }

    private fun updateSearchExpanded() {
        _state.update { state ->
            when (state.selectedSource) {
                CommunitySourceUi.MCS -> state.copy(
                    mcs = state.mcs.copy(
                        isSearchExpanded = !state.mcs.isSearchExpanded,
                        searchDraft = if (state.mcs.isSearchExpanded) {
                            state.mcs.filter
                        } else {
                            state.mcs.searchDraft
                        },
                    ),
                )
                CommunitySourceUi.CMS -> state.copy(
                    cms = state.cms.copy(
                        isSearchExpanded = !state.cms.isSearchExpanded,
                        searchDraft = if (state.cms.isSearchExpanded) {
                            state.cms.filter
                        } else {
                            state.cms.searchDraft
                        },
                    ),
                )
            }
        }
    }

    private fun updateSearchDraft(query: String) {
        _state.update { state ->
            when (state.selectedSource) {
                CommunitySourceUi.MCS -> state.copy(mcs = state.mcs.copy(searchDraft = query))
                CommunitySourceUi.CMS -> state.copy(cms = state.cms.copy(searchDraft = query))
            }
        }
    }

    private fun submitSearch() {
        when (_state.value.selectedSource) {
            CommunitySourceUi.MCS -> {
                _state.update { state ->
                    state.copy(
                        mcs = state.mcs.copy(
                            filter = state.mcs.searchDraft.trim(),
                            isSearchExpanded = false,
                            selectedTopics = emptyList(),
                            selectedCategorySlug = null,
                        ),
                    )
                }
                refreshMcs()
            }
            CommunitySourceUi.CMS -> {
                _state.update { state ->
                    state.copy(
                        cms = state.cms.copy(
                            filter = state.cms.searchDraft.trim(),
                            isSearchExpanded = false,
                            selectedTopics = emptyList(),
                        ),
                    )
                }
                refreshCms()
            }
        }
    }

    private fun clearSearch() {
        when (_state.value.selectedSource) {
            CommunitySourceUi.MCS -> {
                _state.update {
                    it.copy(
                        mcs = it.mcs.copy(
                            filter = "",
                            searchDraft = "",
                            selectedTopics = emptyList(),
                            selectedCategorySlug = null,
                            isSearchExpanded = false,
                        ),
                    )
                }
                refreshMcs()
            }
            CommunitySourceUi.CMS -> {
                _state.update {
                    it.copy(
                        cms = it.cms.copy(
                            filter = "",
                            searchDraft = "",
                            selectedTopics = emptyList(),
                            isSearchExpanded = false,
                        ),
                    )
                }
                refreshCms()
            }
        }
    }

    private fun toggleTopic(topic: String) {
        val normalizedTopic = topic.trim()
        if (normalizedTopic.isBlank()) return
        when (_state.value.selectedSource) {
            CommunitySourceUi.MCS -> {
                val category = _state.value.mcs.categories.firstOrNull {
                    it.name.equals(normalizedTopic, ignoreCase = true)
                }
                _state.update {
                    val selected = if (it.mcs.selectedTopics.any {
                            value -> value.equals(normalizedTopic, ignoreCase = true)
                        }) {
                        emptyList()
                    } else {
                        listOf(normalizedTopic)
                    }
                    it.copy(
                        mcs = it.mcs.copy(
                            selectedTopics = selected,
                            selectedCategorySlug = if (selected.isEmpty()) null else category?.slug,
                            isSearchExpanded = false,
                        ),
                    )
                }
                refreshMcs()
            }
            CommunitySourceUi.CMS -> {
                _state.update {
                    val selected = it.cms.selectedTopics.toggleTopic(normalizedTopic)
                    it.copy(
                        cms = it.cms.copy(
                            filter = selected.joinToString(" "),
                            searchDraft = selected.joinToString(" "),
                            selectedTopics = selected,
                            isSearchExpanded = false,
                        ),
                    )
                }
                refreshCms()
            }
        }
    }

    private fun loadMcsCategories(force: Boolean = false) {
        if (!force && _state.value.mcs.categories.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { mcsRepository.loadCategories() }
                .onSuccess { categories ->
                    _state.update { state ->
                        state.copy(
                            mcs = state.mcs.copy(
                                categories = categories,
                                topics = categories.map { it.name }.distinct(),
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    if (!error.isCancellationNoise()) {
                        _state.update {
                            it.copy(mcs = it.mcs.copy(errorMessage = error.toDisplayMessage()))
                        }
                    }
                }
        }
    }

    private fun refreshMcs() {
        val current = _state.value.mcs
        if (current.isLoading || current.isRefreshing) return
        viewModelScope.launch {
            val filter = _state.value.mcs.filter
            val category = _state.value.mcs.selectedCategorySlug
            _state.update { state ->
                state.copy(
                    mcs = state.mcs.copy(
                        isLoading = state.mcs.items.isEmpty(),
                        isRefreshing = state.mcs.items.isNotEmpty(),
                        errorMessage = null,
                    ),
                )
            }
            runCatching {
                mcsRepository.loadPage(
                    begin = 0,
                    filter = filter,
                    category = category,
                )
            }.onSuccess { page ->
                val items = page.items.mapIndexed { index, blueprint ->
                    blueprint.toCommunityItem(index)
                }
                _state.update { state ->
                    state.copy(
                        mcs = state.mcs.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = items,
                            total = page.totalCount,
                            hasMore = mcsHasMore(page, items.size),
                        ),
                    )
                }
            }.onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update {
                        it.copy(
                            mcs = it.mcs.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = error.toDisplayMessage(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun loadMoreMcs() {
        val current = _state.value.mcs
        if (current.isLoading || current.isRefreshing || !current.hasMore) return
        viewModelScope.launch {
            val start = _state.value.mcs.items.size
            _state.update { it.copy(mcs = it.mcs.copy(isLoading = true, errorMessage = null)) }
            runCatching {
                mcsRepository.loadPage(
                    begin = start,
                    filter = _state.value.mcs.filter,
                    category = _state.value.mcs.selectedCategorySlug,
                )
            }.onSuccess { page ->
                _state.update { state ->
                    val appended = page.items.mapIndexed { index, blueprint ->
                        blueprint.toCommunityItem(start + index)
                    }
                    val combined = state.mcs.items + appended
                    state.copy(
                        mcs = state.mcs.copy(
                            isLoading = false,
                            items = combined,
                            total = page.totalCount.takeIf { it > 0 } ?: state.mcs.total,
                            hasMore = mcsHasMore(page, combined.size),
                        ),
                    )
                }
            }.onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update {
                        it.copy(
                            mcs = it.mcs.copy(
                                isLoading = false,
                                errorMessage = error.toDisplayMessage(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun refreshCms() {
        val current = _state.value.cms
        if (current.isLoading || current.isRefreshing) return
        viewModelScope.launch {
            val filter = _state.value.cms.filter
            _state.update { state ->
                state.copy(
                    cms = state.cms.copy(
                        isLoading = state.cms.items.isEmpty(),
                        isRefreshing = state.cms.items.isNotEmpty(),
                        errorMessage = null,
                    ),
                )
            }
            runCatching {
                cmsRepository.loadPage(begin = 0, filter = filter, heatSort = false)
            }.onSuccess { page ->
                val items = page.items.mapIndexed { index, item ->
                    item.toCommunityItem(index)
                }
                _state.update { state ->
                    state.copy(
                        cms = state.cms.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = items,
                            total = if (items.isEmpty()) 0 else 999,
                            hasMore = cmsHasMore(page.items.size),
                        ),
                    )
                }
            }.onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update {
                        it.copy(
                            cms = it.cms.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = error.toDisplayMessage(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun loadMoreCms() {
        val current = _state.value.cms
        if (current.isLoading || current.isRefreshing || !current.hasMore) return
        viewModelScope.launch {
            val start = _state.value.cms.items.size
            _state.update { it.copy(cms = it.cms.copy(isLoading = true, errorMessage = null)) }
            runCatching {
                cmsRepository.loadPage(
                    begin = start,
                    filter = _state.value.cms.filter,
                    heatSort = false,
                )
            }.onSuccess { page ->
                _state.update { state ->
                    val appended = page.items.mapIndexed { index, item ->
                        item.toCommunityItem(start + index)
                    }
                    val combined = state.cms.items + appended
                    state.copy(
                        cms = state.cms.copy(
                            isLoading = false,
                            items = combined,
                            total = if (combined.isEmpty()) 0 else 999,
                            hasMore = cmsHasMore(page.items.size),
                        ),
                    )
                }
            }.onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update {
                        it.copy(
                            cms = it.cms.copy(
                                isLoading = false,
                                errorMessage = error.toDisplayMessage(),
                            ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val shownOverviewSourcesThisLaunch = mutableSetOf<CommunitySourceUi>()

        private fun initialOverviewSources(): Set<CommunitySourceUi> =
            if (consumeOverviewVisibility(CommunitySourceUi.MCS)) {
                setOf(CommunitySourceUi.MCS)
            } else {
                emptySet()
            }

        private fun consumeOverviewVisibility(source: CommunitySourceUi): Boolean =
            shownOverviewSourcesThisLaunch.add(source)
    }
}

private fun Throwable.toDisplayMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName

private fun Throwable.isCancellationNoise(): Boolean =
    this is CancellationException ||
        this::class.java.simpleName.contains("Cancelled", ignoreCase = true)

private fun List<String>.toggleTopic(topic: String): List<String> =
    if (any { it.equals(topic, ignoreCase = true) }) {
        filterNot { it.equals(topic, ignoreCase = true) }
    } else {
        this + topic
    }
