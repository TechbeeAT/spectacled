package at.techbee.spectacled.screens.account.presentation.calendars.components

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
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListAction
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about
import spectacled.shared.generated.resources.accounts
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.edit
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalListTopBar(
    onAction: (CalendarListAction) -> Unit,
    state: CalendarListState
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
                onClick = { onAction(CalendarListAction.OnShowAboutBottomSheet()) }
            ) {
                Text(stringResource(Res.string.about))
            }
        },
        actions = {

            TextButton(
                onClick = { onAction(CalendarListAction.OnToggleEditMode) },
            ) {
                Text(
                    text = if (state.isEditMode)
                        stringResource(Res.string.done)
                    else
                        stringResource(Res.string.edit),
                )
            }
        }
    )

}


@Preview
@Composable
private fun PrincipalListTopBar_edit_off_Preview() {
    PrincipalListTopBar(
        onAction = {},
        state = CalendarListState(isEditMode = false)
    )
}


@Preview
@Composable
private fun PrincipalListTopBar_edit_on_Preview() {
    PrincipalListTopBar(
        onAction = {},
        state = CalendarListState(isEditMode =  true)
    )
}

