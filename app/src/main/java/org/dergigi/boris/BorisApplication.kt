package org.dergigi.boris

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.maxBitmapSize
import coil3.size.Size
import okio.Path.Companion.toOkioPath
import org.dergigi.boris.data.CacheLimit

class BorisApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .maxBitmapSize(Size(ARTICLE_BITMAP_MAX, ARTICLE_BITMAP_MAX))
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(CacheLimit.bytes(this))
                    .build()
            }
            .build()

    companion object {
        private const val ARTICLE_BITMAP_MAX = 1600
    }
}
