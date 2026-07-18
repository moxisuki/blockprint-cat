package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

object AppInteraction {
    val DefaultPressFeedback: PressFeedbackType = PressFeedbackType.Sink
    val ProminentPressFeedback: PressFeedbackType = PressFeedbackType.Tilt
    val BoundaryHapticFeedback: HapticFeedbackType = HapticFeedbackType.TextHandleMove
}

fun Modifier.appScrollEndHaptic(
    enabled: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = AppInteraction.BoundaryHapticFeedback,
): Modifier =
    if (enabled) {
        scrollEndHaptic(hapticFeedbackType = hapticFeedbackType)
    } else {
        this
    }
