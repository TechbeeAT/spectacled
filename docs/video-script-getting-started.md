# Getting Started with Spectacled — voice-over script

**Video:** "Getting started with Spectacled" (onboarding walkthrough)
**Runtime target:** ~2:45
**Example app used on screen:** Spectacled Notes
**Example provider:** Murena Workspace
**Voice:** calm, unhurried, second person ("you"). Read at ~140 wpm — every timing below assumes that.

Word counts per section are given so the edit can be cut to picture. Every UI label
quoted in the voice-over matches the strings in the app exactly.

---

## 0. Cold open (0:00 – 0:12)

**On screen:** Spectacled logo animation, then a fast montage — a note being typed,
a folder list, the same entry appearing on a second device.

> Spectacled keeps your notes, journals, and tasks on a server you choose — not on ours.
> Let's connect one and write the first entry. It takes about two minutes.

*(28 words · ~12s)*

---

## 1. Opening the app for the first time (0:12 – 0:33)

**On screen:** Launch Spectacled Notes on a clean install. The welcome sheet slides
up on its own. Hold on the headline and the two option cards.

> Open Spectacled for the first time and it asks for exactly one thing: where your data
> should live.
>
> There's no Spectacled account, and nothing to sign up for here. Your entries are stored
> on a CalDAV server — the same open standard your calendar app already speaks — and
> Spectacled is only the client that reads and writes them.
>
> So the welcome screen offers two paths. Option one, if you already have a CalDAV account.
> Option two, if you need one.

*(78 words · ~34s — trim the second paragraph to one sentence if the section runs long)*

---

## 2. Choosing a provider (0:33 – 1:12)

**On screen:** Tap the **"Need an account?"** card. The pager slides to the provider
list. Scroll slowly past the category headings, then settle on the Murena Workspace chip
and let the badges read.

> Tap "Need an account?" and Spectacled shows a short list of providers it works well with,
> grouped by type: Nextcloud providers, self-hosted solutions, and email providers that
> support CalDAV.
>
> These are recommendations, not requirements — Spectacled is provider-independent, and
> any CalDAV server will do. Some of these links do earn the project a referral commission,
> which is stated right on the screen and helps fund development.
>
> For this video we'll use Murena Workspace: a de-Googled, privacy-focused workspace from
> France, built on Nextcloud, with a free tier to get started. The badges tell you what
> matters at a glance — where it's hosted, what it's built on, and that there's a free plan.
>
> Tapping the card opens Murena in your browser, where you create the account. Sign up,
> then come back to Spectacled.

*(140 words · ~60s — see the note below on covering the sign-up)*

> **Editing note:** the Murena sign-up itself happens outside the app. Either cut away to a
> 6–8 second sped-up browser capture under the last paragraph, or hard-cut back to the app
> with a "…once you've signed up" title card. Don't film the real sign-up form with a real
> address.

---

## 3. Adding the account (1:12 – 1:52)

**On screen:** Tap **Back**, then the **"Have an account?"** card. On the form, open the
**⋮** menu at the end of the Server field and pick *Murena Workspace* — the CalDAV URL
fills itself in. Type the username, then the password. Tap **Add account** and hold on the
progress indicator until the account appears in the list.

> Now back in the app, take option one — "Have an account?" — and fill in three fields.
>
> Server, username, password. And you rarely have to type the server by hand: the menu at
> the end of the field has the CalDAV addresses for the providers Spectacled knows, so pick
> Murena and the URL fills itself in. If you leave the server blank entirely, Spectacled will
> infer it from the domain in your username.
>
> Tap "Add account". Spectacled connects, checks what the server can do, and lists what it
> finds. If your provider supports it, use an app-specific password here rather than your
> main one.

*(112 words · ~48s)*

> **Editing note:** blur or replace the username and password on screen, and use a throwaway
> account for the capture. The password field's reveal toggle should stay untouched.

---

## 4. Creating a folder (1:52 – 2:20)

**On screen:** On the Accounts screen, open the **⋮** menu on the account row and choose
**Create folder**. Type a name, add a short description, pick a colour from the selector.
Tap **Create folder** and watch the new card drop into the list.

> A fresh account usually has nowhere to put things yet, so let's make a folder.
>
> Open the menu on the account and choose "Create folder". Give it a name, a description if
> you want one, and a colour — the colour follows the folder everywhere in the app, which is
> what makes a long list scannable later.
>
> There's one switch worth knowing about: "Include support for subtasks". Leave it on and
> this folder can hold tasks alongside your notes, so an entry can have checkable subtasks
> under it.
>
> Tap "Create folder", and Spectacled creates it on the server — not just on this device.

*(107 words · ~46s)*

> **Editing note:** the subtasks switch only appears in Spectacled Notes and Journals when
> creating a *new* folder. If you record this section in Spectacled Tasks, cut that paragraph.

---

## 5. Adding an entry (2:20 – 2:45)

**On screen:** Tap the new folder card to open it. Empty list. Tap the **Add note** button.
On the details screen, type a summary, then a couple of lines of description — pause so the
autosave lands. Tap **Back** and show the entry sitting in the list.

> Open the folder, and you're in your list. It's empty, so tap "Add note".
>
> Type a summary, write your entry, add a category if you like. There's no save button, and
> that's deliberate — Spectacled saves as you type and syncs in the background. Go back, and
> your first entry is there.
>
> It's also stored locally, so the app keeps working on a plane or a bad connection, and
> catches up when you're back.

*(83 words · ~36s)*

---

## 6. Close (2:45 – 3:00)

**On screen:** Same entry appearing on a second platform (desktop or web) beside the phone.
End card with the site and repo links.

> That's it. One account, one folder, one entry — and it's on your server, in an open format,
> readable by anything that speaks iCalendar.
>
> Journals, Notes, and Tasks each have their own app, and they all connect the same way.

*(48 words · ~20s)*

---

## Adapting this script for the other apps

The flow is identical in all three apps; only these lines change.

| Line in the script | Journals | Notes | Tasks |
|---|---|---|---|
| The add button | "Add journal" | "Add note" | "Add task" |
| "your first entry" | "your first journal entry" | "your first note" | "your first task" |
| Subtasks switch (§4) | keep | keep | **cut** — not shown in Tasks |
| Provider list (§2) | Murena is listed | Murena is listed | Murena is listed, plus email providers |

One caveat if you ever swap the example provider: the email providers in the list
(Fastmail, Infomaniak, Mailfence, Posteo) support tasks only, and the app flags them with a
warning in Journals and Notes. Murena has no such caveat, which is part of why it's a good
choice for this video.

## Recording checklist

- Clean install, so the welcome sheet actually appears on launch.
- Throwaway account; blur credentials in post.
- Slow down every tap by about half a second — the sheets animate, and cuts land badly mid-animation.
- Let the autosave in §5 breathe: pause typing for a full second before navigating back.
- Capture at the device's native resolution; the provider badges are small and go mushy when upscaled.
