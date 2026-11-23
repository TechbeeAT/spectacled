package at.techbee.spectacled.theme

import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.language_native_english
import spectacled.shared.generated.resources.language_native_german
import spectacled.shared.generated.resources.language_system_default

/**
 * The app language chosen by the user.
 *
 * [tag] is the BCP 47 language tag handed to the platform (null = follow the system).
 * [nativeName] is a string resource marked translatable="false": each language is shown in its own
 * language, so users who end up in a language they cannot read can still find their way back.
 *
 * When adding a language, also add it to shared/src/androidMain/res/xml/locales_config.xml and to
 * CFBundleLocalizations in the three iosApp/ios<Variant>App/ios<Variant>App/Info.plist files.
 */
enum class LanguageOption(
    val tag: String?,
    val nativeName: StringResource
) {
    SYSTEM(null, Res.string.language_system_default),
    ENGLISH("en", Res.string.language_native_english),
    GERMAN("de", Res.string.language_native_german);

    companion object {
        fun fromString(value: String?) = entries.find { it.name == value } ?: SYSTEM
    }
}
