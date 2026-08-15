package org.dergigi.boris.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import org.dergigi.boris.R

private data class Feature(
    val asset: String,
    @StringRes val title: Int,
    val paragraphs: List<Int>,
)

private val FEATURES = listOf(
    Feature(
        asset = "read-anywhere.svg",
        title = R.string.about_read_anywhere_title,
        paragraphs = listOf(R.string.about_read_anywhere_1, R.string.about_read_anywhere_2),
    ),
    Feature(
        asset = "distraction-free.svg",
        title = R.string.about_distraction_free_title,
        paragraphs = listOf(R.string.about_distraction_free_1, R.string.about_distraction_free_2),
    ),
    Feature(
        asset = "airplane-mode.svg",
        title = R.string.about_airplane_mode_title,
        paragraphs = listOf(R.string.about_airplane_mode_1, R.string.about_airplane_mode_2),
    ),
    Feature(
        asset = "swarm-highlights.svg",
        title = R.string.about_social_highlights_title,
        paragraphs = listOf(R.string.about_social_highlights_1, R.string.about_social_highlights_2),
    ),
    Feature(
        asset = "reading-list.svg",
        title = R.string.about_lists_title,
        paragraphs = listOf(R.string.about_lists_1, R.string.about_lists_2),
    ),
    Feature(
        asset = "zap-splits.svg",
        title = R.string.about_zap_splits_title,
        paragraphs = listOf(R.string.about_zap_splits_1, R.string.about_zap_splits_2),
    ),
    Feature(
        asset = "comforting-colors.svg",
        title = R.string.about_comforting_colors_title,
        paragraphs = listOf(R.string.about_comforting_colors_1),
    ),
    Feature(
        asset = "peace-of-mind.svg",
        title = R.string.about_peace_of_mind_title,
        paragraphs = listOf(R.string.about_peace_of_mind_1, R.string.about_peace_of_mind_2),
    ),
    Feature(
        asset = "free-forever.svg",
        title = R.string.about_free_title,
        paragraphs = listOf(R.string.about_free_1, R.string.about_free_2),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "intro") {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.about_intro_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.about_intro_body),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(FEATURES, key = { it.asset }) { feature ->
                FeatureCard(feature)
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/features/${feature.asset}")
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = stringResource(feature.title),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Text(
            text = stringResource(feature.title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        feature.paragraphs.forEach { paragraph ->
            Text(
                text = stringResource(paragraph),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
