package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    sheetState: SheetState,
    userAppPreferencesStore: UserAppPreferencesStore,
    onAction: (AccountListAction) -> Unit,
    onDismiss: () -> Unit,
) {

    var themeDropdownExpanded by remember { mutableStateOf(false) }
    val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(ThemeOption.SYSTEM.name)


    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        menuAction = { }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxSize()
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            AssistChip(
                onClick = { themeDropdownExpanded = true },
                label = {

                    Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(Res.string.theme),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(stringResource(ThemeOption.fromString(themeOption).stringRes))
                    }

                    DropdownMenu(
                        expanded = themeDropdownExpanded,
                        onDismissRequest = { themeDropdownExpanded = false },
                    ) {

                        ThemeOption.entries.forEach { themeOption ->
                            DropdownMenuItem(
                                text = { Text(stringResource(themeOption.stringRes)) },
                                onClick = {
                                    userAppPreferencesStore.themeOption = themeOption
                                    themeDropdownExpanded = false
                                },
                            )
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.Language, null) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                modifier = Modifier.widthIn(min = 350.dp)
            )

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SettingsBottomSheet_Preview() {
    SettingsBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        userAppPreferencesStore = object: UserAppPreferencesStore {
            override fun save(key: String, value: String) { }
            override fun load(key: String): String? { return null }
            override fun loadAsFlow(key: String): Flow<String?> { return flowOf(null ) }
            override fun remove(key: String) { }
        },
        onAction = {},
        onDismiss = {}
    )
}
