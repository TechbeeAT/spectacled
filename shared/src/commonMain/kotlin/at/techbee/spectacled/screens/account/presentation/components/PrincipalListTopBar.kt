package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about
import spectacled.shared.generated.resources.accounts
import spectacled.shared.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalListTopBar(
    removeHorizontalWindowInsets: Boolean = false,
    onAction: (AccountListAction) -> Unit
) {

    TopAppBar(
        windowInsets = if (removeHorizontalWindowInsets) TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Vertical) else TopAppBarDefaults.windowInsets,
        title = {
            Text(
                text = stringResource(Res.string.accounts),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            TextButton(
                onClick = { onAction(AccountListAction.OnShowAboutBottomSheet()) }
            ) {
                Text(stringResource(Res.string.about))
            }
        },
        actions = {

            TextButton(
                onClick = { onAction(AccountListAction.OnShowSettingsBottomSheet(true)) },
            ) {
                Text(text = stringResource(Res.string.settings))
            }
        }
    )

}


@Preview
@Composable
private fun PrincipalListTopBar_Preview() {
    PrincipalListTopBar(
        onAction = {}
    )
}
