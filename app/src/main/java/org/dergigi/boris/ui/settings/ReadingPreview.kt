package org.dergigi.boris.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.reader.HighlightMarks
import org.dergigi.boris.ui.reader.InkStroke
import org.dergigi.boris.ui.reader.InkUnderline
import org.dergigi.boris.ui.reader.paintHighlight
import org.dergigi.boris.ui.theme.rememberDisplayLook

@Composable
fun ReadingPreview(
    settings: UserSettings,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val family = ReadingFonts.family(settings.readingFont)
    val bodySize = settings.fontSize.sp
    val bodyLine = (settings.fontSize * 36f / 21f).sp
    val align = if (settings.justifyParagraphs) TextAlign.Justify else TextAlign.Start
    val look = rememberDisplayLook(settings)
    val underline = look.underline
    val eink = look.eink
    val show = settings.showHighlights
    val linkColor = look.link
    val mineColor = look.mine
    val friendsColor = look.friends
    val foafColor = look.foaf
    val nostrverseColor = look.nostrverse
    val mineStroke = HighlightMarks.highlightStroke(eink, mine = true)
    val otherStroke = HighlightMarks.highlightStroke(eink, mine = false)
    val linkStroke = if (eink) InkStroke(InkUnderline.Dotted, HighlightMarks.LinkStroke) else null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_preview).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.05.em,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = PreviewCopy.TITLE,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = (settings.fontSize * 1.5f).sp,
                lineHeight = (settings.fontSize * 1.5f * 1.25f).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        PreviewParagraph(
            text = PreviewCopy.P1,
            quote = PreviewCopy.MINE,
            color = mineColor,
            visible = show && settings.defaultHighlightVisibilityMine,
            underline = underline,
            stroke = mineStroke,
            family = family,
            fontSize = bodySize,
            lineHeight = bodyLine,
            align = align,
        )
        PreviewParagraph(
            text = PreviewCopy.P2,
            quote = PreviewCopy.FRIENDS,
            color = friendsColor,
            visible = show && settings.defaultHighlightVisibilityFriends,
            underline = underline,
            stroke = otherStroke,
            family = family,
            fontSize = bodySize,
            lineHeight = bodyLine,
            align = align,
        )
        PreviewParagraph(
            text = PreviewCopy.P_FOAF,
            quote = PreviewCopy.FOAF,
            color = foafColor,
            visible = show && settings.defaultHighlightVisibilityFoaf,
            underline = underline,
            stroke = otherStroke,
            family = family,
            fontSize = bodySize,
            lineHeight = bodyLine,
            align = align,
        )
        PreviewParagraph(
            text = PreviewCopy.P3,
            quote = PreviewCopy.NOSTRVERSE,
            color = nostrverseColor,
            visible = show && settings.defaultHighlightVisibilityNostrverse,
            underline = underline,
            stroke = otherStroke,
            family = family,
            fontSize = bodySize,
            lineHeight = bodyLine,
            align = align,
            link = PreviewCopy.LINK,
            linkColor = if (eink) Color.Unspecified else linkColor,
            linkStroke = linkStroke,
        )
    }
}

@Composable
private fun PreviewParagraph(
    text: String,
    quote: String,
    color: Color,
    visible: Boolean,
    underline: Boolean,
    stroke: InkStroke,
    family: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    align: TextAlign,
    link: String? = null,
    linkColor: Color = Color.Unspecified,
    linkStroke: InkStroke? = null,
) {
    val annotated = remember(text, link, linkColor) {
        if (link.isNullOrBlank() || linkColor == Color.Unspecified) {
            buildAnnotatedString { append(text) }
        } else {
            val start = text.indexOf(link)
            if (start < 0) {
                buildAnnotatedString { append(text) }
            } else {
                buildAnnotatedString {
                    append(text.substring(0, start))
                    withStyle(SpanStyle(color = linkColor)) { append(link) }
                    append(text.substring(start + link.length))
                }
            }
        }
    }
    val quoteStart = text.indexOf(quote)
    val linkStart = link?.let { text.indexOf(it) } ?: -1
    val bodyInk = MaterialTheme.colorScheme.onBackground
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated,
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
            val result = layout ?: return@drawBehind
            if (linkStroke != null && link != null && linkStart >= 0) {
                paintHighlight(
                    result,
                    linkStart,
                    linkStart + link.length,
                    bodyInk,
                    underline = true,
                    ink = linkStroke.ink,
                    stroke = linkStroke.width,
                )
            }
            if (!visible || quoteStart < 0) return@drawBehind
            paintHighlight(
                result,
                quoteStart,
                quoteStart + quote.length,
                color,
                underline,
                ink = stroke.ink,
                stroke = stroke.width,
            )
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = family,
            fontSize = fontSize,
            lineHeight = lineHeight,
            textAlign = align,
            letterSpacing = 0.sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

internal object PreviewCopy {
    const val TITLE = "The Quick Brown Fox"
    const val P1 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
    const val P2 =
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium."
    const val P_FOAF =
        "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident."
    const val P3 =
        "Totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit."
    const val MINE = "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
    const val FRIENDS =
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    const val FOAF = "blanditiis praesentium voluptatum deleniti atque corrupti"
    const val NOSTRVERSE =
        "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."
    const val LINK = "inventore veritatis"
}
