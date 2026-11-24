## Welcome to Spectacled

Spectacled is a family of open, standards-based apps for journals, notes and tasks. Your entries are stored as iCalendar and synced over CalDAV with a server you choose, such as Nextcloud, Radicale or any other CalDAV server. There is no proprietary backend and no account to create: your data stays on your own server.

### Three apps, one family

- **Spectacled Journals**: dated journal entries
- **Spectacled Notes**: free-form notes
- **Spectacled Tasks**: to-dos with status, priority and due dates

All three are available on Android, iOS, Web and Desktop (Windows, macOS, Linux), with the same interface everywhere.

### Features

- Connect to any CalDAV server and browse its collections
- Create, edit and delete entries with summary, description, categories and classification
- Search and filter your entries
- Attachments, inline or linked
- Link entries as parent and child
- Background sync with a local cache, so the apps keep working offline
- Light and dark theme

## Changes during the beta

### What's new
- Desktop and Web: alarms now show up as notifications while the app is open; clicking one opens the entry. Alarms missed while the app was closed are shown at the next start. On macOS, notifications appear under the app's own name, with a menu bar icon while the app runs (#233, #240)
- Task notifications now have "Mark done" and "Snooze" buttons on Android and iOS. Snooze reminds you again in 10 minutes; "Mark done" is only offered for tasks in folders you can edit (#231, #232)
- iOS: alarms now show up as notifications at the set time, and tapping one opens the entry (#223)
- The About screen now links to Spectacled on Mastodon

### Improvements
- The release notes in the About screen are now shown with proper formatting, with badges for the version and for pre-releases
- Release builds only list release versions in the About screen; beta notes appear only in test builds
- "Sponsor" in the About screen is now called "Donate"
- The About sheet no longer closes by an accidental swipe
- When you reload an account, the app tells you which folders were removed because they no longer exist on the server (#220)
- Editing screen: the action button stays visible while you edit, and is only hidden when the keyboard leaves little room; it also moves up when the formatting bar is shown (#238)
- Desktop and Web: swipe actions on list items, pull to refresh and swiping bottom sheets are now only used on phones and tablets, so they no longer get in the way of mouse use
- Android: new notification icon, based on the Spectacled logo

### Fixes
- iOS: tapping "Mark done" or "Snooze" in a task notification no longer crashes the app (#242)
- New journals no longer get a start time shifted by your time zone offset, which could also put them on the wrong day. "Today" and the date picker now use your local date. Journals created with earlier versions may still show a shifted date or time; the original time zone was not saved, so please correct affected entries by hand (#237, #239)
- Web: the app no longer stops responding right after start (#236)
- Sync is much faster for folders with large inline attachments, which could take minutes before (#221)
- Desktop: sync keeps working after a failed sync, instead of stopping until the app is restarted (#213)
- After syncing folders in the account settings, the result is shown once the sync has finished, instead of the result of the previous sync (#219)
- Saving a folder that was deleted on the server no longer reports it as deleted by you or leaves the sheet loading forever; the app removes it and tells you why (#220)
- Languages written right to left, such as Arabic and Hebrew, are now shown right to left in the entry list, the search bar, the editing screen and the folder list
- Fixed colors in the About screen
- Notification and Android widget texts are now translated, and the widget follows a language change right away

This is a beta release. Feedback is very welcome: open an [issue](https://github.com/TechbeeAT/spectacled/issues/new/choose) or write to [spectacled@techbee.at](mailto:spectacled@techbee.at).

**Full changelog**: https://github.com/TechbeeAT/spectacled/compare/v1.0.81-beta...v1.0.89-beta
