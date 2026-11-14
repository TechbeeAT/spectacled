package at.techbee.spectacled.theme

import androidx.compose.runtime.Composable

/**
 * Applies [languageOption] to [content] on platforms where the language has to be overridden at
 * the root of the composition (e.g. Android below 13). Where the platform switches the language
 * for the whole process, this simply emits [content].
 */
@Composable
expect fun ProvideAppLanguage(languageOption: LanguageOption, content: @Composable () -> Unit)
