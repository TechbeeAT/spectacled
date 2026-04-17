package at.techbee.spectacled

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
@Preview
fun NotesApp() = SpectacledApp(spectacledVariant = SpectacledVariant.NOTES)
