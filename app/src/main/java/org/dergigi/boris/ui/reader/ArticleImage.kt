package org.dergigi.boris.ui.reader

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size

@Composable
internal fun ArticleImage(
    url: String,
    fullWidth: Boolean,
    maxHeight: Dp,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var aspect by remember(url) { mutableStateOf<Float?>(null) }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .size(Size.ORIGINAL)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onSuccess = { result ->
            val size = result.painter.intrinsicSize
            if (size.width > 0f && size.height > 0f) {
                aspect = size.width / size.height
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(aspect?.let { Modifier.aspectRatio(it) } ?: Modifier)
            .then(if (fullWidth) Modifier else Modifier.heightIn(max = maxHeight))
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick(url) },
    )
}
