package io.github.moxisuki.blockprint.cat.ui.community

import io.github.moxisuki.blockprint.cat.data.community.CommunitySource
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityStateTest {

    @Test fun copyActive_MCS_doesNotTouchCMS() {
        val s = CommunityListState(currentSource = CommunitySource.MCS)
        val updated = s.copyActive { copy(loading = true, filter = "foo") }
        assertEquals(true, updated.mcs.loading)
        assertEquals("foo", updated.mcs.filter)
        assertEquals(s.cms, updated.cms)
    }

    @Test fun copyActive_CMS_doesNotTouchMCS() {
        val s = CommunityListState(currentSource = CommunitySource.CMS)
        val updated = s.copyActive { copy(loading = true, filter = "bar") }
        assertEquals(true, updated.cms.loading)
        assertEquals("bar", updated.cms.filter)
        assertEquals(s.mcs, updated.mcs)
    }

    @Test fun active_reflectsCurrentSource() {
        val mcs = CommunityListState().copy(
            currentSource = CommunitySource.MCS,
            mcs = PerSourceState(loading = true),
        )
        assertEquals(true, mcs.active.loading)
        val cms = mcs.copy(currentSource = CommunitySource.CMS)
        assertEquals(false, cms.active.loading)
    }
}
