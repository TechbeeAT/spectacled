package at.techbee.spectacled.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.setupShortcuts
import at.techbee.spectacled.widget.SpectacledWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setupShortcuts(this, SpectacledVariant.NOTES)

        val initialCalendarId = intent.getLongExtra(SpectacledWidget.CALENDAR_ID_KEY, -1L).takeIf { it != -1L }
        val initialIcalEntryId = intent.getLongExtra(SpectacledWidget.ICAL_ENTRY_ID_KEY, -1L).takeIf { it != -1L }

        setContent {
            NotesApp(
                initialCalendarId = initialCalendarId,
                initialIcalEntryId = initialIcalEntryId,
                onCloseApp = { finish() }
            )
        }
    }
}

@Preview
@Composable
fun NotesAppAndroidPreview() {
    NotesApp()
}