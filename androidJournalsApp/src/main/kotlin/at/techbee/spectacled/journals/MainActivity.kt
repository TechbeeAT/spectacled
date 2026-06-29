package at.techbee.spectacled.journals

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import at.techbee.spectacled.DeepLinkData
import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.setupShortcuts
import at.techbee.spectacled.widget.SpectacledWidget

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setupShortcuts(this, SpectacledVariant.JOURNALS)
        processIntent(intent)

        setContent {
            JournalsApp(
                onCloseApp = { finish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        val calendarId = intent.getLongExtra(SpectacledWidget.CALENDAR_ID_KEY, -1L).takeIf { it != -1L }
        var icalEntryId = intent.getLongExtra(SpectacledWidget.ICAL_ENTRY_ID_KEY, -1L).takeIf { it != -1L }

        val description = if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else if (intent.action == Intent.ACTION_VIEW && intent.data?.host == DeepLinkData.DEEPLINK_ADD_HOST) {
            intent.data?.getQueryParameter(DeepLinkData.DEEPLINK_DESCRIPTION_PARAM)
        } else null

        if (description != null && icalEntryId == null) {
            icalEntryId = 0L
        }

        if (calendarId != null || icalEntryId != null || description != null) {
            DeepLinkHandler.onDeepLinkReceived(calendarId, icalEntryId, description)
        }
    }
}
