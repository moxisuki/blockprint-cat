package io.github.moxisuki.blockprint.cat.app.feature.community

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsCommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
internal data class CommunityLoginState(
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class CommunityLoginViewModel @Inject constructor(
    private val mcsRepository: McsCommunityRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityLoginState())
    val state: StateFlow<CommunityLoginState> = _state.asStateFlow()

    fun onCookiesCaptured(
        cookies: McsAuthCookies,
        onSuccess: () -> Unit,
    ) {
        Log.d(McsLoginLogTag, "ViewModel received auth candidate: ${cookies.toDebugSummary()}")
        if (_state.value.isVerifying) {
            Log.d(McsLoginLogTag, "ViewModel ignored candidate: verification already running")
            return
        }
        viewModelScope.launch {
            Log.d(McsLoginLogTag, "ViewModel start verifying candidate")
            _state.update { it.copy(isVerifying = true, errorMessage = null) }
            runCatching {
                mcsRepository.verifyAndSaveLogin(cookies)
            }.onSuccess { verified ->
                Log.d(McsLoginLogTag, "ViewModel verification result: verified=$verified")
                if (verified) {
                    _state.update { it.copy(isVerifying = false, errorMessage = null) }
                    Log.d(McsLoginLogTag, "ViewModel login success: navigating back")
                    onSuccess()
                } else {
                    Log.w(McsLoginLogTag, "ViewModel login not verified: keep WebView open")
                    _state.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = "还没有完成 MCS 授权，请继续登录。",
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(McsLoginLogTag, "ViewModel verification failed", error)
                _state.update {
                    it.copy(
                        isVerifying = false,
                        errorMessage = error.message
                            ?.takeIf { message -> message.isNotBlank() }
                            ?: error::class.java.simpleName,
                    )
                }
            }
        }
    }
}
