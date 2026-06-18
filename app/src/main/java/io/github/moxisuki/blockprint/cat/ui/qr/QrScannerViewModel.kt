package io.github.moxisuki.blockprint.cat.ui.qr

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.data.bridge.QrConnection
import io.github.moxisuki.blockprint.cat.data.bridge.parseQrPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface QrScanEvent {
    data class Success(val connection: QrConnection) : QrScanEvent
    data object InvalidPayload : QrScanEvent
}

@HiltViewModel
class QrScannerViewModel @Inject constructor() : ViewModel() {

    private val _events = MutableSharedFlow<QrScanEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<QrScanEvent> = _events.asSharedFlow()

    private val _scanning = MutableStateFlow(true)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _frameFlash = MutableStateFlow(FrameFlash.None)
    val frameFlash: StateFlow<FrameFlash> = _frameFlash.asStateFlow()

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    /** debounce: 同一 URI 1s 内不重复触发。 */
    @Volatile private var lastSuccessAt: Long = 0L

    fun processImageProxy(proxy: ImageProxy) {
        if (!_scanning.value) {
            proxy.close()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastSuccessAt < 1_000L) {
            proxy.close()
            return
        }
        try {
            val buffer = proxy.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val width = proxy.width
            val height = proxy.height
            val source = PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result: Result? = try {
                reader.decodeWithState(bitmap)
            } catch (_: Exception) {
                null
            }
            val text = result?.text
            if (text != null) {
                val parsed = parseQrPayload(text)
                if (parsed != null) {
                    lastSuccessAt = now
                    _scanning.value = false
                    _frameFlash.value = FrameFlash.Success
                    _events.tryEmit(QrScanEvent.Success(parsed))
                } else {
                    _frameFlash.value = FrameFlash.Error
                    _events.tryEmit(QrScanEvent.InvalidPayload)
                }
            }
        } finally {
            proxy.close()
        }
    }

    /** 拍肩后清 flash；border 颜色回普通绿色。 */
    fun clearFlash() {
        _frameFlash.value = FrameFlash.None
    }

    fun resumeScanning() {
        _scanning.value = true
    }
}

enum class FrameFlash { None, Success, Error }
