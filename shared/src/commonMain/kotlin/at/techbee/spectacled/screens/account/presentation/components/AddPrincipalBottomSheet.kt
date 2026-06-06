package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.account.presentation.ProcessingState
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_account
import spectacled.shared.generated.resources.password
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.username

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrincipalBottomSheet(
    sheetState: SheetState,
    processingState: ProcessingState,
    onAction: (AccountListAction.OnAddPrincipal) -> Unit,
    onDismiss: () -> Unit,
) {

    var server by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        showLoadingIndicator = processingState is ProcessingState.Processing,
        menuAction = {
            TextButton(
                onClick = {
                    onAction(AccountListAction.OnAddPrincipal(Credentials(server, username, password)))
                },
                enabled = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && processingState !is ProcessingState.Processing
            ) {
                Text(stringResource(Res.string.add_account))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(Res.string.add_account),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Spectacled works with CalDAV-compatible providers.",
                textAlign = TextAlign.Center
            )
            Text(
                text = "You can either sign in with an existing account or create a new account with one of our recommended providers.",
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Option 1",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Use an existing account",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Connect any CalDAV-compatible server using your existing credentials.",
                textAlign = TextAlign.Center
            )

            val error = processingState as? ProcessingState.Error
            AnimatedVisibility(error != null) {
                Text(
                    text = error?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                placeholder = { Text("https://") },
                //supportingText = { Text("Optional") },
                label = { Text("Server (optional)") },
                singleLine = true,
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text(stringResource(Res.string.username)) },
                //supportingText = { Text("Optional") },
                label = { Text(stringResource(Res.string.username)) },
                singleLine = true,
            )

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
                            ) else Icon(
                                Icons.Outlined.VisibilityOff,
                                contentDescription = stringResource(Res.string.show_hide_password)
                            )
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
                    onAction(CalendarListAction.OnAddPrincipal(Credentials(server, username, password)))
                },
                enabled = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && processingState !is ProcessingState.Processing,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Add account (multiplatform)")
            }

            AnimatedVisibility(processingState is ProcessingState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            }

             */

            TextButton(onClick = {
                username = "caldavnotes"
                password = "caldavnotes"
                server = "https://baikal.techbee.at/html/dav.php/calendars/caldavnotes/"
            }) {
                Text("Set sample baikal.techbee.at (caldavnotes/***)")
            }


            TextButton(onClick = {
                username = "caldavnotes"
                password = "caldavnotes"
                server = "https://nextcloud.techbee.at/remote.php/dav"
            }) {
                Text("Set sample nextcloud.techbee.at (caldavnotes/***)")
            }


            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = "Option 2",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Need an account?",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Spectacled is provider-independent.",
                style = MaterialTheme.typography.titleMedium

            )

            Text(
                text = "Choose from a selection of privacy-focused providers that work well with Spectacled.",
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We recommend services based on privacy, reliability, and compatibility. Some links may generate a referral commission, which helps fund ongoing development. Recommendations are never influenced solely by referral agreements.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Recommended providers",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )

            /*
            Text(
                text = "These providers support open standards and have been selected based on privacy, reliability, and compatibility with Spectacled.",
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )

             */

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard() {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Fastmail",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Fast and reliable email, calendar, and contact synchronization with excellent standards support."
                    )
                    TextButton(onClick = { TODO("Not implemented") }) {
                        Text("Learn more")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AddAccountScreen_Preview_Idle() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        AddPrincipalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            processingState = ProcessingState.Idle,
            onAction = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AddAccountScreen_Preview_Processing() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        AddPrincipalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            processingState = ProcessingState.Processing,
            onAction = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AddAccountScreen_Preview_Error() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        AddPrincipalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            processingState = ProcessingState.Error("This is an error"),
            onAction = {},
            onDismiss = {}
        )
    }
}
