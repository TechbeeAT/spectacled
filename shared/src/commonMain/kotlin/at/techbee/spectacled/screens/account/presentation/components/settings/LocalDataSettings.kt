package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.LocalDataPersistence
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.local_data_change_reloads
import spectacled.shared.generated.resources.local_data_delete
import spectacled.shared.generated.resources.local_data_delete_info
import spectacled.shared.generated.resources.local_data_keep_info
import spectacled.shared.generated.resources.local_data_keep_title
import spectacled.shared.generated.resources.local_data_private_session_hint
import spectacled.shared.generated.resources.local_data_private_session_info
import spectacled.shared.generated.resources.local_data_title


/**
 * The switch that decides whether this browser keeps the account and the offline copy of the
 * entries, or forgets both when the tab is closed.
 *
 * Shown in the add-account sheet as well as in the settings, so the choice is available at the one
 * moment it actually matters - the first login on a machine that is not yours - without hiding it
 * from anyone who only thinks of it afterwards. The mode is taken as a parameter rather than read
 * from [at.techbee.spectacled.screens.core.data.getLocalDataPolicy] so previews can show both
 * states; callers on the web pass the real one.
 */
@Composable
fun LocalDataPersistenceCard(
    persistence: LocalDataPersistence,
    onPersistenceChanged: (LocalDataPersistence) -> Unit,
    modifier: Modifier = Modifier
) {

    val keepSignedIn = persistence == LocalDataPersistence.PERSISTENT

    OutlinedCard(modifier = modifier.widthIn(min = 350.dp).fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {

            Icon(
                imageVector = if (keepSignedIn) Icons.Outlined.Devices else Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(Res.string.local_data_keep_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(
                        if (keepSignedIn) Res.string.local_data_keep_info
                        else Res.string.local_data_private_session_info
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(Res.string.local_data_change_reloads),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = keepSignedIn,
                onCheckedChange = { checked ->
                    onPersistenceChanged(
                        if (checked) LocalDataPersistence.PERSISTENT else LocalDataPersistence.SESSION_ONLY
                    )
                }
            )
        }
    }
}


/**
 * The settings section around [LocalDataPersistenceCard]: the same switch plus the way out for
 * anyone who only realises afterwards that they signed in on a machine they do not own.
 */
@Composable
fun LocalDataSettings(
    persistence: LocalDataPersistence,
    onPersistenceChanged: (LocalDataPersistence) -> Unit,
    onDeleteLocalData: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(Res.string.local_data_title),
            style = MaterialTheme.typography.titleMedium
        )

        LocalDataPersistenceCard(
            persistence = persistence,
            onPersistenceChanged = onPersistenceChanged
        )

        // Only said here, not on the identical card in the add-account sheet: there the browser
        // holds no account yet, while somebody flipping this switch in the settings would otherwise
        // watch their account list empty itself with no explanation.
        Text(
            text = stringResource(Res.string.local_data_private_session_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
        )

        Text(
            text = stringResource(Res.string.local_data_delete_info),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
        )

        TextButton(
            onClick = onDeleteLocalData,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.DeleteForever, null)
                Text(stringResource(Res.string.local_data_delete))
            }
        }
    }
}


@Preview
@Composable
private fun LocalDataSettings_persistent_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            LocalDataSettings(
                persistence = LocalDataPersistence.PERSISTENT,
                onPersistenceChanged = {},
                onDeleteLocalData = {}
            )
        }
    }
}

@Preview
@Composable
private fun LocalDataSettings_sessionOnly_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            LocalDataSettings(
                persistence = LocalDataPersistence.SESSION_ONLY,
                onPersistenceChanged = {},
                onDeleteLocalData = {}
            )
        }
    }
}
