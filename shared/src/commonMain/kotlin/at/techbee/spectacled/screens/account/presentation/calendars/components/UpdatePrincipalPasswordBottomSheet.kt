package at.techbee.spectacled.screens.account.presentation.calendars.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListAction
import at.techbee.spectacled.screens.account.presentation.calendars.ProcessingState
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.password
import spectacled.shared.generated.resources.show_error_details
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.update_passowrd_button
import spectacled.shared.generated.resources.update_password
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePrincipalPasswordBottomSheet(
    sheetState: SheetState,
    principal: Principal,
    processingState: ProcessingState,
    onAction: (CalendarListAction.OnUpdatePrincipalPassword) -> Unit,
    onDismiss: () -> Unit,
) {

    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        showLoadingIndicator = processingState is ProcessingState.Processing,
        menuAction = {
            TextButton(
                onClick = {
                    onAction(CalendarListAction.OnUpdatePrincipalPassword(principal, password))
                },
                enabled = password.isNotBlank() && processingState !is ProcessingState.Processing
            ) {
                Text(stringResource(Res.string.update_passowrd_button))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxSize()
        ) {
            Text(
                text = stringResource(Res.string.update_password),
                style = MaterialTheme.typography.headlineMedium
            )

            principal.displayName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall
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

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                //placeholder = { Text("******") },
                //supportingText = { Text("Optional") },
                label = { Text(stringResource(Res.string.password)) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Crossfade(isPasswordVisible) { visible ->
                            if (visible) Icon(
                                Icons.Outlined.Visibility,
                                contentDescription = stringResource(Res.string.show_hide_password)
                            ) else Icon(Icons.Outlined.VisibilityOff, contentDescription = stringResource(Res.string.show_hide_password))
                        }
                    }
                }
            )

            /*
        Button(
            onClick = {
                onAction(CalendarListAction.OnAddPrincipal(server, username, password, true))
            },
            enabled = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && processingState !is ProcessingState.Processing
        ) {
            Text("Add account (dav4jvm)")
        }
 */

            /*
            Button(
                onClick = {
                    onAction(CalendarListAction.OnUpdatePrincipalPassword(principal, password))
                },
                enabled = password.isNotBlank() && processingState !is ProcessingState.Processing,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Update password")
            }

            AnimatedVisibility(processingState is ProcessingState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            }
            */
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun UpdatePrincipalPasswordBottomSheet_Preview_Idle() {
    UpdatePrincipalPasswordBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        principal = Principal.getPrincipalForPreview(),
        //credentials = Credentials("https://localhost/dav", "my username", "my password"),
        processingState = ProcessingState.Idle,
        onAction = {},
        onDismiss = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun UpdatePrincipalPasswordBottomSheet_Preview_Processing() {
    UpdatePrincipalPasswordBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        principal = Principal.getPrincipalForPreview(),
        //credentials = Credentials("https://localhost/dav", "my username", "my password"),
        processingState = ProcessingState.Processing,
        onAction = {},
        onDismiss = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun UpdatePrincipalPasswordBottomSheet_Preview_Error() {
    UpdatePrincipalPasswordBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        principal = Principal.getPrincipalForPreview(),
        //credentials = Credentials("https://localhost/dav", "my username", "my password"),
        processingState = ProcessingState.Error("This is an error", "Here are the details"),
        onAction = {},
        onDismiss = {}
    )
}
