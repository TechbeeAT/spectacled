package at.techbee.spectacled.screens.core.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

actual fun getPlatformEngine(): HttpClientEngine = Js.create()
