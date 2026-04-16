package at.techbee.spectacled.screens.core.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.Route
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.journals
import spectacled.shared.generated.resources.notes
import org.jetbrains.compose.resources.stringResource


@Composable
fun MainNavigationDrawer(
    drawerState: DrawerState,
    paddingValues: PaddingValues,
    currentDestination: Route,
    onNavigate: (Route) -> Unit,
    mainContent: @Composable () -> Unit,
) {

    /*
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    if(showAboutDialog) {
        AboutDialog(
            onDismissRequest = { showAboutDialog = false }
        )
    }

     */

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {

            ModalDrawerSheet(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "CalDAV Notes",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.padding(top = 16.dp))


                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.NoteAlt, stringResource(Res.string.notes)) },
                    label = { Text(stringResource(Res.string.notes)) },
                    selected = currentDestination is Route.NoteList,
                    onClick = {
                        onNavigate(Route.NoteList(0L))
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.DateRange, stringResource(Res.string.journals)) },
                    label = { Text(stringResource(Res.string.journals)) },
                    selected = false,
                    onClick = {
                        TODO()
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                /*
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Android, "Testing only") },
                    label = { Text("Only for testing") },
                    selected = currentDestination == Route.OnlyForTesting,
                    onClick = {
                        onNavigate(Route.OnlyForTesting)
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                 */

                HorizontalDivider(modifier = Modifier.padding(16.dp))
                Text(
                    text = "Collections",
                    modifier = Modifier.padding(start = 24.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Folder, "Collection1") },
                    label = { Text("Collection 1") },
                    selected = false,
                    onClick = {
                        TODO()
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Folder, "Collection1") },
                    label = { Text("Collection 2") },
                    selected = false,
                    onClick = {
                        TODO()
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )


                HorizontalDivider(modifier = Modifier.padding(16.dp))
                Text(
                    text = "Categories",
                    modifier = Modifier.padding(start = 24.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Outlined.Label, "Category 1") },
                    label = { Text("Category 1") },
                    selected = false,
                    onClick = {
                        TODO()
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Outlined.Label, "Category2") },
                    label = { Text("Category 2") },
                    selected = false,
                    onClick = {
                        TODO()
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )



                HorizontalDivider(modifier = Modifier.padding(16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        TODO()
                    },
                    //modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Outlined.Help, "Info / About") },
                    label = { Text("Info / About") },
                    selected = false,
                    onClick = {
                        //showAboutDialog = true
                    },
                )
            }
        },
        content = {
            Box(
                modifier = Modifier.padding(8.dp).fillMaxSize()
            ) {
                mainContent()
            }

        },
        modifier = Modifier.padding(paddingValues)
    )
}



@Preview
@Composable
fun JtxNavigationDrawerMenu_Preview() {
    MaterialTheme {
        MainNavigationDrawer(
            drawerState = DrawerState(DrawerValue.Open),
            currentDestination = Route.NoteList(0L),
            onNavigate = {},
            paddingValues = PaddingValues()
        ) {}
    }
}

