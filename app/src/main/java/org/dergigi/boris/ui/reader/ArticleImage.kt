package org.dergigi.boris.ui.reader

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import org.dergigi.boris.data.ArticleImages
import org.dergigi.boris.data.UrlExtractor

private const val FALLBACK_ASPECT = 16f / 9f

@Composable
internal fun ArticleImage(
    url: String,
    fullWidth: Boolean,
    maxHeight: Dp,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.roundToPx()
    }
    val heightPx = with(density) { maxHeight.roundToPx() }
    val httpsUrl = remember(url) { UrlExtractor.preferHttps(url) }
    var aspect by remember(httpsUrl) { mutableStateOf<Float?>(null) }
    val ratio = aspect ?: FALLBACK_ASPECT
    val request = remember(httpsUrl, widthPx, heightPx, fullWidth) {
        articleImageRequest(context, httpsUrl, widthPx, heightPx)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clipToBounds(),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { result ->
                val size = result.painter.intrinsicSize
                if (size.width > 0f && size.height > 0f) {
                    aspect = size.width / size.height
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .heightIn(max = maxHeight)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onClick(httpsUrl) },
        )
    }
}

internal fun articleImageRequest(
    context: Context,
    url: String,
    widthPx: Int,
    heightPx: Int,
): ImageRequest = ImageRequest.Builder(context)
    .data(ArticleImages.displaySource(url))
    .size(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
    .precision(Precision.INEXACT)
    .build()
