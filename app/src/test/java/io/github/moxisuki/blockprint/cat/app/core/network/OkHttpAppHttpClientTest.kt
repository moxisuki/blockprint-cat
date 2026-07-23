package io.github.moxisuki.blockprint.cat.app.core.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OkHttpAppHttpClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpAppHttpClient

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        client = OkHttpAppHttpClient(OkHttpClient())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun `getJson parses a json body`() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"hello\":\"world\"}"))
        val result = client.getJson(server.url("/anything").toString())
        assertThat(result).isInstanceOf(AppNetworkResult.Success::class.java)
        assertThat((result as AppNetworkResult.Success).value.getString("hello"))
            .isEqualTo("world")
    }

    @Test fun `getBytes invokes progress callback`() = runBlocking {
        server.enqueue(MockResponse().setBody("hello".repeat(20)))
        val seen = mutableListOf<Float>()
        val result = client.getBytes(server.url("/file").toString()) { seen.add(it) }
        assertThat(result).isInstanceOf(AppNetworkResult.Success::class.java)
        assertThat(seen.last()).isEqualTo(1f)
    }

    @Test fun `non 2xx returns Failure with code`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val result = client.getJson(server.url("/oops").toString())
        assertThat(result).isInstanceOf(AppNetworkResult.Failure::class.java)
        assertThat((result as AppNetworkResult.Failure).code).isEqualTo(500)
    }
}
