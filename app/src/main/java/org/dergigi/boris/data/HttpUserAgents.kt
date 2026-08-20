package org.dergigi.boris.data

import org.dergigi.boris.BuildConfig

object HttpUserAgents {
    const val BORIS_UA =
        "Boris/${BuildConfig.VERSION_NAME} (Android; +https://github.com/dergigi/boris-android)"
    const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
}
