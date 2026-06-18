package io.github.moxisuki.blockprint.cat

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import io.github.moxisuki.blockprint.cat.data.IconIndexResolver
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.settings.AppIconManager
import io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        iconIndexResolver.init(this)
        RenderResourceManager.init(this, glbCacheDao)
        appIconManager.reconcile()

        appScope.launch(Dispatchers.IO) {
            iconIndexResolver.ensureLoaded()
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
