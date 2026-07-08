package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
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
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_account
import spectacled.shared.generated.resources.back
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.password
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.username

enum class AddPrincipalBottomSheetPage { SELECTION, USE_EXISTING, SELECT_FROM_LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrincipalBottomSheet(
    sheetState: SheetState,
    processingState: ProcessingState,
    onAction: (AccountListAction.OnAddPrincipal) -> Unit,
    onDismiss: () -> Unit,
) {

    var selectedPage by rememberSaveable { mutableStateOf(AddPrincipalBottomSheetPage.SELECTION) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var credentials by rememberSaveable { mutableStateOf<Credentials?>(null) }

    LaunchedEffect(selectedPage) {
        if (selectedPage == AddPrincipalBottomSheetPage.SELECTION)
            scope.launch { pagerState.animateScrollToPage(0) }
        else
            scope.launch { pagerState.animateScrollToPage(1) }
    }

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        showLoadingIndicator = processingState is ProcessingState.Processing,
        gesturesEnabled = false,
        menuActionLeft = {
            TextButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        onDismiss()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(0) }
                        selectedPage = AddPrincipalBottomSheetPage.SELECTION
                    }
                },
            ) {
                Crossfade(pagerState.currentPage == 0) { isFirst ->
                    if (isFirst)
                        Text(stringResource(Res.string.cancel))
                    else
                        Text(stringResource(Res.string.back))
                }

            }
        },
        menuActionRight = {
            AnimatedVisibility(selectedPage == AddPrincipalBottomSheetPage.USE_EXISTING) {
                TextButton(
                    onClick = {
                        credentials?.let { onAction(AccountListAction.OnAddPrincipal(it)) }
                    },
                    enabled = credentials != null && processingState !is ProcessingState.Processing
                ) {
                    Text(stringResource(Res.string.add_account))
                }
            }
        }
    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (pagerState.currentPage == 0) {
                SelectAccountOptionScreen(
                    onPageChanged = { selectedPage = it },
                    modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
                )
            } else {
                if (selectedPage == AddPrincipalBottomSheetPage.USE_EXISTING) {
                    AddAccountScreen(
                        processingState = processingState,
                        //onAction = onAction,
                        onCredentialsUpdated = { credentials = it },
                        modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
                    )
                } else if (selectedPage == AddPrincipalBottomSheetPage.SELECT_FROM_LIST) {    // SELECT FROM LIST
                    ChooseProviderScreen(
                        modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}


@Composable
fun SelectAccountOptionScreen(
    onPageChanged: (AddPrincipalBottomSheetPage) -> Unit,
    modifier: Modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = "Spectacled works with CalDAV-compatible providers. " +
                    "You can either sign in with an existing account or create a new account with one of our recommended providers.",
            textAlign = TextAlign.Center
        )

        ElevatedCard(
            onClick = { onPageChanged(AddPrincipalBottomSheetPage.USE_EXISTING) }
        ) {

            Box(
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 36.dp)
                ) {


                    Text(
                        text = "Option 1",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Have an account?",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Connect any CalDAV-compatible server using your existing credentials.",
                        textAlign = TextAlign.Center
                    )
                }

                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.padding(8.dp))
            }
        }


        ElevatedCard(
            onClick = { onPageChanged(AddPrincipalBottomSheetPage.SELECT_FROM_LIST) }
        ) {

            Box(
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 36.dp)
                ) {


                    Text(
                        text = "Option 2",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Need an account?",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Choose from a selection of privacy-focused providers that work well with Spectacled.",
                        textAlign = TextAlign.Center
                    )
                }

                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun AddAccountScreen(
    processingState: ProcessingState,
    //onAction: (AccountListAction.OnAddPrincipal) -> Unit,
    onCredentialsUpdated: (Credentials?) -> Unit,
    modifier: Modifier = Modifier
) {

    var server by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    val passwordState = rememberTextFieldState()
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var testDropdownMenuExpanded by remember { mutableStateOf(false) }

    val credentials by remember {
        derivedStateOf {
            val trimmedServer = server.trim()
            val trimmedUsername = username.trim()
            val effectiveServer = when {
                trimmedServer.isNotBlank() -> trimmedServer
                trimmedUsername.contains("@") -> trimmedUsername.substringAfter("@")
                else -> null
            }

            if (!effectiveServer.isNullOrBlank() && trimmedUsername.isNotBlank() && passwordState.text.isNotBlank()) {
                val urlString = if (!effectiveServer.startsWith("http://") && !effectiveServer.startsWith("https://")) {
                    "https://$effectiveServer"
                } else {
                    effectiveServer
                }

                try {
                    Credentials(Url(urlString), trimmedUsername, passwordState.text.toString())
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    LaunchedEffect(credentials) {
        onCredentialsUpdated(credentials)
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = "Spectacled is provider-independent",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                placeholder = { Text("https://") },
                supportingText = {
                    val inferred = if (server.isBlank() && username.contains("@")) {
                        val domain = username.substringAfter("@").trim()
                        if (domain.isNotEmpty()) "https://$domain" else null
                    } else null
                    Text(inferred?.let { "Inferred: $it" } ?: "Optional")
                },
                label = { Text("Server (optional)") },
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
                                    passwordState.setTextAndPlaceCursorAtEnd("caldavnotes")
                                    server = "https://baikal.techbee.at/html/dav.php/calendars/caldavnotes/"
                                    testDropdownMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set tyler@baikal") },
                                onClick = {
                                    username = "tyler"
                                    passwordState.setTextAndPlaceCursorAtEnd("tyler")
                                    server = "https://baikal.techbee.at/html/dav.php/calendars/"
                                    testDropdownMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set caldavnotes@nextcloud") },
                                onClick = {
                                    username = "caldavnotes"
                                    passwordState.setTextAndPlaceCursorAtEnd("caldavnotes")
                                    server = "https://nextcloud.techbee.at/remote.php/dav"
                                    testDropdownMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.width(400.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text(stringResource(Res.string.username)) },
                //supportingText = { Text("Optional") },
                label = { Text(stringResource(Res.string.username)) },
                singleLine = true,
                modifier = Modifier.width(400.dp)
            )

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
                            ) else Icon(
                                Icons.Outlined.VisibilityOff,
                                contentDescription = stringResource(Res.string.show_hide_password)
                            )
                        }
                    }
                },
                modifier = Modifier.width(400.dp)
            )
            /*
                            TextButton(
                                onClick = {
                                    onAction(AccountListAction.OnAddPrincipal(Credentials(serverUrl, username, passwordState.text.toString())))
                                },
                                enabled = server.isNotBlank() && username.isNotBlank() && passwordState.text.isNotEmpty() && processingState !is ProcessingState.Processing
                            ) {
                                Text(stringResource(Res.string.add_account))
                            }

             */
        }
    }
}


@Composable
fun ChooseProviderScreen(modifier: Modifier = Modifier) {

    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = "Spectacled is provider-independent",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
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


        Spacer(modifier = Modifier.height(16.dp))

        Column {


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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AddAccountScreen_Preview_Idle() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            AddPrincipalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
private fun AddAccountScreen_Preview_Processing() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        Scaffold {
            AddPrincipalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
private fun ChooseProviderScreen_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            ChooseProviderScreen()
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AddAccountScreen_Preview_Error() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AddAccountScreen(
                processingState = ProcessingState.Error("This is an error"),
                onCredentialsUpdated = {},
                //onAction = {}
            )
        }

    }
}
