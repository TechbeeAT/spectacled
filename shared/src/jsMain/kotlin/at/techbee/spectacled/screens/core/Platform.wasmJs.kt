package at.techbee.spectacled.screens.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


class WasmPlatform: Platform {
    override val platform: Platforms = Platforms.WASM
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

// format datetime. Solution taken over from ChatGPT.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("lang => new Intl.DateTimeFormat(lang, { dateStyle: 'medium', timeStyle: 'short' })")
private external fun createFormatter(lang: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(formatter, millis) => formatter.format(new Date(millis))")
private external fun formatWithFormatter(formatter: JsAny, millis: Double): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => navigator.language")
private external fun getNavigatorLanguage(): String

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default