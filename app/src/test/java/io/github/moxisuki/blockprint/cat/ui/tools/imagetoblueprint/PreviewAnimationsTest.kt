package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PreviewAnimationsTest {
    @Test fun `durations match design spec`() {
        assertThat(PreviewAnimations.TINY).isEqualTo(120)
        assertThat(PreviewAnimations.SHORT).isEqualTo(220)
        assertThat(PreviewAnimations.MEDIUM).isEqualTo(320)
        assertThat(PreviewAnimations.LONG).isEqualTo(480)
    }

    @Test fun `standard easing uses cubic bezier 0_45 0 0_25 1`() {
        val s = PreviewAnimations.EasingStandard
        assertThat(s.toString()).contains("0.45")
        assertThat(s.toString()).contains("0.25")
    }
}
