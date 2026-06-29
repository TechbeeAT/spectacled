package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import io.ktor.http.Url
import io.ktor.http.parseUrl
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.url

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUrlBottomSheet(
    initialUrl: Url?,
    onUrlEdited: (Url?) -> Unit,
    onDismiss: () -> Unit
    ) {

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialUrl?.toString() ?: "",
                selection = TextRange(initialUrl?.toString()?.length ?: 0)
            )
        )
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val url by derivedStateOf { parseUrl(textFieldValue.text) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.url),
        menuAction = {
            TextButton(
                onClick = {
                    textFieldValue = TextFieldValue("")
                    onUrlEdited(null)
                }
            ) {
                Text(stringResource(Res.string.delete))
            }
        }
    ) {

            TextField(
                value = textFieldValue,
                isError = url == null,
                onValueChange = {
                    textFieldValue = it
                    onUrlEdited(parseUrl(it.text))
                                },
                placeholder = { Text(stringResource(Res.string.url)) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (textFieldValue.text.isNotBlank()) {
                                onUrlEdited(null)
                                textFieldValue = TextFieldValue("")
                            }
                            keyboardController?.hide()
                        },
                        enabled = textFieldValue.text.isNotBlank(),
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
                    onDone = { onDismiss() }
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

    }
}


@Preview
@Composable
private fun AddSubtaskBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            EditUrlBottomSheet(
                initialUrl = Url("https://techbee.at"),
                onUrlEdited = { },
                onDismiss = { }
            )
        }
    }
}

@Preview
@Composable
private fun AddSubtaskBottomSheet_Error_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            EditUrlBottomSheet(
                initialUrl = null,
                onUrlEdited = { },
                onDismiss = { }
            )
        }
    }
}