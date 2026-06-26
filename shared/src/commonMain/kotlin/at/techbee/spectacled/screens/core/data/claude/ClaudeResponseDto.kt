package at.techbee.spectacled.screens.core.data.claude

import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.mapper.ics.parseIcsDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }


@Serializable
data class ClaudeResponseDto(
    @SerialName("content") val content: List<ClaudeContentDto>
) {

    fun applyClaudeResponse(icalEntry: IcalEntry): IcalEntry {
        // Find the first content item of type "text"
        val textContent = content.firstOrNull { it.type == "text" } ?: return icalEntry

        return try {
            // Parse the nested JSON string inside the 'text' field
            val data = json.decodeFromString<ClaudeIcalDataDto>(textContent.text)
            
            icalEntry.copy(
                summary = data.summary ?: icalEntry.summary,
                description = data.description ?: icalEntry.description,
                dtStart = if(icalEntry.isTask() || icalEntry.isJournal()) parseIcsDateTime(data.dtstart) else null,
                due = if(icalEntry.isTask()) parseIcsDateTime(data.due) else null,
                categories = data.categories ?: icalEntry.categories
            )
        } catch (e: Exception) {
            println("Failed to parse AI response: ${e.message}")
            icalEntry
        }
    }
}

@Serializable
data class ClaudeContentDto(
    @SerialName("type") val type: String,
    @SerialName("text") val text: String
)

/**
 * Data structure for the nested JSON string within Claude's response text.
 */
@Serializable
data class ClaudeIcalDataDto(
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("dtstart") val dtstart: String? = null,
    @SerialName("due") val due: String? = null,
    @SerialName("location") val location: String? = null, // Note: IcalEntry currently doesn't have a location field
    @SerialName("categories") val categories: List<String>? = null
)


/*
// SAMPLE RESPONSE
{
  "model": "claude-sonnet-4-6",
  "id": "msg_01VTU55epgVGzo3dA4cC9853",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "{\"summary\":\"Looking forward to meeting at Stadtpark to discuss new app project\",\"description\":\"Hi, looking forward to meeting you soon at Stadtpark to discuss our new app project!\",\"location\":\"Stadtpark\",\"categories\":[\"meeting\",\"app development\",\"project planning\"]}"
    }
  ],
  "stop_reason": "end_turn",
  "stop_sequence": null,
  "stop_details": null,
  "usage": {
    "input_tokens": 138,
    "cache_creation_input_tokens": 0,
    "cache_read_input_tokens": 0,
    "cache_creation": {
      "ephemeral_5m_input_tokens": 0,
      "ephemeral_1h_input_tokens": 0
    },
    "output_tokens": 60,
    "service_tier": "standard",
    "inference_geo": "global"
  }
}
 */