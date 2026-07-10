package at.techbee.spectacled

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import at.techbee.spectacled.shared.R
import at.techbee.spectacled.widget.SpectacledWidget.Companion.CALENDAR_ID_KEY
import at.techbee.spectacled.widget.SpectacledWidget.Companion.ICAL_ENTRY_ID_KEY
import org.jetbrains.compose.resources.getString
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_journal
import spectacled.shared.generated.resources.add_note
import spectacled.shared.generated.resources.add_task

suspend fun setupShortcuts(context: Context, spectacledVariant: SpectacledVariant) {

    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    val label = getString(when(spectacledVariant) {
        SpectacledVariant.JOURNALS -> Res.string.add_journal
        SpectacledVariant.NOTES -> Res.string.add_note
        SpectacledVariant.TASKS -> Res.string.add_task
    })

    val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_add)
    val icon = iconDrawable?.let {
        Icon.createWithBitmap(it.toBitmap())
    } ?: Icon.createWithResource(context, R.drawable.ic_add)

    val shortcut = ShortcutInfo.Builder(context, "new_entry")
        .setShortLabel(label)
        .setIcon(icon)
        .setIntent(
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                action = Intent.ACTION_VIEW
                putExtra(CALENDAR_ID_KEY, 0L)
                putExtra(ICAL_ENTRY_ID_KEY, 0L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            } ?: Intent()
        )
        .build()
    shortcutManager.dynamicShortcuts = listOf(shortcut)
}
