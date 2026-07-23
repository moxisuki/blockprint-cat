package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.testfakes.FakeAppHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModrinthSourceTest {
    private val searchBody = """
        {"hits":[{"slug":"create","title":"Create","description":"mechanical mod","project_id":"PJ1"}]}
    """.trimIndent()

    @Test fun `searchMods parses hits list`() = runTest {
        val src = ModrinthSource(FakeAppHttpClient(jsonResponses = mapOf(
            "${AssetMirrors.MODRINTH_API}/search?query=create&limit=10&index=downloads" to searchBody,
        )))
        val hits = src.searchMods("create")
        assertThat(hits).hasSize(1)
        assertThat(hits.first().slug).isEqualTo("create")
    }

    @Test fun `versionsFor parses fileUrl and fileName`() = runTest {
        val body = """
            [{"id":"V1","name":"1.0","game_versions":["1.20.1"],"loaders":["forge"],
              "files":[{"primary":true,"url":"https://files.test/create.jar","filename":"create.jar","size":4096}]}]
        """.trimIndent()
        val src = ModrinthSource(FakeAppHttpClient(stringResponses = mapOf(
            "${AssetMirrors.MODRINTH_API}/project/PJ1/version" to body,
        )))
        val versions = src.versionsFor("PJ1")
        assertThat(versions).hasSize(1)
        assertThat(versions.first().fileName).isEqualTo("create.jar")
        assertThat(versions.first().fileSize).isEqualTo(4096L)
    }
}
