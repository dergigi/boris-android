package org.dergigi.boris.ui.reader

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
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
    val httpsUrl = remember(url) { UrlExtractor.preferHttps(url) }
    var aspect by remember(httpsUrl) { mutableStateOf<Float?>(null) }
    val ratio = aspect ?: FALLBACK_ASPECT
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clipToBounds(),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(httpsUrl)
                .build(),
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
                .then(if (fullWidth) Modifier else Modifier.heightIn(max = maxHeight))
                .clip(RoundedCornerShape(6.dp))
                .clickable { onClick(httpsUrl) },
        )
    }
}
