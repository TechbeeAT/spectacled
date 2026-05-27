package at.techbee.spectacled.journals

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledApp
import at.techbee.spectacled.SpectacledVariant
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
@Preview
fun JournalsApp(initialCalendarId: Long? = null) = SpectacledApp(
    spectacledVariant = SpectacledVariant.JOURNALS,
    initialCalendarId = initialCalendarId
)
