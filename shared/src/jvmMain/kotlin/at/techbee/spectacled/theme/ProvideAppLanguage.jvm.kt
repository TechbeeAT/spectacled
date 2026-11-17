package at.techbee.spectacled.theme

import androidx.compose.runtime.Composable

// TODO: switch the language on this platform, for now the system language is used.
@Composable
actual fun ProvideAppLanguage(languageOption: LanguageOption, content: @Composable () -> Unit) = content()
