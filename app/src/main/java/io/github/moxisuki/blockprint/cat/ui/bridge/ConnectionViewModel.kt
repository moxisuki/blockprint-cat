package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val pairedDeviceDao: PairedDeviceDao,
) : ViewModel() {

    val paired: StateFlow<List<PairedDeviceEntity>> =
        pairedDeviceDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deletePaired(host: String, port: Int, folderName: String) = viewModelScope.launch {
        pairedDeviceDao.delete(host, port, folderName)
    }

    fun renamePaired(host: String, port: Int, folderName: String, newLabel: String) = viewModelScope.launch {
        pairedDeviceDao.find(host, port, folderName)?.let { existing ->
            pairedDeviceDao.upsert(existing.copy(label = newLabel))
        }
    }

    fun updateToken(host: String, port: Int, folderName: String, newToken: String) = viewModelScope.launch {
        pairedDeviceDao.find(host, port, folderName)?.let { existing ->
            pairedDeviceDao.upsert(existing.copy(token = newToken, tokenHint = newToken.take(6) + "\u2026"))
        }
    }
}
