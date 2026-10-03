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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.location
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationBottomSheet(
    initialLocation: String?,
    onLocationEdited: (String?) -> Unit,
    onDismiss: () -> Unit
    ) {

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialLocation ?: "",
                selection = TextRange(initialLocation?.length ?: 0)
            )
        )
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        focusRequester.requestFocus()
    }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.location),
        menuActionRight = {
            TextButton(
                onClick = {
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.done))
            }
        },
        menuActionLeft = {
            TextButton(
                onClick = {
                    textFieldValue = TextFieldValue("")
                    onLocationEdited(null)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.delete))
            }
        }
    ) {

            TextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onLocationEdited(it.text.ifBlank { null })
                                },
                placeholder = { Text(stringResource(Res.string.location)) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (textFieldValue.text.isNotBlank()) {
                                onLocationEdited(null)
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
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.PostalAddress,
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
private fun EditLocationBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            EditLocationBottomSheet(
                initialLocation = "Am Stadtpark 3a/4/c, 1020 Wien",
                onLocationEdited = {},
                onDismiss = { }
            )
        }
    }
}

@Preview
@Composable
private fun EditLocationBottomSheet_empty_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            EditLocationBottomSheet(
                initialLocation = null,
                onLocationEdited = {},
                onDismiss = { }
            )
        }
    }
}