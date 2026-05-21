package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onAction: (AccountListAction) -> Unit
) {

    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.accounts),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
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
