-- Sample entries for Spectacled app-store screenshots.
-- Replace <calendarId> with the target calendar's id before running.
-- Mirrors IcalEntry.toDto(): dates as YYYYMMDD / YYYYMMDDTHHMMSS (floating),
-- color as signed 32-bit ARGB, categories comma-joined, enums stored by name.

-- ===== JOURNALS (VJOURNAL, with date) =====
INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-journal-diary-1', 'A morning that actually started well', 'Woke up before the alarm and went for a run along the river — the light on the water was unreal. Came back, made coffee, and finally wrote for twenty minutes without checking my phone.

_Note to self:_ mornings like this happen when I put the phone in the other room the night before.', '20260614', NULL, NULL, NULL, NULL, NULL, 'FINAL', NULL, NULL, 'PRIVATE', '20260716T120000', -11751600, NULL, 'Diary,Health', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-journal-diary-2', 'Weekend in the mountains 🏔️', 'Drove up to the cabin on Friday evening. Saturday we hiked the *Eagle Ridge* loop — 14 km, roughly 900 m of climbing, and completely worth it for the view at the top.

Highlights:
• Fresh bread from the tiny bakery in the village
• Spotting two ibex on the way down
• No signal for a full day

Definitely coming back in autumn.', '20260606', NULL, NULL, NULL, NULL, NULL, 'FINAL', NULL, NULL, 'PRIVATE', '20260716T120000', -12627531, NULL, 'Diary,Travel', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-journal-diary-3', 'Little wins today', 'Three things I''m grateful for:

1. Finished the report a day early
2. Long lunch with Sam — first time in months
3. The tomatoes on the balcony are _finally_ turning red', '20260612', NULL, NULL, NULL, NULL, NULL, 'FINAL', NULL, NULL, 'PRIVATE', '20260716T120000', NULL, NULL, 'Diary,Gratitude', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-journal-minutes-1', 'Sprint Planning — Sprint 24', '*Attendees:* Mara, Jonas, Priya, Tom (remote)

*Sprint goal:* Ship the new onboarding flow to 10% of users.

*Committed:*
• Onboarding screens v2 — Priya
• Feature-flag rollout — Jonas
• Analytics events — Tom

*Decisions:*
• Push the settings redesign to next sprint
• Daily stand-up moves to 9:30

*Action items:* see the linked tasks in the Tasks list.', '20260615T090000', NULL, NULL, NULL, NULL, NULL, 'FINAL', NULL, NULL, NULL, '20260716T120000', -16738680, NULL, 'Meetings,Work', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-journal-minutes-2', 'Client kick-off — Redesign project', '*Client:* Northwind Coffee
*Present:* Anna (PM), Leo (design), client team

*Scope agreed:*
• New website + online shop
• Brand refresh (logo stays, palette updated)

*Open questions:*
• Do they need multi-language from day one?
• Who provides product photography?

*Next step:* proposal by _June 22_.', '20260611T143000', NULL, NULL, NULL, NULL, NULL, 'DRAFT', NULL, NULL, NULL, '20260716T120000', -8825528, NULL, 'Meetings,Clients', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

-- ===== NOTES (VJOURNAL, no date) =====
INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-note-books', '📚 Books to read', '• *Project Hail Mary* — Andy Weir
• *The Midnight Library* — Matt Haig
• *Thinking in Systems* — Donella Meadows
• *Klara and the Sun* — Kazuo Ishiguro

_Ask the library which of these are available as audiobook._', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '20260716T120000', NULL, NULL, '📌,Personal', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-note-recipe', 'Banana bread recipe 🍌', '*Ingredients*
• 3 ripe bananas
• 80 g melted butter
• 150 g sugar
• 1 egg
• 190 g flour
• 1 tsp baking soda, pinch of salt

*Method*
Mash bananas, mix in butter, then sugar, egg, salt. Fold in flour + baking soda. Bake at *175 °C* for ~50 min.

_Best with a handful of walnuts._', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '20260716T120000', -16121, NULL, 'Recipes,Kitchen', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-note-gifts', 'Gift ideas 🎁', '*Mum* — that ceramics workshop she mentioned
*Alex* — noise-cancelling headphones
*Team* — voucher for the coffee place downstairs

_Budget: keep each under €80._', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '20260716T120000', NULL, NULL, 'Personal,Shopping', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-note-wifi', 'Home Wi-Fi & guest info', '*Network:* Fjord-Guest
*Password:* seagull-42-harbour

Smart TV: the Netflix app is already signed in.
Spare key is with the neighbour in flat 3B.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CONFIDENTIAL', '20260716T120000', NULL, NULL, 'Home', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-note-ideas', 'Ideas parking lot 💡', 'Random things worth a second look:

• A weekly "no-meeting" afternoon
• Try the standing desk for one week straight
• Newsletter for the running club
• Learn enough Portuguese for the trip', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '20260716T120000', NULL, NULL, 'Ideas', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VJOURNAL', NULL, NULL, NULL);

-- ===== TASKS (VTODO) =====
INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-plan-japan', 'Plan trip to Japan 🇯🇵', 'Two weeks in October — Tokyo, Kyoto, Kanazawa.', NULL, NULL, '20260801', NULL, NULL, NULL, 'IN_PROCESS', 40, 5, NULL, '20260716T120000', -1499549, NULL, 'Travel,Personal', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-japan-flights', 'Book return flights', NULL, NULL, NULL, '20260701', NULL, NULL, NULL, 'COMPLETED', 100, 1, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-plan-japan', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-japan-ryokan', 'Reserve ryokan in Kyoto', NULL, NULL, NULL, '20260710', NULL, NULL, NULL, 'IN_PROCESS', 50, 3, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-plan-japan', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-japan-rail', 'Order Japan Rail Pass', NULL, NULL, NULL, '20260820', NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 5, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-plan-japan', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-japan-itinerary', 'Draft day-by-day itinerary', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 6, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-plan-japan', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-website-relaunch', 'Website relaunch', 'Go-live target: end of the quarter.', NULL, NULL, '20260930', NULL, NULL, NULL, 'IN_PROCESS', 25, 2, NULL, '20260716T120000', -12627531, NULL, 'Work,Project', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-relaunch-copy', 'Finalize copy for landing page', NULL, NULL, NULL, '20260815', NULL, NULL, NULL, 'IN_PROCESS', 70, 3, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-website-relaunch', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-relaunch-blog', 'Migrate blog posts', NULL, NULL, NULL, '20260901', NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 4, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-website-relaunch', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-relaunch-analytics', 'Set up analytics & consent banner', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 5, NULL, '20260716T120000', NULL, NULL, NULL, '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', 'sample-task-website-relaunch', 'PARENT', NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-passport', 'Renew passport', 'Expires in October — book an appointment at the town hall.', NULL, NULL, '20260725', NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 1, NULL, '20260716T120000', -769226, NULL, '📌,Admin', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-groceries', 'Weekly groceries 🛒', '• Oats & coffee
• Olive oil
• Spinach, tomatoes, lemons
• Pasta
• Dish soap', NULL, NULL, '20260718', NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 6, NULL, '20260716T120000', NULL, NULL, 'Home,Shopping', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-dentist', 'Call the dentist', NULL, NULL, NULL, '20260717', NULL, NULL, NULL, 'NEEDS_ACTION', NULL, 4, NULL, '20260716T120000', NULL, NULL, 'Health', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', NULL, NULL, NULL);

INSERT INTO IcalEntryDto (calendarId, uid, summary, description, dtstart, dtStartTimeZone, due, dueTimeZone, completed, completedTimeZone, status, percentComplete, priority, classification, dtstamp, color, sequence, categories, created, lastModified, extraProperties, syncState, etag, href, calendarComponent, parentUid, relType, url)
VALUES (<calendarId>, 'sample-task-plants', 'Water the plants', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', 100, NULL, NULL, '20260716T120000', NULL, NULL, 'Home', '20260716T120000', '20260716T120000', NULL, 'LOCAL_MODIFIED', NULL, NULL, 'VTODO', NULL, NULL, NULL);
