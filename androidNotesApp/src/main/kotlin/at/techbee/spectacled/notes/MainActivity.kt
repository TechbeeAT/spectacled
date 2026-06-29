package at.techbee.spectacled.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.setupShortcuts
import at.techbee.spectacled.widget.SpectacledWidget

class MainActivity : ComponentActivity() {

    private var initialCalendarId by mutableStateOf<Long?>(null)
    private var initialIcalEntryId by mutableStateOf<Long?>(null)
    private var initialIcalEntryDescription by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setupShortcuts(this, SpectacledVariant.NOTES)
        processIntent(intent)

        setContent {
            NotesApp(
                initialCalendarId = initialCalendarId,
                initialIcalEntryId = initialIcalEntryId,
                initialIcalEntryDescription = initialIcalEntryDescription,
                onCloseApp = { finish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        initialCalendarId = intent.getLongExtra(SpectacledWidget.CALENDAR_ID_KEY, -1L).takeIf { it != -1L }
        initialIcalEntryId = intent.getLongExtra(SpectacledWidget.ICAL_ENTRY_ID_KEY, -1L).takeIf { it != -1L }

        initialIcalEntryDescription = if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else if (intent.action == Intent.ACTION_VIEW && intent.data?.host == "add") {
            intent.data?.getQueryParameter("description")
        } else null

        if (initialIcalEntryDescription != null && initialIcalEntryId == null) {
            initialIcalEntryId = 0L
        }
    }
}
