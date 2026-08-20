package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.account.presentation.ProcessingState
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.password
import spectacled.shared.generated.resources.show_error_details
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.update_password
import spectacled.shared.generated.resources.update_password_button

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePrincipalPasswordBottomSheet(
    sheetState: SheetState,
    principal: Principal,
    processingState: ProcessingState,
    onAction: (AccountListAction.OnUpdatePrincipalPassword) -> Unit,
    onDismiss: () -> Unit,
) {

    val passwordState = rememberTextFieldState()
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    LaunchedEffect(processingState) {
        if(processingState is ProcessingState.Success)
            onDismiss()   // close when successful
    }

    BottomSheetWithMenu(
        headline = stringResource(Res.string.update_password),
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        gesturesEnabled = false,
        showLoadingIndicator = processingState is ProcessingState.Processing,
        menuActionLeft = {
            TextButton(
                onClick = { onDismiss() },
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        menuActionRight = {
            TextButton(
                onClick = {
                    onAction(AccountListAction.OnUpdatePrincipalPassword(principal, passwordState.text.toString()))
                },
                enabled = passwordState.text.isNotBlank() && processingState !is ProcessingState.Processing
            ) {
                Text(stringResource(Res.string.update_password_button))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxSize()
        ) {

            principal.displayName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Text(
                text = principal.principalUrl.toString(),
                style = MaterialTheme.typography.bodyMedium
            )

            val error = processingState as? ProcessingState.Error
            AnimatedVisibility(error != null) {
                Text(
                    text = error?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(error?.detail?.isNotEmpty() == true && !showMore) {
                TextButton(
                    onClick = { showMore = true }
                ) {
                    Text(stringResource(Res.string.show_error_details))
                }
            }

            AnimatedVisibility(showMore) {
                error?.detail?.let {
                    Text(
                        text = it,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedSecureTextField(
                state = passwordState,
                //placeholder = { Text("******") },
                //supportingText = { Text("Optional") },
                label = { Text(stringResource(Res.string.password)) },
                textObfuscationMode = if (isPasswordVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Crossfade(isPasswordVisible) { visible ->
                            if (visible) Icon(
                                Icons.Outlined.Visibility,
                                contentDescription = stringResource(Res.string.show_hide_password)
                            ) else Icon(Icons.Outlined.VisibilityOff, contentDescription = stringResource(Res.string.show_hide_password))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                    //imeAction = ImeAction.Done
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun UpdatePrincipalPasswordBottomSheet_Preview_Idle() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            UpdatePrincipalPasswordBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                principal = Principal.getPrincipalForPreview(),
                //credentials = Credentials("https://localhost/dav", "my username", "my password"),
                processingState = ProcessingState.Idle,
                onAction = {},
                onDismiss = {}
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun UpdatePrincipalPasswordBottomSheet_Preview_Processing() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            UpdatePrincipalPasswordBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                principal = Principal.getPrincipalForPreview(),
                //credentials = Credentials("https://localhost/dav", "my username", "my password"),
                processingState = ProcessingState.Processing,
                onAction = {},
                onDismiss = {}
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun UpdatePrincipalPasswordBottomSheet_Preview_Error() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            UpdatePrincipalPasswordBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                principal = Principal.getPrincipalForPreview(),
                //credentials = Credentials("https://localhost/dav", "my username", "my password"),
                processingState = ProcessingState.Error("This is an error", "Here are the details"),
                onAction = {},
                onDismiss = {}
            )
        }
    }

}
