package at.techbee.spectacled.screens.core.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Unpublished
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import io.ktor.http.Url
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.status_cancelled
import spectacled.shared.generated.resources.status_completed
import spectacled.shared.generated.resources.status_draft
import spectacled.shared.generated.resources.status_final
import spectacled.shared.generated.resources.status_in_process
import spectacled.shared.generated.resources.status_needs_action
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


enum class SyncState {
    LOCAL_MODIFIED,
    LOCAL_DELETED,
    CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED,
    CONFLICT_LOCAL_DELETED_SERVER_MODIFIED,
    CONFLICT_LOCAL_MODIFIED_SERVER_DELETED,
    USER_DECIDED_CLIENT_WINS,
    USER_DECIDED_SERVER_WINS,
    SYNCED,
    REMOTE_DELETED_LOCAL_TRASHBIN;

    fun isDeletedState() = this == LOCAL_DELETED || this == REMOTE_DELETED_LOCAL_TRASHBIN

    fun isConflictState() = this == CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED
            || this == CONFLICT_LOCAL_DELETED_SERVER_MODIFIED
            || this == CONFLICT_LOCAL_MODIFIED_SERVER_DELETED
}


@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
data class IcalEntry(
    val id: Long = 0L,
    val calendarId: Long = 0L,
    val uid: String = getRandomUUID(),
    val summary: String? = null,
    val description: String? = null,
    val dtStart: IcsDateTime? = null,
    val due: IcsDateTime? = null,
    val completed: IcsDateTime? = null,
    val status: Status? = null,
    val percentComplete: Long = 0,
    val priority: Long? = null,
    val dtstamp: IcsDateTime = IcsDateTime.now(),
    val color: Color? = null,
    val sequence: Long? = null,
    val classification: Classification? = null,
    val categories: List<String> = emptyList(),
    val created: IcsDateTime = IcsDateTime.now(),
    val lastModified: IcsDateTime? = IcsDateTime.now(),
    val orderNo: Long? = null,
    val syncState: SyncState = SyncState.LOCAL_MODIFIED,
    val extraProperties: List<RawIcsProperty> = emptyList(),
    val etag: String? = null,
    val href: Url? = null,
    val calendarComponent: CalendarComponent,
    val parentUid: String? = null,
    val relType: String? = "PARENT"
    ) {

    companion object {

        const val PINNED_CATEGORY = "\uD83D\uDCCC"

        private val sampleParagraphs = listOf(
            "Lorem *ipsum* dolor sit _amet_, consectetur adipiscing elit. *_Nulla_* id libero felis. Vestibulum tristique suscipit elit, et pharetra leo posuere non. Suspendisse purus lacus, pretium et porttitor a, consequat at sapien. Integer dictum sagittis lacus, sed semper massa euismod non. Pellentesque viverra nunc id felis ullamcorper, at viverra nisl consequat. Quisque et fermentum elit, tincidunt molestie nulla. Proin leo neque, luctus ac nisi convallis, ultricies tempor felis. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nulla facilisi. Phasellus sit amet diam elementum, ultrices lorem sed, blandit lacus.",
            "Quisque egestas rhoncus quam nec ornare. Donec vitae pharetra quam, ut tincidunt tellus. Pellentesque at lectus eget libero ullamcorper tristique. Maecenas ornare nisi ante, vel volutpat elit sagittis et. Nulla ac arcu dui. Nam accumsan nunc dui, non posuere magna imperdiet congue. Nunc non feugiat ante, a tempus augue. In quis porttitor mi. Integer arcu mauris, suscipit in risus non, vestibulum aliquam nibh. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras urna neque, aliquet sed consectetur sit amet, porta eu mauris. Proin et enim mollis, egestas metus nec, accumsan augue. Vivamus sed justo non leo accumsan ultrices. Maecenas condimentum neque ac justo viverra, non tincidunt nisl consequat." ,
            "Nunc ut sem et magna consectetur ullamcorper sed commodo libero. Ut suscipit hendrerit tortor id tempus. Nullam vel imperdiet orci, ut luctus neque. Integer nec velit sit amet augue facilisis ultricies. Morbi lectus lectus, iaculis et aliquet eu, rutrum vel ante. Vestibulum sed felis maximus, sodales tortor ut, posuere libero. Duis non odio sollicitudin, imperdiet arcu vel, finibus nunc. Curabitur quis eleifend metus. Pellentesque consectetur et lacus mattis posuere. Sed ac tellus aliquam, ultrices turpis vel, ultrices diam. Sed semper ligula dolor, sit amet malesuada risus commodo sed. Donec dictum, lorem eu venenatis elementum, massa enim finibus velit, vitae consequat ligula tortor a arcu.",
            "In hac habitasse platea dictumst. In posuere, ligula ut fermentum euismod, eros nibh aliquam odio, sit amet sodales arcu diam vitae quam. Suspendisse commodo urna in felis ultricies, ac rhoncus lacus sodales. Praesent nisl ligula, aliquam sed ex vel, cursus eleifend turpis. Quisque rhoncus lobortis aliquam. Integer nisl nisi, rutrum at aliquam sed, sodales at libero. Cras orci tellus, convallis at ultrices quis, tincidunt ut purus. Donec dapibus mi sit amet urna sollicitudin porttitor. Donec blandit sagittis condimentum. Duis tortor nisi, feugiat sed posuere mollis, pharetra et magna. Aliquam vel augue vel enim lobortis efficitur eget sed tellus. Vestibulum laoreet orci vel dolor faucibus dictum quis in lacus. Integer vulputate, est non gravida tempus, leo odio hendrerit ligula, vel lacinia tellus erat sed mi. Etiam viverra et dolor vel vestibulum.",
            "In a dui luctus nisi elementum cursus. Vivamus fermentum feugiat nibh in tincidunt. Quisque egestas orci in lorem volutpat bibendum. Nunc ut lacus sed ante aliquam ultrices id eu purus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Ut et vulputate lorem, maximus tincidunt mauris. Vivamus velit risus, mattis vitae fermentum at, cursus eget urna. Aliquam sit amet arcu molestie dui consectetur congue. Aliquam ut leo vitae eros efficitur varius sit amet et est. Maecenas placerat risus ac auctor pretium. Suspendisse non erat id eros dignissim lacinia. Quisque non libero id nisl porttitor elementum. In consectetur id urna in fermentum. In et iaculis felis.",
            "Generated 5 paragraphs, 547 words, 3653 bytes of Lorem Ipsum",
            "help@lipsum.com",
            "Privacy Policy",
            "freestar"
        )

        fun newJournal() = IcalEntry(dtStart = IcsDateTime.now(), calendarComponent = CalendarComponent.VJOURNAL)
        fun newNote() = IcalEntry(calendarComponent = CalendarComponent.VJOURNAL)
        fun newTask() = IcalEntry(calendarComponent = CalendarComponent.VTODO)

        fun getSampleIcalEntry()= IcalEntry(
            summary = sampleParagraphs[0].substring(0, 30),
            description = sampleParagraphs[0],
            categories = listOf("Category 1", "Category 2"),
            calendarComponent = CalendarComponent.VJOURNAL
        )

        fun getSampleJournal()= newJournal().copy(
            summary = sampleParagraphs[0].substring(0, 30),
            description = sampleParagraphs[0],
            dtStart = IcsDateTime.now(),
            categories = listOf("Category 1", "Category 2"),
            calendarComponent = CalendarComponent.VJOURNAL
        )

        fun getSampleTask()= newTask().copy(
            summary = sampleParagraphs[0].substring(0, 30),
            description = sampleParagraphs[0],
            dtStart = IcsDateTime.now(),
            due = IcsDateTime.now(),
            categories = listOf("Category 1", "Category 2"),
            calendarComponent = CalendarComponent.VTODO
        )

        fun getRandomUUID() = Uuid.random().toString()
    }

    fun isNote() = calendarComponent == CalendarComponent.VJOURNAL && dtStart == null
    fun isJournal() = calendarComponent == CalendarComponent.VJOURNAL && dtStart != null
    fun isTask() = calendarComponent == CalendarComponent.VTODO
    fun isSubtask() = parentUid != null

    fun getProgressTriState() = when {
        percentComplete == 0L -> ToggleableState.Off
        percentComplete in 1L .. 99L -> ToggleableState.Indeterminate
        percentComplete == 100L -> ToggleableState.On
        status == null || status == Status.NEEDS_ACTION -> ToggleableState.Off
        status == Status.IN_PROCESS -> ToggleableState.Indeterminate
        status == Status.COMPLETED -> ToggleableState.On
        else -> ToggleableState.Indeterminate   // undefined
    }

    @Composable
    fun getAnnotatedStringForShare(): AnnotatedString {
        return AnnotatedString.Builder().apply {
            summary?.let {
                pushStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold).toSpanStyle())
                append(it)
                pop()
                append("\n") // Add some space between summary and description
            }
            description?.let {
                append(it)
                append("\n")
            }
            if (categories.isNotEmpty()) {
                append("\n")
                pushStyle(MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic).toSpanStyle())
                append(stringResource(Res.string.category))
                append(": ")
                append(categories.joinToString(", "))
                pop()
            }
        }.toAnnotatedString()
    }

    fun getPlainTextForShare(categoryLabel: String): String {
        return buildString {
            summary?.let {
                append(it)
                append("\n")
            }
            description?.let {
                append(it)
                append("\n")
            }
            if (categories.isNotEmpty()) {
                append("\n")
                append(categoryLabel)
                append(": ")
                append(categories.joinToString(", "))
            }
        }
    }
}



enum class Status(
    val rfcName: String,
    val stringRes: StringResource,
    val vectorIcon: ImageVector?
) {
    FINAL("FINAL", Res.string.status_final, Icons.Outlined.CheckCircle),
    DRAFT("DRAFT", Res.string.status_draft, Icons.Outlined.Unpublished),

    NEEDS_ACTION("NEEDS-ACTION", Res.string.status_needs_action, null),
    IN_PROCESS("IN-PROCESS", Res.string.status_in_process, null),
    COMPLETED("COMPLETED", Res.string.status_completed, null),
    CANCELLED("CANCELLED", Res.string.status_cancelled, Icons.Outlined.Cancel);

    companion object {
        fun entriesForComponent(calendarComponent: CalendarComponent): List<Status> {
            return when(calendarComponent) {
                CalendarComponent.VEVENT -> emptyList()
                CalendarComponent.VJOURNAL -> listOf(DRAFT, FINAL, CANCELLED)
                CalendarComponent.VTODO -> listOf(NEEDS_ACTION, IN_PROCESS, COMPLETED, CANCELLED)
            }
        }
    }


}

enum class Classification {
    PUBLIC, PRIVATE, CONFIDENTIAL
}
