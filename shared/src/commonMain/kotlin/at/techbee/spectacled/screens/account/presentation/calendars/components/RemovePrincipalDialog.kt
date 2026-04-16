package at.techbee.spectacled.screens.account.presentation.calendars.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.core.domain.Principal
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.remove_account_warning
import spectacled.shared.generated.resources.remove_account_x
import spectacled.shared.generated.resources.remove
import org.jetbrains.compose.resources.stringResource

@Composable
fun RemovePrincipalDialog(
    principal: Principal,
    onConfirm: (Principal) -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(principal) }
            ) {
                Text(stringResource(Res.string.remove))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = { Icon(Icons.Outlined.NoAccounts, null) },
        title = {
            Text(stringResource(Res.string.remove_account_x, principal.displayName?:principal.principalUrl))
        },
        text = {
            Text(stringResource(Res.string.remove_account_warning))

        }
    )
}

@Preview
@Composable
private fun RemoveAccountDialog_Preview() {
    RemovePrincipalDialog(
        principal = Principal.getPrincipalForPreview(),
        onConfirm = {},
        onDismiss = {}
    )
}
