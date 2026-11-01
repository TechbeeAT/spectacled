---
name: release
description: Prepare a Spectacled release for a version and channel the user names - set the version and build number, write channel-aware release notes, and open a release PR. Tagging stays manual after the merge.
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

If the version is not higher than the current `appVersionString`, warn and ask once. Do
not refuse: the user may have a reason.

## 2. Show the plan and wait for confirmation

Print the following, then wait for an explicit yes:

```
Version   1.0.77 (77)  ->  <version> (<build>)
Channel   <channel>    Tag after merge: <tag>
Notes     since <previous tag on this channel>  (N commits)  |  first release on this channel
Branch    release/<tag>
```

## 3. Find the changes since the last release on this channel

Only consider tags of the same channel:

| Channel    | Matches                        |
|------------|--------------------------------|
| internal   | `v*-internal`                  |
| alpha      | `v*-alpha`                     |
| beta       | `v*-beta`                      |
| production | `v*` with no `-` suffix        |

Pick the previous tag with
`git tag --merged origin/main --list 'v*' --sort=-creatordate`, filtered with the table
above, first result. Use `grep -E -e '<pattern>'`: the patterns start with `-`, which grep
otherwise reads as an option. The release workflow's fallback uses the same rule.

**If a previous tag exists:** read `git log --format='%h %s%n%b' <previous>..origin/main`.
PR numbers appear as `(#123)` in commit subjects. For a commit whose subject is unclear,
read its PR: `gh pr view 123` if `gh` works, otherwise the GitHub MCP tools.
Leave out commits that users never notice:
- version bumps and `Release v…` commits
- Dependabot and other dependency-only updates, unless they fix something users see
- CI, workflows, build scripts and tests
- pure refactors

Going by commit range means a production release automatically includes everything its
alphas and betas already shipped.

**If there is no previous tag on this channel (the first release there):** do not list
changes. Write an introduction instead, based on `README.md` ("Spectacled" heading, the
app table, "Why" and "Features"):
- what Spectacled is
- the three apps: Journals, Notes, Tasks
- CalDAV against the user's own server, no account needed
- the platforms
- the main features

The same text goes to all three apps on Google Play, so describe the family, not one app.

## 4. Write the release notes

Create these files. Write them in English, for users, not developers. Describe what changed
for the user, not how it was implemented.

**`release-notes/<tag>/github.md`**: the body of the GitHub release (beta and production
only; the workflow ignores it for internal and alpha). Markdown:

```markdown
## What's new
- …
## Improvements
- …
## Fixes
- …

**Full changelog**: https://github.com/TechbeeAT/spectacled/compare/<previous>...<tag>
```

Leave out empty sections. You may keep `(#123)` references at the end of bullets. For a
first release, use the introduction instead of the sections, and leave out the changelog
link.

**`release-notes/<tag>/play/whatsnew-en-US`**: the Google Play "What's new" text (every
channel). Plain text, no Markdown. Short `•` bullet lines are fine. It must be **at most
500 characters**: check with `wc -m`. The upload workflow fails the job above that limit.
Include only the most important points.

**`release-notes/<tag>/testflight/whattotest-en-US`**: the TestFlight "What to Test" text
(alpha, beta and production; the iOS workflow does not run for internal). Plain text, no
Markdown, **at most 4000 characters**. It is written for testers: what changed, and what
they should try out or watch for, including risky areas such as sync, migrations and
notifications. For a first release on a channel, use the introduction plus a short note
on what to try first. If the file is missing, the workflow falls back to the Play text.

**`release-notes/<tag>/appstore/whatsnew-en-US`** (production only): the App Store
"What's New in This Version" ("Neue Funktionen in dieser Version"), shown to everyone in
the App Store version history. Plain text, no Markdown, **at most 4000 characters**. It is
longer than the Play text but just as user-facing, with no testing instructions. If the
file is missing, the workflow falls back to the Play text. An app's first App Store version
has no such field, so skip this file when no production tag exists yet.

The App Store "Werbetext" (Promotional Text, 170 characters) is marketing copy that is not
tied to a version, and the user edits it by hand. For a production release, you may
suggest a new one in the PR description, but do not write a file for it.

Show all drafts to the user and apply their edits before continuing.

## 5. Set the version

Only these two files hold the version:

- `gradle/libs.versions.toml`: `appVersionString = "<version>"` and
  `appBuildNumber = "<build>"`. Change `appVersionCodename` only if the user asks.
- `iosApp/Version.xcconfig`: `MARKETING_VERSION=<version>` and
  `CURRENT_PROJECT_VERSION=<build>`. All three iOS apps include this file.

Do not touch anything else. Android, desktop and `BuildKonfig` read the version from the
toml file.

## 6. Commit, push and open the PR

1. Create the branch: `git switch -c release/<tag> origin/main`. If the session prescribes
   a branch (cloud sessions do), use that branch instead.
2. Make one commit: `Release <tag>`, containing the two version files and
   `release-notes/<tag>/`.
3. Push with `git push -u origin <branch>`.
4. Open a PR against `main` titled `Release <tag>`. The body contains:
   - the version/build change and the channel
   - which previous tag the notes are based on
   - the Play text, and the GitHub, TestFlight and App Store notes in `<details>` blocks
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
