package at.techbee.spectacled.screens.core.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun getPlatformEngine(): HttpClientEngine =
    OkHttp.create {
        config {
            //followRedirects(false)
        }
    }
