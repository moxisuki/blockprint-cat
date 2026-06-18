package io.github.moxisuki.blockprint.cat.data.community

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CmsClient"

internal const val BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 13; BlockPrintCat) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36"

@Singleton
class CmsClient @Inject constructor(
    private val cookieStore: CmsCookieStore,
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String = "https://www.creativemechanicserver.com",
) {

    companion object {
        const val PAGE_SIZE = 15
    }

    suspend fun ensureCsrf(): String = withContext(Dispatchers.IO) {
        // Sync cookies from WebView (Cloudflare clearance etc.)
        cookieStore.importFromWebView(baseUrl)
        // Always re-fetch form token — cookie may have changed
        cookieStore.remove("csrf_form_token")
        val url = "$baseUrl/tree/"
        Log.i(TAG, "GET $url (ensureCsrf)")
        val req = mkReq(url).header("Referer", "$baseUrl/").build()
        val resp = okHttpClient.newCall(req).execute()
        val code = resp.code
        if (code == 403) {
            val body = resp.body?.string().orEmpty()
            if (body.contains("Just a moment") || body.contains("challenge")) throw CmsCloudflareException()
            throw CmsException("HTTP 403 GET $url")
        }
        if (code !in 200..299) throw CmsException("HTTP $code GET $url")
        captureSetCookies(resp)
        // Extract csrfmiddlewaretoken from the HTML form
        val html = resp.body?.string() ?: throw CmsException("Empty body")
        val formToken = Regex("csrfmiddlewaretoken\" value=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            ?: throw CmsException("未找到 CSRF form token")
        cookieStore.put("csrf_form_token", formToken)
        cookieStore.saveToDisk()
        Log.d(TAG, "CSRF form token obtained: ${formToken.take(16)}...")
        formToken
    }

    suspend fun search(query: String, sort: String = "time", page: Int = 1): CmsSearchPage = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.take(100), "UTF-8")
        val csrf = ensureCsrf()

        // POST /search/ — the actual AJAX API endpoint
        val body = okhttp3.FormBody.Builder()
            .add("csrfmiddlewaretoken", csrf)
            .add("search_type", "t")
            .add("q", query.take(100))
            .add("loader_type", "any")
            .add("mc_type", "any")
            .add("create_type", "any")
            .add("sort", sort)
            .add("order", "down")
            .add("grid_col", "1")
            .add("page", page.toString())
            .build()
        val req = mkReq("$baseUrl/search/").header("Referer", "$baseUrl/tree/").post(body).build()
        val resp = okHttpClient.newCall(req).execute()
        val html = resp.body?.string() ?: throw CmsException("Empty search response")
        Log.i(TAG, "search response: ${resp.code} ${html.length} chars")
        Log.i(TAG, "search HTML: ${html.take(2000)}")
        val result = CmsParsers.parseSearchHtml(html, page)
        Log.i(TAG, "search results: ${result.items.size} items (page=$page)")
        result
    }

    /** Quick test: can we get search results? Returns false if Cloudflare blocks us. */
    suspend fun testSearch(): Boolean = withContext(Dispatchers.IO) {
        try {
            val page = search("", "time", 1)
            page.items.isNotEmpty()
        } catch (e: CmsCloudflareException) { throw e }
        catch (e: Exception) { false }
    }

    suspend fun fetchDetail(detailId: Int): CmsDetail = withContext(Dispatchers.IO) {
        val csrf = ensureCsrf()
        val url = "$baseUrl/detail/$detailId/?csrfmiddlewaretoken=${URLEncoder.encode(csrf, "UTF-8")}"
        val html = getText(url)
        Log.i(TAG, "detail HTML: ${html.length} chars, has_tip_box=${html.contains("tip_box")}, has_cover=${html.contains("class=\"cover\"")}")
        val detail = CmsParsers.parseDetailHtml(html, detailId)
        Log.i(TAG, "detail parsed: downloadId=${detail.downloadId}, materials=${detail.materials.size}, production=${detail.production.size}, cover=${detail.coverUrl}")
        detail
    }

    suspend fun fetchBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val resp = okHttpClient.newCall(mkReq(url).build()).execute()
        if (resp.code == 404) return@withContext ByteArray(0)
        if (resp.code !in 200..299) throw CmsException("HTTP ${resp.code} for $url")
        resp.body?.bytes() ?: ByteArray(0)
    }

    suspend fun resolveDownloadUrl(downloadPageId: Int): String = withContext(Dispatchers.IO) {
        val pageUrl = "$baseUrl/download/$downloadPageId/"
        val html = getText(pageUrl)
        val filePath = Regex("""href="(/upload/blueprint/[^"]+)"""").find(html)?.groupValues?.get(1)
            ?: throw CmsException("未找到下载文件链接")
        "$baseUrl$filePath"
    }

    suspend fun downloadToFile(
        fileUrl: String, targetDir: File, fileName: String,
        onProgress: suspend (Long, Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        Log.i(TAG, "downloadToFile: $fileUrl")
        if (!targetDir.exists()) targetDir.mkdirs()
        val tempFile = File(targetDir, "$fileName.download")
        val resp = okHttpClient.newCall(mkReq(fileUrl).build()).execute()
        if (resp.code !in 200..299) throw CmsException("HTTP ${resp.code} for download")
        val body = resp.body ?: throw CmsException("Empty body")
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buf = ByteArray(64 * 1024); var cumulative = 0L
                while (true) {
                    val n = input.read(buf); if (n <= 0) break
                    output.write(buf, 0, n); cumulative += n
                    onProgress(cumulative, totalBytes)
                }
            }
        }
        val finalFile = File(targetDir, fileName)
        if (finalFile.exists()) finalFile.delete()
        if (!tempFile.renameTo(finalFile)) { tempFile.copyTo(finalFile, overwrite = true); tempFile.delete() }
        Log.i(TAG, "downloadToFile done: ${finalFile.length()} bytes, ${System.currentTimeMillis() - t0}ms")
        finalFile
    }

    private suspend fun getText(url: String, referer: String = "$baseUrl/tree/"): String = withContext(Dispatchers.IO) {
        Log.i(TAG, "GET $url")
        val resp = okHttpClient.newCall(mkReq(url).header("Referer", referer).build()).execute()
        val body = resp.body?.string() ?: throw CmsException("Empty body")
        Log.i(TAG, "← HTTP ${resp.code} (${body.length} chars)")
        if (resp.code == 403) {
            if (body.contains("Just a moment") || body.contains("challenge")) throw CmsCloudflareException()
        }
        if (resp.code !in 200..299) throw CmsException("HTTP ${resp.code} for $url")
        body
    }

    private fun mkReq(url: String): Request.Builder =
        Request.Builder().url(url).header("User-Agent", BROWSER_UA)
            .also { b ->
                val h = cookieStore.headerValue()
                if (h.isNotEmpty()) b.header("Cookie", h)
            }

    private fun captureSetCookies(resp: Response) {
        val setCookies = resp.headers("Set-Cookie")
        for (sc in setCookies) {
            val parts = sc.split(";").firstOrNull()?.split("=", limit = 2) ?: continue
            if (parts.size == 2) cookieStore.put(parts[0].trim(), parts[1].trim())
        }
        cookieStore.saveToDisk()
    }
}
