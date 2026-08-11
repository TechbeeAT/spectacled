package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.ai_create_entries
import spectacled.shared.generated.resources.ai_paste_text_placeholder
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.create
import kotlin.time.Duration.Companion.milliseconds

/**
 * Bottom sheet for the AI "derive entries from text" feature. The user pastes free text; on
 * "Create" the text is sent to the AI and the derived notes/journals/tasks + subtasks are created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeriveEntriesBottomSheet(
    isLoading: Boolean,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        focusRequester.requestFocus()
    }

    BottomSheetWithMenu(
        onDismiss = { if (!isLoading) onDismiss() },
        headline = stringResource(Res.string.ai_create_entries),
        menuActionRight = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {

            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(Res.string.ai_paste_text_placeholder)) },
                enabled = !isLoading,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .focusRequester(focusRequester)
            )

            Button(
                onClick = { if (text.isNotBlank()) onCreate(text) },
                enabled = text.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(Res.string.create))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DeriveEntriesBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            DeriveEntriesBottomSheet(
                isLoading = false,
                onCreate = { },
                onDismiss = { }
            )
        }
    }
}
