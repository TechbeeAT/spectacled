package at.techbee.spectacled.screens.core.domain

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * Curated, ready-to-use [IcalEntry] samples for app-store screenshots.
 *
 * The goal is to show off what Spectacled can be used for rather than to look empty:
 *  - [journals]  -> VJOURNAL with a date, e.g. a diary or meeting minutes
 *  - [notes]     -> VJOURNAL without a date, e.g. general notes and lists
 *  - [tasks]     -> VTODO for structured task management (incl. sub-tasks, priority and progress)
 *
 * Formatting reminder (see MarkdownVisualTransformation):
 *  - *text*  renders bold
 *  - _text_  renders italic
 *  - http(s) links are auto-detected and made clickable
 *
 * Just push [allSamples] (or the individual lists) into your repository / preview state.
 * All dates are pinned so screenshots stay reproducible — adjust the year if needed.
 */
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
object SampleScreenshotEntries {

    // ---- little helpers so the samples below stay readable ------------------

    /** Date + time, e.g. a meeting that started at a concrete time. */
    private fun at(iso: String) = IcsDateTime(instant = Instant.parse(iso), isDateOnly = false)

    /** Whole-day date only, e.g. a diary day or a task due date. */
    private fun on(isoDate: String) =
        IcsDateTime(instant = Instant.parse("${isoDate}T00:00:00Z"), isDateOnly = true)

    // =========================================================================
    //  JOURNAL  — a diary and meeting minutes (VJOURNAL, has a date)
    // =========================================================================

    val journals: List<IcalEntry> = listOf(

        // --- Diary --------------------------------------------------------
        IcalEntry(
            summary = "A morning that actually started well",
            description = "Woke up before the alarm and went for a run along the river — " +
                "the light on the water was unreal. Came back, made coffee, and finally " +
                "wrote for twenty minutes without checking my phone.\n\n" +
                "_Note to self:_ mornings like this happen when I put the phone in the " +
                "other room the night before.",
            dtStart = on("2026-06-14"),
            categories = listOf("Diary", "Health"),
            status = Status.FINAL,
            classification = Classification.PRIVATE,
            color = Color(0xFF4CAF50),
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Weekend in the mountains 🏔️",
            description = "Drove up to the cabin on Friday evening. Saturday we hiked the " +
                "*Eagle Ridge* loop — 14 km, roughly 900 m of climbing, and completely " +
                "worth it for the view at the top.\n\n" +
                "Highlights:\n" +
                "• Fresh bread from the tiny bakery in the village\n" +
                "• Spotting two ibex on the way down\n" +
                "• No signal for a full day\n\n" +
                "Definitely coming back in autumn.",
            dtStart = on("2026-06-06"),
            categories = listOf("Diary", "Travel"),
            status = Status.FINAL,
            classification = Classification.PRIVATE,
            color = Color(0xFF3F51B5),
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Little wins today",
            description = "Three things I'm grateful for:\n\n" +
                "1. Finished the report a day early\n" +
                "2. Long lunch with Sam — first time in months\n" +
                "3. The tomatoes on the balcony are _finally_ turning red",
            dtStart = on("2026-06-12"),
            categories = listOf("Diary", "Gratitude"),
            status = Status.FINAL,
            classification = Classification.PRIVATE,
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        // --- Meeting minutes ---------------------------------------------
        IcalEntry(
            summary = "Sprint Planning — Sprint 24",
            description = "*Attendees:* Mara, Jonas, Priya, Tom (remote)\n\n" +
                "*Sprint goal:* Ship the new onboarding flow to 10% of users.\n\n" +
                "*Committed:*\n" +
                "• Onboarding screens v2 — Priya\n" +
                "• Feature-flag rollout — Jonas\n" +
                "• Analytics events — Tom\n\n" +
                "*Decisions:*\n" +
                "• Push the settings redesign to next sprint\n" +
                "• Daily stand-up moves to 9:30\n\n" +
                "*Action items:* see the linked tasks in the Tasks list.",
            dtStart = at("2026-06-15T09:00:00Z"),
            categories = listOf("Meetings", "Work"),
            status = Status.FINAL,
            color = Color(0xFF009688),
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Client kick-off — Redesign project",
            description = "*Client:* Northwind Coffee\n" +
                "*Present:* Anna (PM), Leo (design), client team\n\n" +
                "*Scope agreed:*\n" +
                "• New website + online shop\n" +
                "• Brand refresh (logo stays, palette updated)\n\n" +
                "*Open questions:*\n" +
                "• Do they need multi-language from day one?\n" +
                "• Who provides product photography?\n\n" +
                "*Next step:* proposal by _June 22_.",
            dtStart = at("2026-06-11T14:30:00Z"),
            categories = listOf("Meetings", "Clients"),
            status = Status.DRAFT,
            color = Color(0xFF795548),
            calendarComponent = CalendarComponent.VJOURNAL
        )
    )

    // =========================================================================
    //  NOTES  — general notes and lists (VJOURNAL, no date)
    // =========================================================================

    val notes: List<IcalEntry> = listOf(

        IcalEntry(
            summary = "📚 Books to read",
            description = "• *Project Hail Mary* — Andy Weir\n" +
                "• *The Midnight Library* — Matt Haig\n" +
                "• *Thinking in Systems* — Donella Meadows\n" +
                "• *Klara and the Sun* — Kazuo Ishiguro\n\n" +
                "_Ask the library which of these are available as audiobook._",
            categories = listOf(IcalEntry.PINNED_CATEGORY, "Personal"),
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Banana bread recipe 🍌",
            description = "*Ingredients*\n" +
                "• 3 ripe bananas\n" +
                "• 80 g melted butter\n" +
                "• 150 g sugar\n" +
                "• 1 egg\n" +
                "• 190 g flour\n" +
                "• 1 tsp baking soda, pinch of salt\n\n" +
                "*Method*\n" +
                "Mash bananas, mix in butter, then sugar, egg, salt. Fold in flour + " +
                "baking soda. Bake at *175 °C* for ~50 min.\n\n" +
                "_Best with a handful of walnuts._",
            categories = listOf("Recipes", "Kitchen"),
            color = Color(0xFFFFC107),
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Gift ideas 🎁",
            description = "*Mum* — that ceramics workshop she mentioned\n" +
                "*Alex* — noise-cancelling headphones\n" +
                "*Team* — voucher for the coffee place downstairs\n\n" +
                "_Budget: keep each under €80._",
            categories = listOf("Personal", "Shopping"),
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Home Wi-Fi & guest info",
            description = "*Network:* Fjord-Guest\n" +
                "*Password:* seagull-42-harbour\n\n" +
                "Smart TV: the Netflix app is already signed in.\n" +
                "Spare key is with the neighbour in flat 3B.",
            categories = listOf("Home"),
            classification = Classification.CONFIDENTIAL,
            calendarComponent = CalendarComponent.VJOURNAL
        ),

        IcalEntry(
            summary = "Ideas parking lot 💡",
            description = "Random things worth a second look:\n\n" +
                "• A weekly \"no-meeting\" afternoon\n" +
                "• Try the standing desk for one week straight\n" +
                "• Newsletter for the running club\n" +
                "• Learn enough Portuguese for the trip",
            categories = listOf("Ideas"),
            calendarComponent = CalendarComponent.VJOURNAL
        )
    )

    // =========================================================================
    //  TASKS  — structured task management (VTODO, incl. sub-tasks)
    // =========================================================================

    // Stable parent UIDs so the sub-tasks below can reference them.
    private const val UID_JAPAN = "sample-task-plan-japan"
    private const val UID_RELAUNCH = "sample-task-website-relaunch"

    val tasks: List<IcalEntry> = listOf(

        // --- Project with sub-tasks: Trip planning -----------------------
        IcalEntry(
            uid = UID_JAPAN,
            summary = "Plan trip to Japan 🇯🇵",
            description = "Two weeks in October — Tokyo, Kyoto, Kanazawa.",
            due = on("2026-08-01"),
            status = Status.IN_PROCESS,
            percentComplete = 40,
            priority = 5,
            categories = listOf("Travel", "Personal"),
            color = Color(0xFFE91E63),
            calendarComponent = CalendarComponent.VTODO
        ),
        subtaskOf(
            UID_JAPAN,
            summary = "Book return flights",
            due = on("2026-07-01"),
            status = Status.COMPLETED,
            percentComplete = 100,
            priority = 1
        ),
        subtaskOf(
            UID_JAPAN,
            summary = "Reserve ryokan in Kyoto",
            due = on("2026-07-10"),
            status = Status.IN_PROCESS,
            percentComplete = 50,
            priority = 3
        ),
        subtaskOf(
            UID_JAPAN,
            summary = "Order Japan Rail Pass",
            due = on("2026-08-20"),
            status = Status.NEEDS_ACTION,
            priority = 5
        ),
        subtaskOf(
            UID_JAPAN,
            summary = "Draft day-by-day itinerary",
            status = Status.NEEDS_ACTION,
            priority = 6
        ),

        // --- Project with sub-tasks: Website relaunch --------------------
        IcalEntry(
            uid = UID_RELAUNCH,
            summary = "Website relaunch",
            description = "Go-live target: end of the quarter.",
            due = on("2026-09-30"),
            status = Status.IN_PROCESS,
            percentComplete = 25,
            priority = 2,
            categories = listOf("Work", "Project"),
            color = Color(0xFF3F51B5),
            calendarComponent = CalendarComponent.VTODO
        ),
        subtaskOf(
            UID_RELAUNCH,
            summary = "Finalize copy for landing page",
            due = on("2026-08-15"),
            status = Status.IN_PROCESS,
            percentComplete = 70,
            priority = 3
        ),
        subtaskOf(
            UID_RELAUNCH,
            summary = "Migrate blog posts",
            due = on("2026-09-01"),
            status = Status.NEEDS_ACTION,
            priority = 4
        ),
        subtaskOf(
            UID_RELAUNCH,
            summary = "Set up analytics & consent banner",
            status = Status.NEEDS_ACTION,
            priority = 5
        ),

        // --- Standalone everyday tasks -----------------------------------
        IcalEntry(
            summary = "Renew passport",
            description = "Expires in October — book an appointment at the town hall.",
            due = on("2026-07-25"),
            status = Status.NEEDS_ACTION,
            priority = 1,
            categories = listOf(IcalEntry.PINNED_CATEGORY, "Admin"),
            color = Color(0xFFF44336),
            calendarComponent = CalendarComponent.VTODO
        ),
        IcalEntry(
            summary = "Weekly groceries 🛒",
            description = "• Oats & coffee\n• Olive oil\n• Spinach, tomatoes, lemons\n" +
                "• Pasta\n• Dish soap",
            due = on("2026-07-18"),
            status = Status.NEEDS_ACTION,
            priority = 6,
            categories = listOf("Home", "Shopping"),
            calendarComponent = CalendarComponent.VTODO
        ),
        IcalEntry(
            summary = "Call the dentist",
            due = on("2026-07-17"),
            status = Status.NEEDS_ACTION,
            priority = 4,
            categories = listOf("Health"),
            calendarComponent = CalendarComponent.VTODO
        ),
        IcalEntry(
            summary = "Water the plants",
            status = Status.COMPLETED,
            percentComplete = 100,
            categories = listOf("Home"),
            calendarComponent = CalendarComponent.VTODO
        )
    )

    /** Everything in one list, handy for seeding a single screen. */
    val allSamples: List<IcalEntry> = journals + notes + tasks

    // ---- sub-task factory ---------------------------------------------------

    private fun subtaskOf(
        parentUid: String,
        summary: String,
        due: IcsDateTime? = null,
        status: Status? = null,
        percentComplete: Long = 0,
        priority: Long? = null,
    ) = IcalEntry(
        summary = summary,
        due = due,
        status = status,
        percentComplete = percentComplete,
        priority = priority,
        parentUid = parentUid,
        relType = "PARENT",
        calendarComponent = CalendarComponent.VTODO
    )
}
