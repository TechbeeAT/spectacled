# Getting Started with Spectacled — voice-over script

**Video:** "Getting started with Spectacled" (onboarding walkthrough)
**Published:** https://youtu.be/lu-Grqnp4no
**Runtime target:** ~3:52
**Example app used on screen:** Spectacled Notes
**Example provider:** Murena Workspace
**Voice:** calm, unhurried, second person ("you"). Read at ~140 wpm — every timing below assumes that.

Word counts per section are given so the edit can be cut to picture. Every UI label
quoted in the voice-over matches the strings in the app exactly.

---

## 0. Cold open (0:00 – 0:13)

**On screen:** Spectacled logo animation, then a fast montage — a note being typed,
a folder list, the same entry appearing on a second device.

> Spectacled keeps your notes, journals, and tasks on a server you choose — not on ours.
> Let's connect one and write the first entry. The setup itself takes about two minutes.

*(31 words · ~13s)*

> **Alternative opens,** if you want a sharper hook:
>
> - *"Every notes app asks you to trust it with your notes. Spectacled asks you to pick a
>   server instead. Let's connect one and write the first entry."* (27 words)
> - *"Your notes shouldn't live on someone else's server — least of all ours. Spectacled
>   keeps them on a server you choose, in a format anything can read. Here's how to set it
>   up."* (33 words)
> - *"The iCalendar standard has had a slot for journal entries since 1998. Almost nothing
>   ever used it. Spectacled does — on a server you choose. Let's set it up."* (29 words —
>   strongest for the existing community, weakest for newcomers)

---

## 1. Opening the app for the first time (0:15 – 0:49)

**On screen:** Launch Spectacled Notes on a clean install. The welcome sheet slides
up on its own. Hold on the headline and the two option cards.

> Open Spectacled for the first time and it asks for exactly one thing: where your data
> should live.
>
> There's no Spectacled account, and nothing to sign up for here. Your entries live on a
> CalDAV server — the open standard behind shared calendars and to-do lists — and Spectacled
> is only the client that reads and writes them.
>
> So the welcome screen offers two paths. Option one, if you already have a CalDAV account.
> Option two, if you need one.

*(80 words · ~34s — trim the second paragraph to one sentence if the section runs long)*

---

## 2. Choosing a provider (0:51 – 1:40)

**On screen:** Tap the **"Need an account?"** card. The pager slides to the provider
list. Scroll slowly past the category headings, then settle on the Murena Workspace chip
and let the badges read.

> Tap "Need an account?" and Spectacled shows a short list of providers it's known to work
> well with.
>
> These are recommendations, not requirements — Spectacled is provider-independent. Some of
> these links do earn the project a referral commission, which is stated right on the screen
> and helps fund development.
>
> For this video we'll use Murena Workspace: a de-Googled, privacy-focused workspace from
> France, built on Nextcloud, with a free tier to get started. The badges tell you what
> matters at a glance — where it's hosted, what it's built on, and that there's a free plan.
>
> Tapping the card opens Murena in your browser, where you create the account. Sign up,
> then come back to Spectacled.

*(115 words · ~49s — see the note below on covering the sign-up)*

> **Editing note:** the Murena sign-up itself happens outside the app. Either cut away to a
> 6–8 second sped-up browser capture under the last paragraph, or hard-cut back to the app
> with a "…once you've signed up" title card. Don't film the real sign-up form with a real
> address.

> **Editing note:** in Spectacled Notes and Journals the provider list only shows hosts that
> support journals, so the "Email providers with CalDAV support" category does not appear at
> all — only "Nextcloud providers" and "Self-hosted solutions". Don't scroll looking for it.

---

## 3. Adding the account (1:42 – 2:21)

**On screen:** Tap **Back**, then the **"Have an account?"** card. Type the username
(`you@murena.io`) and password, and **leave the Server field empty** — the account resolves
from the username's domain. Open the **⋮** menu at the end of the Server field briefly as an
aside, to show the ready-made provider addresses, then close it. Tap **Add account** and
hold on the progress indicator until the account appears in the list.

> Now back in the app, take option one — "Have an account?" — and fill in the form.
>
> Server, username, password — and often just two of those. Leave the server blank and
> Spectacled works it out from the domain in your username. If it can't, the menu at the end
> of the field has ready-made addresses for the providers it knows.
>
> Tap "Add account". Spectacled connects, asks the server what it supports, and lists only
> the folders that can actually hold notes — anything calendar- or task-only simply won't
> appear.

*(91 words · ~39s)*

> **Editing note:** blur or replace the username and password on screen, and use a throwaway
> account for the capture. The password field's reveal toggle should stay untouched.

---

## 4. Creating a folder (2:23 – 2:59)

**On screen:** On the Accounts screen, open the **⋮** menu on the account row and choose
**Create folder**. Type a name, add a short description, pick a colour from the selector.
Tap **Create folder** and watch the new card drop into the list.

> A fresh account usually has nowhere to put things yet, so let's make a folder.
>
> Open the menu on the account and choose "Create folder". Give it a name, a description and a colour if
> you want.
>
> There's one switch worth knowing about: "Include support for subtasks". Leave it on and
> this folder can hold tasks alongside your notes, so an entry can have checkable subtasks
> under it.
>
> Tap "Create folder", and Spectacled creates it on the server — not just on this device.

*(84 words · ~36s)*

> **Editing note:** the subtasks switch only appears in Spectacled Notes and Journals when
> creating a *new* folder. If you record this section in Spectacled Tasks, cut that paragraph.

> **Editing note:** leaving the switch on asks the server for a collection supporting both
> journals and tasks. Nextcloud — and so Murena — accepts that; stricter servers may refuse
> it. If you ever re-record against a different host, confirm the folder is actually created
> before committing to the take.

---

## 5. Adding an entry (3:01 – 3:33)

**On screen:** Tap the new folder card to open it. Empty list. Tap the **Add note** button.
On the details screen, type a summary, then a couple of lines of description — pause so the
autosave lands. The category selector opens from the **label icon in the bottom bar**, not
from the body of the entry. Tap **Back** and show the entry sitting in the list.

> Open the folder, and you're in your list. It's empty, so tap "Add note".
>
> Type a summary, write your entry, add a category if you like. There's no save button, and
> that's deliberate — Spectacled saves as you type and syncs in the background. Go back, and
> your first entry is there.
>
> It's also stored locally, so the app keeps working on a plane or a bad connection, and
> catches up when you're back.

*(74 words · ~32s)*

---

## 6. Close (3:35 – 3:52)

**On screen:** Same entry appearing on a second platform (desktop or web) beside the phone.
End card with the site and repo links.

> That's it. One account, one folder, one entry — and it's on your server, in an open format,
> readable by anything that speaks iCalendar.
>
> Journals, Notes, and Tasks each have their own app, and they all connect the same way.

*(40 words · ~17s)*

---

## Adapting this script for the other apps

The flow is identical in all three apps; only these lines change.

| Line in the script | Journals | Notes | Tasks |
|---|---|---|---|
| The add button | "Add journal" | "Add note" | "Add task" |
| "your first entry" | "your first journal entry" | "your first note" | "your first task" |
| Subtasks switch (§4) | keep | keep | **cut** — not shown in Tasks |
| Provider list (§2) | email category **absent** | email category **absent** | all providers shown |
| Tasks-only warning | never appears | never appears | shown on the email providers |

The last two rows are worth internalising before you record. The provider list is filtered
by what the app itself needs, so in Journals and Notes the tasks-only email providers
(Fastmail, Infomaniak, Mailfence, Posteo) are removed before the screen renders — which is
also why the "only supports tasks" warning is something you will only ever see while
recording Spectacled Tasks.

## Recording checklist

- Clean install, so the welcome sheet actually appears on launch.
- Throwaway account; blur credentials in post.
- Slow down every tap by about half a second — the sheets animate, and cuts land badly mid-animation.
- Let the autosave in §5 breathe: pause typing for a full second before navigating back.
- Capture at the device's native resolution; the provider badges are small and go mushy when upscaled.
- If the account you record with has two-factor authentication enabled, the main password
  will be rejected — use an app password for the capture, and consider saying so in the
  video description.
