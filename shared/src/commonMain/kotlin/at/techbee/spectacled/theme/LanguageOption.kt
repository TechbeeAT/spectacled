package at.techbee.spectacled.theme

/**
 * The app language chosen by the user.
 *
 * [tag] is the BCP 47 language tag handed to the platform (null = follow the system).
 * [nativeName] is deliberately not a translatable resource: each language is shown in its own
 * language, so users who end up in a language they cannot read can still find their way back.
 *
 * When adding a language, also add it to shared/src/androidMain/res/xml/locales_config.xml.
 */
enum class LanguageOption(
    val tag: String?,
    val nativeName: String?
) {
    SYSTEM(null, null),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch");

    companion object {
        fun fromString(value: String?) = entries.find { it.name == value } ?: SYSTEM
    }
}
