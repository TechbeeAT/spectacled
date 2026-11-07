---
name: release
description: Prepare a Spectacled release for a version and channel the user names - set the version (with the channel suffix for test releases) and build number, write cumulative release notes since the last production release (or since the v1.0.81-beta baseline before the first one), and open a release PR. Tagging stays manual after the merge.
argument-hint: <version> <internal|alpha|beta|production> [build-number]
disable-model-invocation: true
---

# Prepare a release

The user decides the version and the channel. This skill does not suggest or change
them. It sets the version, writes the release notes, and opens a PR. **It never creates
or pushes a tag.** Pushing a `v*` tag starts the Google Play, App Store and GitHub release
workflows, so the user tags `main` themselves after merging.

Arguments: `$ARGUMENTS`

## 1. Parse and validate the input

- `version`: `MAJOR.MINOR.PATCH`, e.g. `1.0.78` or `1.1.0`. Any bump is allowed.
- `channel`: `internal`, `alpha`, `beta` or `production`.
- `build` (optional): a positive integer. It defaults to the current `appBuildNumber` + 1.

If the version or channel is missing or invalid, ask for it. Do not guess.

The tag is `v<version>-<channel>`, or `v<version>` for production. This tag convention is
what `.github/workflows/upload-aab-to-google.yml` maps to the Play tracks.

Check these and stop on any failure:

1. The working tree is clean. Run `git fetch origin main --tags`. If
   `git rev-parse --is-shallow-repository` prints `true`, run `git fetch --unshallow` too.
   Step 3 needs the full history.
2. The tag does not exist yet: `git ls-remote --tags origin "refs/tags/<tag>"` is empty.
3. The build number is higher than every build used so far, on any channel. Google Play
   rejects a versionCode it has already seen on any track. Compare it with `appBuildNumber`
   in `origin/main:gradle/libs.versions.toml` and with the patch numbers of the existing
   `v1.0.*` tags (on 1.0.x the patch number has been the build number).
4. `release-notes/<tag>/` does not exist on `origin/main`.

If the version is not higher than the numeric part of the current `appVersionString`
(ignore any `-<channel>` suffix), warn and ask once. Do not refuse: the user may have a
reason.

The **app version string** is `<version>-<channel>` for a test release (e.g.
`1.0.82-beta`) and plain `<version>` for production. Step 5 sets it.

## 2. Show the plan and wait for confirmation

Print the following, then wait for an explicit yes:

```
Version   1.0.81-beta (81)  ->  <app version string> (<build>)
Channel   <channel>    Tag after merge: <tag>
Notes     since <baseline tag>  (N commits)  [+ introduction, before the first production release]
Branch    release/<tag>
```

## 3. Find the changes since the baseline

The notes always cover everything since the **baseline**, whatever the channel. A test
release (internal, alpha, beta) therefore repeats what earlier test releases already
listed, and its notes grow until the next production release. This keeps testers on
Android and iOS reading the same cumulative list, and the production notes are simply the
final version of it.

The last production tag is the newest `v*` tag without a `-` suffix:
`git tag --merged origin/main --list 'v*' --sort=-creatordate | grep -E -e '^v[^-]*$' | head -n 1`.
The GitHub release workflow's fallback uses the same rule.

**The baseline:**
- **If there is a production tag**, the baseline is that tag. Notes are only the changes.
- **If there is no production tag yet**, the baseline is `v1.0.81-beta`, the start of the
  public beta. Notes are the introduction (below) **plus** the changes since
  `v1.0.81-beta`, so that what changed based on beta feedback is visible. This applies to
  every channel, including the first production release. If the release being prepared is
  `1.0.81` itself or older, write the introduction only.

**Changes:** read `git log --format='%h %s%n%b' <baseline>..origin/main`.
PR numbers appear as `(#123)` in commit subjects. For a commit whose subject is unclear,
read its PR: `gh pr view 123` if `gh` works, otherwise the GitHub MCP tools.
Leave out commits that users never notice:
- version bumps and `Release v…` commits
- Dependabot and other dependency-only updates, unless they fix something users see
- CI, workflows, build scripts and tests
- pure refactors

For a change that was reworked across several commits, describe only the end result.
If nothing user-visible is left, say so in the plan.

**Introduction** (only before the first production release), based on `README.md`
("Spectacled" heading, the app table, "Why" and "Features"):
- what Spectacled is
- the three apps: Journals, Notes, Tasks
- CalDAV against the user's own server, no account needed
- the platforms
- the main features

The same text goes to all three apps, so describe the family, not one app.

## 4. Write the release notes

Create these two files. Write them in English, for users, not developers. Describe what
changed for the user, not how it was implemented.

**`release-notes/<tag>/github.md`**: the body of the GitHub release (beta and production
only; the workflow ignores it for internal and alpha). Markdown, with the **complete**
list:

```markdown
## What's new
- …
## Improvements
- …
## Fixes
- …

**Full changelog**: https://github.com/TechbeeAT/spectacled/compare/<last production tag>...<tag>
```

Leave out empty sections. You may keep `(#123)` references at the end of bullets. Before
the first production release, start with the introduction, then add the sections with the
changes since `v1.0.81-beta` (e.g. under `## Changes during the beta`), and use
`v1.0.81-beta` as the start of the changelog link. If there are no changes yet, the
introduction alone is fine and the link is left out.

**`release-notes/<tag>/whatsnew/whatsnew-en-US`**: the store text, used on every channel
in the same way for both platforms:
- Google Play "What's new"
- TestFlight "What to Test" (alpha, beta, production)
- App Store "What's New in This Version" (production, from the second App Store version on;
  an app's first App Store version has no such field)

Plain text, no Markdown. Short `•` bullet lines are fine. It must be **at most 500
characters** (the Google Play limit): check with `wc -m`, since the upload workflow fails
above it. Because the notes are cumulative, this is a summary of the highlights since the
baseline, not the full list: pick the changes users notice most, merge related ones into
one line. Before the first production release, shorten the introduction to a sentence or
two so the beta changes still fit. If anything is left out of a beta or production text, end
with "Full release notes on GitHub." Internal and alpha releases get no GitHub release,
so leave that line out there.

The App Store "Werbetext" (Promotional Text, 170 characters) is marketing copy that is not
tied to a version, and the user edits it by hand. For a production release, you may
suggest a new one in the PR description, but do not write a file for it.

Show both drafts to the user and apply their edits before continuing.

## 5. Set the version

Only these two files hold the version:

- `gradle/libs.versions.toml`: `appVersionString = "<app version string>"` (with the
  `-<channel>` suffix for internal, alpha and beta; without it for production) and
  `appBuildNumber = "<build>"`. Change `appVersionCodename` only if the user asks.
- `iosApp/Version.xcconfig`: `MARKETING_VERSION=<version>` and
  `CURRENT_PROJECT_VERSION=<build>`. All three iOS apps include this file.
  `MARKETING_VERSION` stays **numeric only**, without the channel: App Store Connect
  rejects anything but `MAJOR.MINOR.PATCH`. The iOS apps still show the full version
  string in the About screen, which reads it from `BuildKonfig`.

Do not touch anything else. Android (`versionName`), the About screen (`BuildKonfig`) and
desktop read the version from the toml file; the desktop builds strip the suffix
themselves, because jpackage accepts only numeric versions.

## 6. Commit, push and open the PR

1. Create the branch: `git switch -c release/<tag> origin/main`. If the session prescribes
   a branch (cloud sessions do), use that branch instead.
2. Make one commit: `Release <tag>`, containing the two version files and
   `release-notes/<tag>/`.
3. Push with `git push -u origin <branch>`.
4. Open a PR against `main` titled `Release <tag>`. The body contains:
   - the version/build change and the channel
   - which baseline tag the notes count from (and whether they include the introduction)
   - the store text, and the GitHub notes in a `<details>` block
   - the commands to run after merging:

     ```bash
     git fetch origin main
     git tag -a <tag> origin/main -m "<tag>"
     git push origin <tag>
     ```

   Tag `origin/main` **after** the merge, not the release branch. The merge (squash or
   merge commit) creates a new commit, and the workflows read the release notes from the
   tagged commit.

Finish by sending the user the PR link and the tag command. Do not tag.
