package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.MarkdownFormat
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.format_bold
import spectacled.shared.generated.resources.format_italic
import spectacled.shared.generated.resources.format_underline
import spectacled.shared.generated.resources.undo

/**
 * Slim formatting bar (bold / italic / underline) meant to sit just above the software keyboard.
 * The buttons are made non-focusable so tapping them does not steal focus from the editor and hide
 * the keyboard.
 */
@Composable
fun FormattingBar(
    onFormat: (MarkdownFormat) -> Unit,
    onReleaseFocus: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth()
        ) {
            IconButton(
                onClick = { onFormat(MarkdownFormat.BOLD) },
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = stringResource(Res.string.format_bold))
            }
            IconButton(
                onClick = { onFormat(MarkdownFormat.ITALIC) },
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = stringResource(Res.string.format_italic))
            }
            IconButton(
                onClick = { onFormat(MarkdownFormat.UNDERLINE) },
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = stringResource(Res.string.format_underline))
            }

            VerticalDivider(modifier = Modifier.padding(vertical = 4.dp).height(24.dp))

            IconButton(
                onClick = { onUndo() },
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = stringResource(Res.string.undo))
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { onReleaseFocus() },
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown, null)
            }
        }
    }
}

@Preview
@Composable
private fun FormattingBar_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            FormattingBar(
                onFormat = {},
                onReleaseFocus = {},
                onUndo = {}
            )
        }
    }
}
