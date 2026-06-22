package io.github.moxisuki.blockprint.cat

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.tencent.bugly.crashreport.CrashReport
import io.github.moxisuki.blockprint.cat.BuildConfig
import io.github.moxisuki.blockprint.cat.data.IconIndexResolver
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.settings.AppIconManager
import io.github.moxisuki.blockprint.cat.data.settings.TermsAcceptance
import io.github.moxisuki.blockprint.cat.ui.render.GlbResourceManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class BlockPrintCatApp : Application(), ImageLoaderFactory {

    @Inject lateinit var iconIndexResolver: IconIndexResolver
    @Inject lateinit var cookieStore: McschematicCookieStore
    @Inject lateinit var glbCacheDao: GlbCacheDao
    @Inject lateinit var appIconManager: AppIconManager
    @Inject lateinit var termsAcceptance: TermsAcceptance

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        iconIndexResolver.init(this)
        GlbResourceManager.init(this, glbCacheDao)
        appIconManager.reconcile()

        // Bugly 合规要求：仅在用户已同意隐私条款后才初始化 SDK。
        // 首次启动 → TermsGate 展示 → 用户同意 → initBuglyIfConsented()
        // 后续启动 → 已同意 → 这里直接初始化
        if (termsAcceptance.isAccepted()) {
            initBuglyIfConsented()
        }

        appScope.launch(Dispatchers.IO) {
            iconIndexResolver.ensureLoaded()
        }
    }

    /**
     * 由 TermsGate 在用户点击"我已阅读并同意"时调用。
     * 满足 Bugly SDK 合规要求（延迟初始化）。
     */
    fun initBuglyIfConsented() {
        if (BuildConfig.BUGLY_APP_ID.isNotEmpty() && termsAcceptance.isAccepted()) {
            CrashReport.initCrashReport(this, BuildConfig.BUGLY_APP_ID, BuildConfig.DEBUG)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cookieInterceptor = Interceptor { chain ->
            val cookieHeader = cookieStore.cookies().toHeaderValue()
            val request = if (cookieHeader.isNotEmpty()) {
                chain.request().newBuilder()
                    .header("Cookie", cookieHeader)
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(cookieInterceptor)
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(true)
            .crossfade(250)
            .build()
    }
}
