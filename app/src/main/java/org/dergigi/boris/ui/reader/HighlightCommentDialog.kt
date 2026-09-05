package org.dergigi.boris.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

data class PendingHighlightAnnotation(
    val quote: String,
    val ownerText: String,
    val ownerOffset: Int,
)

@Composable
fun HighlightCommentDialog(
    quote: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by rememberSaveable { mutableStateOf("") }
    val trimmed = comment.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.highlight_comment_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (quote.isNotBlank()) {
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.highlight_comment_hint)) },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (trimmed.isNotEmpty()) onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty(),
            ) {
                Text(stringResource(R.string.highlight_comment_publish))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
