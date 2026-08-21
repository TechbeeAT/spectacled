package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import io.ktor.http.Url
import io.ktor.http.parseUrl
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.link_file_by_url
import spectacled.shared.generated.resources.link_file_by_url_hint
import spectacled.shared.generated.resources.url
import kotlin.time.Duration.Companion.milliseconds

/**
 * Prompts for a URL that points at an externally-hosted file and adds it as a *linked* attachment
 * (ATTACH:<url>) — the bytes stay on their server, so this works on any CalDAV server regardless of
 * whether it supports storing attachments. The URL is validated as a well-formed http(s) link; it
 * is deliberately not fetched here (it may be auth-gated, large, or offline) — download happens
 * lazily when the attachment is opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUrlAttachmentBottomSheet(
    onUrlConfirmed: (Url) -> Unit,
    onDismiss: () -> Unit
) {

    var text by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Only accept well-formed absolute http(s) URLs — the pointer is useless if it can't be fetched.
    val url by derivedStateOf {
        parseUrl(text)?.takeIf { it.protocol.name == "http" || it.protocol.name == "https" }
    }

    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        focusRequester.requestFocus()
    }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.link_file_by_url),
        menuActionRight = {
            TextButton(
                onClick = {
                    url?.let { onUrlConfirmed(it) }
                    onDismiss()
                },
                enabled = url != null
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        menuActionLeft = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            TextField(
                value = text,
                isError = text.isNotBlank() && url == null,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(Res.string.url)) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            text = ""
                            keyboardController?.hide()
                        },
                        enabled = text.isNotBlank(),
                        content = { Icon(Icons.Outlined.Clear, stringResource(Res.string.delete)) }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        url?.let { onUrlConfirmed(it) }
                        onDismiss()
                    }
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            Text(
                text = stringResource(Res.string.link_file_by_url_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}


@Preview
@Composable
private fun AddUrlAttachmentBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            AddUrlAttachmentBottomSheet(
                onUrlConfirmed = { },
                onDismiss = { }
            )
        }
    }
}
