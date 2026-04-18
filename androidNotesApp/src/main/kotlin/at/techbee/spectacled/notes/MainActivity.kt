package at.techbee.spectacled.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.koin.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        startKoin {
            // Log Koin logs to the Android logger
            androidLogger()
            // Provide the Android context to Koin
            androidContext(this@MainActivity)
            // Declare modules for Android. Attention: for other platforms this is done in App.kt!
            modules(
                module { single { SpectacledVariant.NOTES } },
                sharedModule
            )
        }

        setContent {
            NotesApp()
        }
    }
}

@Preview
@Composable
fun NotesAppAndroidPreview() {
    NotesApp()
}