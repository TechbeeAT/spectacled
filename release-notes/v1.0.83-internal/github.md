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
- The About screen now links to Spectacled on Mastodon

### Improvements
- The release notes in the About screen are now shown with proper formatting, with badges for the version and for pre-releases
- Release builds only list release versions in the About screen; beta notes appear only in test builds
- "Sponsor" in the About screen is now called "Donate"
- The About sheet no longer closes by an accidental swipe
- When you reload an account, the app tells you which folders were removed because they no longer exist on the server (#220)

### Fixes
- Sync is much faster for folders with large inline attachments, which could take minutes before (#221)
- Desktop: sync keeps working after a failed sync, instead of stopping until the app is restarted (#213)
- After syncing folders in the account settings, the result is shown once the sync has finished, instead of the result of the previous sync (#219)
- Saving a folder that was deleted on the server no longer reports it as deleted by you or leaves the sheet loading forever; the app removes it and tells you why (#220)
- Fixed colors in the About screen

This is a test release. Feedback is very welcome: open an [issue](https://github.com/TechbeeAT/spectacled/issues/new/choose) or write to [spectacled@techbee.at](mailto:spectacled@techbee.at).

**Full changelog**: https://github.com/TechbeeAT/spectacled/compare/v1.0.81-beta...v1.0.83-internal
