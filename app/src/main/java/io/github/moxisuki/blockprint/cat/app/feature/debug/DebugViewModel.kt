package io.github.moxisuki.blockprint.cat.app.feature.debug

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DebugState(
            totalRamMb = context.totalRamMb(),
            heapLimitMb = context.heapLimitMb(),
            heapMaxMb = context.heapMaxMb(),
            densityDpi = context.densityDpi(),
            isAppStorageLoading = true,
        )
    )
    val state: StateFlow<DebugState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val appStorageMb = context.appStorageMb()
            _state.update {
                it.copy(
                    appStorageMb = appStorageMb,
                    isAppStorageLoading = false,
                )
            }
        }
    }
}

private fun Context.totalRamMb(): Int {
    val mem = ActivityManager.MemoryInfo()
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mem)
    return (mem.totalMem / (1024 * 1024)).toInt()
}

private fun Context.heapLimitMb(): Int {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.memoryClass
}

private fun Context.heapMaxMb(): Int {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        am.largeMemoryClass
    } else {
        am.memoryClass
    }
}

private fun Context.densityDpi(): Int =
    resources.displayMetrics.densityDpi

private fun Context.appStorageMb(): Int {
    val dir = filesDir.parentFile ?: return 0
    return (dirSizeBytes(dir) / (1024 * 1024)).toInt()
}

private fun dirSizeBytes(dir: File): Long {
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) dirSizeBytes(file) else file.length()
    }
    return size
}
