package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    sheetState: SheetState,
    userAppPreferencesStore: UserAppPreferencesStore,
    onDismiss: () -> Unit,
) {

    val screens = mutableListOf(
        SettingsTabDestination.Appearance,
        SettingsTabDestination.AiProvider
    ).apply {
        if (getPlatform().platform == Platforms.WASM || LocalInspectionMode.current)
            add(SettingsTabDestination.More)
    }.toList()

    val pagerState = rememberPagerState(initialPage = screens.indexOf(SettingsTabDestination.Appearance), pageCount = { screens.size })
    val scope = rememberCoroutineScope()


    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        gesturesEnabled = false,
        menuActionLeft = { },
        menuActionRight = {
            TextButton(
                onClick = { onDismiss() },
            ) {
                Text(stringResource(Res.string.close))
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                screens.forEachIndexed { index, screen ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    screen.icon,
                                    stringResource(screen.titleResource)
                                )
                                Text(
                                    text = stringResource(screen.titleResource),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                        })
                }
            }

            val defaultPageModifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)


            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f).padding(8.dp)
            ) { page ->
                when (page) {
                    screens.indexOf(SettingsTabDestination.Appearance) -> SettingsAppearancePage(userAppPreferencesStore, defaultPageModifier)
                    screens.indexOf(SettingsTabDestination.AiProvider) -> SettingsAiPage(userAppPreferencesStore, defaultPageModifier)
                    screens.indexOf(SettingsTabDestination.More) -> SettingsMorePage(userAppPreferencesStore, defaultPageModifier)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SettingsBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            SettingsBottomSheet(
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
                userAppPreferencesStore = UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS),
                onDismiss = {}
            )
        }
    }
}
