package at.techbee.spectacled.screens.core.data.ics

import kotlinx.serialization.Serializable

@Serializable
data class RawIcsProperty(
    val name: String,
    val unfoldedLine: String
)