package at.techbee.spectacled.screens.core.domain

import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.priority_high
import spectacled.shared.generated.resources.priority_low
import spectacled.shared.generated.resources.priority_medium
import spectacled.shared.generated.resources.priority_not_specified

/**
 * The coarse priority levels offered in the UI, ordered from lowest to highest so that
 * [ordinal] can be used directly as a slider step.
 *
 * RFC 5545 (3.8.1.9) defines PRIORITY as 0 (undefined) or 1 (highest) to 9 (lowest) and groups
 * 1-4 as high, 5 as medium and 6-9 as low. [icsValue] is the value written when a level is
 * picked; [fromIcsValue] maps any stored value back to its level.
 */
enum class Priority(
    val icsValue: Long?,
    val stringRes: StringResource
) {
    NOT_SPECIFIED(null, Res.string.priority_not_specified),
    LOW(9L, Res.string.priority_low),
    MEDIUM(5L, Res.string.priority_medium),
    HIGH(1L, Res.string.priority_high);

    companion object {
        fun fromIcsValue(icsValue: Long?): Priority = when (icsValue) {
            in 1L..4L -> HIGH
            5L -> MEDIUM
            in 6L..9L -> LOW
            else -> NOT_SPECIFIED   // null, 0 (undefined per RFC 5545) and out-of-range values
        }

        fun fromSliderStep(step: Int): Priority = entries.getOrElse(step) { NOT_SPECIFIED }
    }
}
