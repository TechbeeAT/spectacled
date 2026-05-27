package at.techbee.spectacled.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import at.techbee.spectacled.widget.SpectacledWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val initialCalendarId = intent.getLongExtra(SpectacledWidget.CALENDAR_ID_KEY, -1L).takeIf { it != -1L }

        setContent {
            TasksApp(initialCalendarId = initialCalendarId)
        }
    }
}

@Preview
@Composable
fun NotesAppAndroidPreview() {
    TasksApp()
}