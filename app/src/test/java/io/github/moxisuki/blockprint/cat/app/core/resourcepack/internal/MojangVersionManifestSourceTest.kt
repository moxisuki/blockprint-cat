package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import io.github.moxisuki.blockprint.cat.app.testfakes.FakeAppHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MojangVersionManifestSourceTest {
    @Test fun `latestVersion returns parsed release string`() = runTest {
        val body = """
            { "latest": { "release": "1.21.4", "snapshot": "1.21.5" },
              "versions": [
                { "id": "1.21.4", "url": "https://example.test/1.21.4.json" },
                { "id": "1.21.3", "url": "https://example.test/1.21.3.json" }
              ]
            }
        """.trimIndent()
        val source = MojangVersionManifestSource(
            FakeAppHttpClient(jsonResponses = mapOf("https://bmclapi2.bangbang93.com/mc/game/version_manifest.json" to body)),
        )
        val result = source.latestRelease()
        assertThat(result).isInstanceOf(AppNetworkResult.Success::class.java)
        assertThat((result as AppNetworkResult.Success).value).isEqualTo("1.21.4")
    }

    @Test fun `versionJson returns parsed json object for given id`() = runTest {
        val body = """{"downloads":{"client":{"url":"https://example.test/client.jar","size":42}}}"""
        val source = MojangVersionManifestSource(
            FakeAppHttpClient(jsonResponses = mapOf("https://example.test/1.21.4.json" to body)),
        )
        val result = source.versionJson("https://example.test/1.21.4.json")
        assertThat(result).isInstanceOf(AppNetworkResult.Success::class.java)
    }
}
