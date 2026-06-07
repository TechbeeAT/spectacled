package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.account.presentation.ProcessingState
import at.techbee.spectacled.screens.account.presentation.components.datastructures.CalDavProvider
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_account
import spectacled.shared.generated.resources.password
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.username

enum class AddPrincipalBottomSheetSection { USE_EXISTING, SELECT_FROM_LIST }

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
    val uriHandler = LocalUriHandler.current

    var expandedSection by remember { mutableStateOf<AddPrincipalBottomSheetSection?>(null) }
    var testDropdownMenuExpanded by remember { mutableStateOf(false) }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        showLoadingIndicator = processingState is ProcessingState.Processing,
        menuAction = {
            /*
            TextButton(
                onClick = {
                    onAction(AccountListAction.OnAddPrincipal(Credentials(server, username, password)))
                },
                enabled = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && processingState !is ProcessingState.Processing
            ) {
                Text(stringResource(Res.string.add_account))
            }
             */
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "Spectacled works with CalDAV-compatible providers. " +
                        "You can either sign in with an existing account or create a new account with one of our recommended providers.",
                textAlign = TextAlign.Center
            )

            ElevatedCard(
                onClick = { expandedSection = AddPrincipalBottomSheetSection.USE_EXISTING }
            ) {

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {

                    Text(
                        text = "Option 1",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = expandedSection == AddPrincipalBottomSheetSection.USE_EXISTING,
                            onClick = { expandedSection = AddPrincipalBottomSheetSection.USE_EXISTING }
                        )
                        Text(
                            text = "Use an existing account",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

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

                    AnimatedVisibility(visible = expandedSection == AddPrincipalBottomSheetSection.USE_EXISTING) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = server,
                                onValueChange = { server = it },
                                placeholder = { Text("https://") },
                                //supportingText = { Text("Optional") },
                                label = { Text("Server (optional)") },
                                singleLine = true,
                                trailingIcon = {
                                    TextButton(
                                        onClick = { testDropdownMenuExpanded = !testDropdownMenuExpanded },
                                    ) {
                                        Icon(Icons.Outlined.MoreVert, null)

                                        DropdownMenu(
                                            expanded = testDropdownMenuExpanded,
                                            onDismissRequest = { testDropdownMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Set caldavnotes@baikal") },
                                                onClick = {
                                                    username = "caldavnotes"
                                                    password = "caldavnotes"
                                                    server = "https://baikal.techbee.at/html/dav.php/calendars/caldavnotes/"
                                                    testDropdownMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Set tyler@baikal") },
                                                onClick = {
                                                    username = "tyler"
                                                    password = "tyler"
                                                    server = "https://baikal.techbee.at/html/dav.php/calendars/caldavnotes/"
                                                    testDropdownMenuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Set caldavnotes@nextcloud") },
                                                onClick = {
                                                    username = "caldavnotes"
                                                    password = "caldavnotes"
                                                    server = "https://nextcloud.techbee.at/remote.php/dav"
                                                    testDropdownMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = { Text(stringResource(Res.string.username)) },
                                //supportingText = { Text("Optional") },
                                label = { Text(stringResource(Res.string.username)) },
                                singleLine = true
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

                            TextButton(
                                onClick = {
                                    onAction(AccountListAction.OnAddPrincipal(Credentials(server, username, password)))
                                },
                                enabled = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && processingState !is ProcessingState.Processing
                            ) {
                                Text(stringResource(Res.string.add_account))
                            }
                        }
                    }
                }
            }



            ElevatedCard(
                onClick = { expandedSection = AddPrincipalBottomSheetSection.SELECT_FROM_LIST }
            ) {

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {


                    Text(
                        text = "Option 2",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = expandedSection == AddPrincipalBottomSheetSection.SELECT_FROM_LIST,
                            onClick = { expandedSection = AddPrincipalBottomSheetSection.SELECT_FROM_LIST }
                        )
                        Text(
                            text = "Need an account?",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }


                    Text(
                        text = "Spectacled is provider-independent.",
                        style = MaterialTheme.typography.titleMedium

                    )

                    Text(
                        text = "Choose from a selection of privacy-focused providers that work well with Spectacled.",
                        textAlign = TextAlign.Center
                    )

                    AnimatedVisibility(visible = expandedSection == AddPrincipalBottomSheetSection.SELECT_FROM_LIST) {
                        Column {

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

                            CalDavProvider.entries.forEach { calDavProvider ->
                                AssistChip(
                                    onClick = { uriHandler.openUri(calDavProvider.url) },
                                    label = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                calDavProvider.tags.forEach { tag ->
                                                    Badge { Text(tag) }
                                                }
                                            }
                                            Text(
                                                text = calDavProvider.providerName,
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(calDavProvider.description)
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { uriHandler.openUri(calDavProvider.url) }
                                        ) {
                                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser")
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
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
