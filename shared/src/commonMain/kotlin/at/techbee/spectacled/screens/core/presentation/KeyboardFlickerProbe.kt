package at.techbee.spectacled.screens.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import kotlin.time.TimeSource

/*
 * Throwaway isolation harness for "the keyboard leaves and re-enters the screen when moving focus
 * from one text field to another". NOT FOR RELEASE - delete this file once the layer responsible is
 * known.
 *
 * The problem shows up on the details editors and in AddPrincipalBottomSheet, so it is not specific
 * to any screen. This strips everything away: two plain text fields with identical keyboard options,
 * no navigation, no view model, no Scaffold, no theme, no IME padding. Tap one field, then the
 * other, and watch whether the keyboard stays up. The readout shows the live IME inset with
 * timestamps, so a flap is visible even when the animation is too quick to follow.
 *
 * HOW TO RUN IT
 *
 * In any of the MainActivity files, swap the app composable for the probe:
 *
 *     setContent { KeyboardFlickerProbe() }        // instead of TasksApp(onCloseApp = { finish() })
 *
 * HOW TO READ IT
 *
 * With all three flags below false, nothing of this app's UI is in play - only the Activity setup
 * (enableEdgeToEdge, the manifest) and Compose Multiplatform itself.
 *
 *   - Keyboard already flickers => no Compose code in this app is responsible. The next step is an
 *     upstream report against Compose Multiplatform, or trying a different CMP version, rather than
 *     any change in these screens.
 *   - Keyboard stays up => our UI adds the trigger. Turn the flags on ONE AT A TIME, in the order
 *     listed. The first one that makes it flicker is the layer at fault.
 */

/** Wrap the fields in the app's real theme. */
const val PROBE_WITH_THEME = false

/** Wrap the fields in a Material3 Scaffold, as every real screen does. */
const val PROBE_WITH_SCAFFOLD = false

/** Apply [imeAwarePadding], as DetailsScreenRoot does. */
const val PROBE_WITH_IME_PADDING = false

/** Put the fields in a vertical scroll container, as the details screen does. */
const val PROBE_WITH_SCROLL = false

@Composable
fun KeyboardFlickerProbe() {
    if (PROBE_WITH_THEME) {
        AppTheme(spectacledVariant = SpectacledVariant.NOTES) { ProbeScaffoldLayer() }
    } else {
        ProbeScaffoldLayer()
    }
}

@Composable
private fun ProbeScaffoldLayer() {
    if (PROBE_WITH_SCAFFOLD) {
        Scaffold { innerPadding -> ProbeFields(Modifier.padding(innerPadding)) }
    } else {
        ProbeFields()
    }
}

@Composable
private fun ProbeFields(modifier: Modifier = Modifier) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }

    // Deliberately identical for both fields, and built once: a genuine input-type change is the one
    // legitimate reason Android has to restart the IME, so it must not be a variable here.
    val options = remember {
        KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.LongMessage
        )
    }

    var content: Modifier = modifier.fillMaxSize()
    if (PROBE_WITH_IME_PADDING) content = content.imeAwarePadding()
    if (PROBE_WITH_SCROLL) content = content.verticalScroll(rememberScrollState())

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = content.padding(16.dp)
    ) {
        ImeReadout()
        TextField(
            value = first,
            onValueChange = { first = it },
            label = { Text("first") },
            keyboardOptions = options,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = second,
            onValueChange = { second = it },
            label = { Text("second") },
            keyboardOptions = options,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Tap 'first', then 'second'. Does the keyboard stay up?",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ImeReadout() {
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val started = remember { TimeSource.Monotonic.markNow() }
    val log = remember { mutableStateListOf<String>() }

    LaunchedEffect(imeBottom) {
        log.add(0, "+${started.elapsedNow().inWholeMilliseconds}ms ime -> ${imeBottom}px")
        while (log.size > 6) log.removeAt(log.lastIndex)
    }

    Column {
        Text("ime ${imeBottom}px", style = MaterialTheme.typography.labelMedium)
        log.forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
    }
}
