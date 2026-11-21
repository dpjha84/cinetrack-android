package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.util.AnalyticsTracker
import com.example.util.TrailerInterstitialAdManager

class WatchPilotApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AnalyticsTracker.init(this)
    }

    private var cachedImageLoader: ImageLoader? = null

    override fun newImageLoader(): ImageLoader {
        return cachedImageLoader ?: ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.12)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(40L * 1024L * 1024L)
                    .build()
            }
            .allowRgb565(true)
            .crossfade(true)
            .build()
            .also { cachedImageLoader = it }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        cachedImageLoader?.memoryCache?.let { memCache ->
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                memCache.clear()
            } else {
                memCache.trimMemory(level)
            }
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            TrailerInterstitialAdManager.releasePreloaded()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        cachedImageLoader?.memoryCache?.clear()
        TrailerInterstitialAdManager.releasePreloaded()
    }
}
