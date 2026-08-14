package org.dergigi.boris.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.ui.auth.AuthBar
import org.dergigi.boris.ui.auth.AuthViewModel

const val DEFAULT_ARTICLE_URL = "https://www.citadel21.com/the-paranoid-wallet"

@Composable
fun HomeScreen(
    onRead: (String) -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    var url by rememberSaveable { mutableStateOf("") }
    val extracted = UrlExtractor.extract(url.trim().ifEmpty { DEFAULT_ARTICLE_URL })
    val canRead = extracted != null
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val authMessage by viewModel.message.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignerResult(result.resultCode, result.data)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    fun submit() {
        extracted?.let(onRead)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hello! I'm Boris.",
                style = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "I like to read.",
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            AuthBar(
                state = authState,
                message = authMessage,
                onConnect = {
                    viewModel.connectIntent()?.let(launcher::launch)
                },
                onSignOut = viewModel::signOut,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(DEFAULT_ARTICLE_URL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = ::submit,
                enabled = canRead,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Read")
            }
        }
    }
}
