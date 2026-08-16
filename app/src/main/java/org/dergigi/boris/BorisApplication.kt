package org.dergigi.boris

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import okio.Path.Companion.toOkioPath
import org.dergigi.boris.data.CacheLimit

class BorisApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(CacheLimit.bytes(this))
                    .build()
            }
            .build()
}
