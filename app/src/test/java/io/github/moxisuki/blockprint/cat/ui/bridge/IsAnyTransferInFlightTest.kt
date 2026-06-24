package io.github.moxisuki.blockprint.cat.ui.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsAnyTransferInFlightTest {
    @Test
    fun empty_list_is_false() {
        assertFalse(isAnyTransferInFlight(emptyList()))
    }

    @Test
    fun single_running_is_true() {
        assertTrue(
            isAnyTransferInFlight(
                listOf(
                    TransferItem(
                        id = 1,
                        type = TransferType.UPLOAD,
                        fileName = "x",
                        totalBytes = 0L,
                        phase = TransferPhase.RUNNING,
                    )
                )
            )
        )
    }

    @Test
    fun done_phase_is_false() {
        assertFalse(
            isAnyTransferInFlight(
                listOf(
                    TransferItem(
                        id = 1,
                        type = TransferType.UPLOAD,
                        fileName = "x",
                        totalBytes = 0L,
                        phase = TransferPhase.DONE,
                    )
                )
            )
        )
    }

    @Test
    fun failed_phase_is_false() {
        assertFalse(
            isAnyTransferInFlight(
                listOf(
                    TransferItem(
                        id = 1,
                        type = TransferType.UPLOAD,
                        fileName = "x",
                        totalBytes = 0L,
                        phase = TransferPhase.FAILED,
                    )
                )
            )
        )
    }

    @Test
    fun mixed_list_with_any_running_is_true() {
        assertTrue(
            isAnyTransferInFlight(
                listOf(
                    TransferItem(
                        id = 1,
                        type = TransferType.DOWNLOAD,
                        fileName = "a",
                        totalBytes = 0L,
                        phase = TransferPhase.DONE,
                    ),
                    TransferItem(
                        id = 2,
                        type = TransferType.UPLOAD,
                        fileName = "b",
                        totalBytes = 0L,
                        phase = TransferPhase.RUNNING,
                    ),
                )
            )
        )
    }
}
