package org.dergigi.boris.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.ImageSaveLocationStore
import org.dergigi.boris.data.UserSettings

@Composable
fun MediaSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var saveLocation by remember { mutableStateOf(ImageSaveLocationStore.load(context)) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.isSuccess || ImageSaveLocationStore.hasWritePermission(context, uri)
        if (!persisted) return@rememberLauncherForActivityResult
        ImageSaveLocationStore.save(context, uri)
        saveLocation = uri
    }
    LaunchedEffect(Unit) {
        saveLocation = ImageSaveLocationStore.load(context)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingCheckbox(
            label = stringResource(R.string.settings_full_width_images),
            checked = settings.fullWidthImages,
            onCheckedChange = { onUpdate(settings.withBoolean("fullWidthImages", it)) },
        )
        SettingRow(stringResource(R.string.settings_media_image_save_location)) {
            OutlinedButton(onClick = { folderPicker.launch(null) }) {
                Text(
                    ImageSaveLocationStore.displayName(saveLocation)
                        ?: stringResource(R.string.settings_media_image_save_location_default),
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_media_image_save_location_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (saveLocation != null) {
            OutlinedButton(
                onClick = {
                    ImageSaveLocationStore.clear(context)
                    saveLocation = null
                },
            ) {
                Text(stringResource(R.string.settings_media_image_save_location_reset))
            }
        }
    }
}
