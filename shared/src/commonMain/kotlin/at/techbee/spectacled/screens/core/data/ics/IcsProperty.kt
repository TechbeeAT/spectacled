package at.techbee.spectacled.screens.core.data.ics


data class IcsProperty(
    val name: String,
    val params: Map<String, String>,
    val value: String
)