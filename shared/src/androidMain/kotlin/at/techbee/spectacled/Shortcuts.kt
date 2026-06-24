package at.techbee.spectacled

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import at.techbee.spectacled.shared.R
import at.techbee.spectacled.widget.SpectacledWidget.Companion.CALENDAR_ID_KEY
import at.techbee.spectacled.widget.SpectacledWidget.Companion.ICAL_ENTRY_ID_KEY

fun setupShortcuts(context: Context) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    val shortcut = ShortcutInfo.Builder(context, "new_entry")
        .setShortLabel("New entry")
        .setIcon(Icon.createWithResource(context, R.drawable.ic_add))
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