package org.dergigi.boris.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object BorisIcons {
    val Highlighter: ImageVector
        get() {
            val cached = highlighter
            if (cached != null) return cached
            val next = ImageVector.Builder(
                name = "Highlighter",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 576f,
                viewportHeight = 512f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(HighlighterPath).toNodes(),
                    fill = SolidColor(Color.Black),
                )
            }.build()
            highlighter = next
            return next
        }

    val Books: ImageVector
        get() {
            val cached = books
            if (cached != null) return cached
            val next = ImageVector.Builder(
                name = "Books",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 512f,
                viewportHeight = 512f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(BooksPath).toNodes(),
                    fill = SolidColor(Color.Black),
                )
            }.build()
            books = next
            return next
        }
}

private var highlighter: ImageVector? = null
private var books: ImageVector? = null

private const val HighlighterPath =
    "M315 315L473.4 99.9 444.1 70.6 229 229 315 315zm-187 5l0 0 0-71.7c0-15.3 7.2-29.6 19.5-38.6L420.6 8.4C428 2.9 437 0 446.2 0 457.6 0 468.5 4.5 476.6 12.6l54.8 54.8c8.1 8.1 12.6 19 12.6 30.5 0 9.2-2.9 18.2-8.4 25.6L334.4 396.5c-9 12.3-23.4 19.5-38.6 19.5l-71.7 0-25.4 25.4c-12.5 12.5-32.8 12.5-45.3 0l-50.7-50.7c-12.5-12.5-12.5-32.8 0-45.3L128 320zM7 466.3l51.7-51.7 70.6 70.6-19.7 19.7c-4.5 4.5-10.6 7-17 7L24 512c-13.3 0-24-10.7-24-24l0-4.7c0-6.4 2.5-12.5 7-17z"

private const val BooksPath =
    "M510.354 435.363L402.686 35.422C396.939 14.078 377.547 0 356.354 0C352.242 0 348.059 0.531 343.896 1.641L282.078 18.125C276.193 19.695 270.939 22.383 266.295 25.758C258.254 10.508 242.436 0 224 0H160C151.213 0 143.084 2.531 136 6.656C128.916 2.531 120.787 0 112 0H48C21.49 0 0 21.492 0 48V464C0 490.508 21.49 512 48 512H112C120.787 512 128.916 509.469 136 505.344C143.084 509.469 151.213 512 160 512H224C250.51 512 272 490.508 272 464V165.281L355.805 476.578C361.553 497.926 380.945 512 402.139 512C406.25 512 410.432 511.469 414.594 510.359L476.412 493.875C502.018 487.043 517.215 460.848 510.354 435.363ZM224 48V96H160V48H224ZM160 144H224V368H160V144ZM112 368H48V144H112V368ZM112 48V96H48V48H112ZM48 464V416H112V464H48ZM160 464V416H224V464H160ZM294.445 64.504L356.271 48.02L356.361 48L368.742 93.93L306.828 110.445L294.445 64.504ZM319.266 156.586L381.18 140.074L439.223 355.41L377.309 371.922L319.266 156.586ZM402.154 464.102L389.746 418.066L451.66 401.555L464.045 447.496L402.154 464.102Z"
