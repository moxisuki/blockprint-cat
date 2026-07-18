package io.github.moxisuki.blockprint.cat.app.feature.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import io.github.moxisuki.blockprint.cat.app.feature.about.data.AboutRepository
import io.github.moxisuki.blockprint.cat.app.feature.about.data.HitokotoQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val repository: AboutRepository,
    private val languageManager: AppLanguageManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AboutState(isChineseLocale = languageManager.isChinese())
    )
    val state: StateFlow<AboutState> = _state.asStateFlow()
    private var hasLoadedHitokoto = false

    init {
        onAction(AboutAction.Opened)
    }

    fun onAction(action: AboutAction) {
        when (action) {
            AboutAction.Opened -> {
                if (!hasLoadedHitokoto) {
                    hasLoadedHitokoto = true
                    if (languageManager.isChinese()) {
                        loadHitokoto()
                    }
                }
            }

            AboutAction.RefreshHitokoto -> loadHitokoto()
        }
    }

    private fun loadHitokoto() {
        viewModelScope.launch {
            val hitokotoState = when (val result = repository.loadHitokoto()) {
                is AppNetworkResult.Success -> result.value.toAboutState()
                is AppNetworkResult.Failure -> AboutHitokotoState.Unavailable
            }
            _state.update { it.copy(hitokoto = hitokotoState) }
        }
    }
}

private fun HitokotoQuote.toAboutState(): AboutHitokotoState.Content {
    val source = buildString {
        if (fromWho.isNotBlank()) append(fromWho)
        if (from.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(from)
        }
    }
    return AboutHitokotoState.Content(
        text = text,
        source = source,
    )
}
