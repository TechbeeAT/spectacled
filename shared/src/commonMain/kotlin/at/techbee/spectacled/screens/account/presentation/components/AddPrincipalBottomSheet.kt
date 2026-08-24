package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.account.presentation.ProcessingState
import at.techbee.spectacled.screens.account.presentation.components.datastructures.CalDavProvider
import at.techbee.spectacled.screens.account.presentation.components.datastructures.CalDavProviderCategory
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_account
import spectacled.shared.generated.resources.add_account_header_info
import spectacled.shared.generated.resources.add_account_option1_headline
import spectacled.shared.generated.resources.add_account_option1_text
import spectacled.shared.generated.resources.add_account_option2_headline
import spectacled.shared.generated.resources.add_account_option2_recommendation_info
import spectacled.shared.generated.resources.add_account_option2_recommended_providers
import spectacled.shared.generated.resources.add_account_option2_text
import spectacled.shared.generated.resources.add_account_option_x
import spectacled.shared.generated.resources.add_account_provider_tasks_only_warning
import spectacled.shared.generated.resources.add_account_spectacled_is_provider_independent
import spectacled.shared.generated.resources.back
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.insecure_connection_warning
import spectacled.shared.generated.resources.open_in_browser
import spectacled.shared.generated.resources.password
import spectacled.shared.generated.resources.server_inferred
import spectacled.shared.generated.resources.server_optional
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.username
import spectacled.shared.generated.resources.welcome

enum class AddPrincipalBottomSheetPage { SELECTION, USE_EXISTING, SELECT_FROM_LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrincipalBottomSheet(
    sheetState: SheetState,
    processingState: ProcessingState,
    isFirstAccount: Boolean,
    onAction: (AccountListAction.OnAddPrincipal) -> Unit,
    onDismiss: () -> Unit,
) {

    var selectedPage by rememberSaveable { mutableStateOf(AddPrincipalBottomSheetPage.SELECTION) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showInsecureConnectionAlert by rememberSaveable { mutableStateOf(false) }
    var credentials by rememberSaveable { mutableStateOf<Credentials?>(null) }

    LaunchedEffect(selectedPage) {
        if (selectedPage == AddPrincipalBottomSheetPage.SELECTION)
            scope.launch { pagerState.animateScrollToPage(0) }
        else
            scope.launch { pagerState.animateScrollToPage(1) }
    }

    if(showInsecureConnectionAlert) {
        InsecureConnectionWarningDialog(
            server = credentials?.server?.toString()?:"",
            onDismiss = { showInsecureConnectionAlert = false },
            onConfirm = {
                credentials?.let { onAction(AccountListAction.OnAddPrincipal(it)) }
                showInsecureConnectionAlert = false
            }
        )
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

                AnimatedVisibility(pagerState.currentPage == 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(stringResource(Res.string.cancel))
                        }
                    }

                AnimatedVisibility(pagerState.currentPage != 0){
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronLeft,
                                contentDescription = stringResource(Res.string.back)
                            )
                            Text(text = stringResource(Res.string.back))
                        }

                    }

            }
        },
        menuActionRight = {
            AnimatedVisibility(selectedPage == AddPrincipalBottomSheetPage.USE_EXISTING) {
                TextButton(
                    onClick = {
                        if(credentials?.server?.toString()?.startsWith("http://") == true)
                            showInsecureConnectionAlert = true
                        else
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
                    isFirstAccount = isFirstAccount,
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
    isFirstAccount: Boolean,
    onPageChanged: (AddPrincipalBottomSheetPage) -> Unit,
    modifier: Modifier = Modifier.padding(8.dp).fillMaxSize().verticalScroll(rememberScrollState())
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(if(isFirstAccount) Res.string.welcome else Res.string.add_account),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = stringResource(Res.string.add_account_header_info),
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
                        text = stringResource(Res.string.add_account_option_x, 1),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(Res.string.add_account_option1_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = stringResource(Res.string.add_account_option1_text),
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
                        text = stringResource(Res.string.add_account_option_x, 2),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(Res.string.add_account_option2_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = stringResource(Res.string.add_account_option2_text),
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
            text = stringResource(Res.string.add_account_spectacled_is_provider_independent),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = stringResource(Res.string.add_account_option1_text),
            textAlign = TextAlign.Center
        )

        val error = processingState as? ProcessingState.Error
        AnimatedVisibility(error != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                error?.detail?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }

            }

        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                placeholder = { Text("https://") },
                supportingText = {
                    val trimmedServer = server.trim()
                    val isInsecure = trimmedServer.startsWith("http://")
                    val inferred = if (server.isBlank() && username.contains("@")) {
                        val domain = username.substringAfter("@").trim()
                        if (domain.isNotEmpty()) "https://$domain" else null
                    } else null

                    Column {
                        AnimatedVisibility(inferred?.isNotBlank() == true) {
                            Text(stringResource(Res.string.server_inferred, inferred.orEmpty()))
                        }
                        AnimatedVisibility(isInsecure) {
                            Text(
                                text = stringResource(Res.string.insecure_connection_warning),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                label = { Text(stringResource(Res.string.server_optional)) },
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
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false
                    //imeAction = ImeAction.Done
                ),
                modifier = Modifier.width(400.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text(stringResource(Res.string.username)) },
                label = { Text(stringResource(Res.string.username)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false
                    //imeAction = ImeAction.Done
                ),
                modifier = Modifier.width(400.dp)
            )

            OutlinedSecureTextField(
                state = passwordState,
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
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                    //imeAction = ImeAction.Done
                ),
                modifier = Modifier.width(400.dp)
            )
        }
    }
}


@Composable
fun ChooseProviderScreen(
    modifier: Modifier = Modifier,
    spectacledVariant: SpectacledVariant = koinInject()
) {

    val requiredComponent = spectacledVariant.mainCalendarComponent
    val providers = CalDavProvider.entries.filter {
        it.supportedCalendarComponents.contains(requiredComponent)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(Res.string.add_account_spectacled_is_provider_independent),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = stringResource(Res.string.add_account_option2_text),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.add_account_option2_recommendation_info),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(Res.string.add_account_option2_recommended_providers),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        Column {

            CalDavProviderCategory.entries.forEach { category ->

                val providersInCategory = providers.filter { it.category == category }
                if (providersInCategory.isEmpty()) return@forEach

                Text(
                    text = stringResource(category.headline),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)
                )

                providersInCategory.forEach { calDavProvider ->
                    CalDavProviderChip(calDavProvider = calDavProvider)
                }
            }
        }

    }
}

@Composable
private fun CalDavProviderChip(
    calDavProvider: CalDavProvider,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    AssistChip(
        onClick = { uriHandler.openUri(calDavProvider.url) },
        label = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    calDavProvider.tags.forEach { tag ->
                        Badge { Text(stringResource(tag)) }
                    }
                }
                Text(
                    text = calDavProvider.providerName,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(stringResource(calDavProvider.description))

                if (calDavProvider.hasTodo && !calDavProvider.hasJournal) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(Res.string.add_account_provider_tasks_only_warning),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { uriHandler.openUri(calDavProvider.url) }
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.open_in_browser))
            }
        },
        modifier = modifier.padding(vertical = 2.dp)
    )
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
                isFirstAccount = true,
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
                isFirstAccount = false,
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
            ChooseProviderScreen(spectacledVariant = SpectacledVariant.TASKS)
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

@Preview
@Composable
private fun CalDavProviderChip_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            CalDavProviderChip(CalDavProvider.FASTMAIL)
        }

    }
}