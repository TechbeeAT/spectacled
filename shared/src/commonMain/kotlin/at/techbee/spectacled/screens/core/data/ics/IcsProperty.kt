package at.techbee.spectacled.screens.core.data.ics


data class IcsProperty(
    val name: String,
    val params: Map<String, String>,
    /** The unescaped property value - what almost every consumer wants. */
    val value: String,
    /**
     * The still-escaped value as it appeared on the wire. Needed for list-valued
     * properties like CATEGORIES, where the list must be split on unescaped commas
     * BEFORE unescaping the elements - after unescaping, a comma that was escaped
     * inside a value is indistinguishable from a separator.
     */
    val rawValue: String
)