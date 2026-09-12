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
        observeMcsCookies()
        viewModelScope.launch {
            if (mcsRepository.hasLocalLogin()) {
                checkMcsLogin()
                loadMcsTags()
                refreshMcs()
            }
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
                        if (_state.value.mcs.isLoggedIn && _state.value.mcs.items.isEmpty()) {
                            loadMcsTags()
                            refreshMcs()
                        }
                    }
                    CommunitySourceUi.CMS -> {
                        if (_state.value.cms.items.isEmpty()) {
                            refreshCms()
                        }
                    }
                }
            }
            CommunityAction.RefreshRequested -> {
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> {
                        loadMcsTags(force = true)
                        refreshMcs()
                    }
                    CommunitySourceUi.CMS -> refreshCms()
                }
            }
            CommunityAction.LoadMoreRequested -> {
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> loadMoreMcs()
                    CommunitySourceUi.CMS -> loadMoreCms()
                }
            }
            CommunityAction.SearchToggled -> {
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> _state.update { state ->
                        state.copy(
                            mcs = state.mcs.copy(
                                isSearchExpanded = !state.mcs.isSearchExpanded,
                                searchDraft = if (state.mcs.isSearchExpanded) {
                                    state.mcs.filter
                                } else {
                                    state.mcs.searchDraft
                                },
                            ),
                        )
                    }
                    CommunitySourceUi.CMS -> _state.update { state ->
                        state.copy(
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
            is CommunityAction.SearchQueryChanged -> {
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> _state.update { state ->
                        state.copy(mcs = state.mcs.copy(searchDraft = action.query))
                    }
                    CommunitySourceUi.CMS -> _state.update { state ->
                        state.copy(cms = state.cms.copy(searchDraft = action.query))
                    }
                }
            }
            CommunityAction.SearchSubmitted -> {
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> {
                        val query = _state.value.mcs.searchDraft.trim()
                        _state.update { state ->
                            state.copy(
                                mcs = state.mcs.copy(
                                    filter = query,
                                    isSearchExpanded = false,
                                    searchDraft = query,
                                    selectedTopics = emptyList(),
                                ),
                            )
                        }
                        refreshMcs()
                    }
                    CommunitySourceUi.CMS -> {
                        val query = _state.value.cms.searchDraft.trim()
                        _state.update { state ->
                            state.copy(
                                cms = state.cms.copy(
                                    filter = query,
                                    isSearchExpanded = false,
                                    searchDraft = query,
                                    selectedTopics = emptyList(),
                                ),
                            )
                        }
                        refreshCms()
                    }
                }
            }
            CommunityAction.SearchCleared -> {
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> {
                        _state.update { state ->
                            state.copy(
                                mcs = state.mcs.copy(
                                    filter = "",
                                    searchDraft = "",
                                    selectedTopics = emptyList(),
                                    isSearchExpanded = false,
                                ),
                            )
                        }
                        refreshMcs()
                    }
                    CommunitySourceUi.CMS -> {
                        _state.update { state ->
                            state.copy(
                                cms = state.cms.copy(
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
            is CommunityAction.TopicToggled -> {
                val topic = action.topic.trim()
                if (topic.isBlank()) return
                when (_state.value.selectedSource) {
                    CommunitySourceUi.MCS -> {
                        _state.update { state ->
                            val selectedTopics = state.mcs.selectedTopics.toggleTopic(topic)
                            val filter = selectedTopics.joinToString(" ")
                            state.copy(
                                mcs = state.mcs.copy(
                                    filter = filter,
                                    searchDraft = filter,
                                    selectedTopics = selectedTopics,
                                    isSearchExpanded = false,
                                ),
                            )
                        }
                        refreshMcs()
                    }
                    CommunitySourceUi.CMS -> {
                        _state.update { state ->
                            val selectedTopics = state.cms.selectedTopics.toggleTopic(topic)
                            val filter = selectedTopics.joinToString(" ")
                            state.copy(
                                cms = state.cms.copy(
                                    filter = filter,
                                    searchDraft = filter,
                                    selectedTopics = selectedTopics,
                                    isSearchExpanded = false,
                                ),
                            )
                        }
                        refreshCms()
                    }
                }
            }
            is CommunityAction.OverviewDismissed -> {
                _state.update {
                    it.copy(visibleOverviewSources = it.visibleOverviewSources - action.source)
                }
            }
            CommunityAction.LogoutRequested -> {
                viewModelScope.launch {
                    mcsRepository.clearLogin()
                    _state.update { it.copy(mcs = CommunityMcsState()) }
                }
            }
        }
    }

    private fun observeMcsCookies() {
        viewModelScope.launch {
            mcsRepository.authCookies.collect { cookies ->
                val wasLoggedIn = _state.value.mcs.isLoggedIn
                _state.update { state ->
                    state.copy(mcs = state.mcs.copy(cookies = cookies))
                }
                if (!cookies.isLoggedIn) {
                    _state.update { state ->
                        state.copy(
                            mcs = state.mcs.copy(
                                items = emptyList(),
                                total = 0,
                                hasMore = false,
                                filter = "",
                                searchDraft = "",
                                selectedTopics = emptyList(),
                                topics = emptyList(),
                                isSearchExpanded = false,
                            ),
                        )
                    }
                } else if (!wasLoggedIn && _state.value.selectedSource == CommunitySourceUi.MCS) {
                    checkMcsLogin()
                    loadMcsTags()
                    refreshMcs()
                }
            }
        }
    }

    private suspend fun checkMcsLogin() {
        _state.update { state ->
            state.copy(mcs = state.mcs.copy(isCheckingLogin = true, errorMessage = null))
        }
        runCatching { mcsRepository.refreshLoginStatus() }
            .onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update { state ->
                        state.copy(mcs = state.mcs.copy(errorMessage = error.toDisplayMessage()))
                    }
                }
            }
        _state.update { state ->
            state.copy(mcs = state.mcs.copy(isCheckingLogin = false))
        }
    }

    private fun refreshMcs() {
        val current = _state.value.mcs
        if (!current.isLoggedIn || current.isLoading || current.isRefreshing) return
        viewModelScope.launch {
            if (_state.value.mcs.topics.isEmpty()) {
                loadMcsTags()
            }
            val filter = _state.value.mcs.filter
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
                    heatSort = false,
                )
            }.onSuccess { page ->
                val items = page.items.mapIndexed { index, schematic ->
                    schematic.toCommunityItem(index)
                }
                _state.update { state ->
                    state.copy(
                        mcs = state.mcs.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = items,
                            total = page.total.coerceAtLeast(0),
                            hasMore = mcsHasMore(
                                total = page.total,
                                loaded = items.size,
                                latestPageSize = page.items.size,
                            ),
                        ),
                    )
                }
            }.onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update { state ->
                        state.copy(
                            mcs = state.mcs.copy(
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

    private fun loadMcsTags(force: Boolean = false) {
        val current = _state.value.mcs
        if (!current.isLoggedIn || (!force && current.topics.isNotEmpty())) return
        viewModelScope.launch {
            runCatching { mcsRepository.loadTags() }
                .onSuccess { tags ->
                    val topics = tags
                        .map { it.name }
                        .distinct()
                    _state.update { state ->
                        state.copy(mcs = state.mcs.copy(topics = topics))
                    }
                }
        }
    }

    private fun loadMoreMcs() {
        val current = _state.value.mcs
        if (!current.isLoggedIn || current.isLoading || current.isRefreshing || !current.hasMore) return
        viewModelScope.launch {
            val start = _state.value.mcs.items.size
            val filter = _state.value.mcs.filter
            _state.update { state ->
                state.copy(mcs = state.mcs.copy(isLoading = true, errorMessage = null))
            }
            runCatching {
                mcsRepository.loadPage(
                    begin = start,
                    filter = filter,
                    heatSort = false,
                )
            }.onSuccess { page ->
                _state.update { state ->
                    val appended = page.items.mapIndexed { index, schematic ->
                        schematic.toCommunityItem(start + index)
                    }
                    val combined = state.mcs.items + appended
                    state.copy(
                        mcs = state.mcs.copy(
                            isLoading = false,
                            items = combined,
                            total = if (page.total >= 0) page.total else state.mcs.total,
                            hasMore = mcsHasMore(
                                total = state.mcs.total,
                                loaded = combined.size,
                                latestPageSize = page.items.size,
                            ),
                        ),
                    )
                }
            }.onFailure { error ->
                if (!error.isCancellationNoise()) {
                    _state.update { state ->
                        state.copy(
                            mcs = state.mcs.copy(
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
                cmsRepository.loadPage(
                    begin = 0,
                    filter = filter,
                    heatSort = false,
                )
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
                    _state.update { state ->
                        state.copy(
                            cms = state.cms.copy(
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
            val filter = _state.value.cms.filter
            _state.update { state ->
                state.copy(cms = state.cms.copy(isLoading = true, errorMessage = null))
            }
            runCatching {
                cmsRepository.loadPage(
                    begin = start,
                    filter = filter,
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
                    _state.update { state ->
                        state.copy(
                            cms = state.cms.copy(
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
