package at.techbee.spectacled.screens.core

import android.content.Context
import android.content.Intent

/**
 * The Android-specific implementation of ShareManager.
 * It uses an Intent to trigger the system's share sheet.
 */
actual class PlatformShareManager(private val context: Context): ShareManager {
    actual override fun share(content: ShareContent) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, content.subject)
            putExtra(Intent.EXTRA_TEXT, content.body)
        }
        val chooser = Intent.createChooser(intent, content.subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
