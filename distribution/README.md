# Play Store release notes

One "What's new" text per app, uploaded to Google Play by
`.github/workflows/upload-aab-to-google.yml` alongside the app bundle:

```
distribution/<app>/whatsnew/whatsnew-<locale>
```

`<app>` is `journals`, `notes` or `tasks`; `<locale>` is a BCP 47 tag. Only `en-US`
exists today, because the apps themselves ship no localized resources yet. Adding a
language is just another file in the same directory — the workflow uploads whatever is
there.

## Rules

- **Rewrite these before every tag.** They are not generated. If you leave a file
  untouched, Play republishes the previous release's notes without warning.
- **500 characters maximum per file.** That is a hard Google Play limit; the release
  workflow checks it up front and fails the build rather than letting the upload be
  rejected halfway through.
- The files must not be empty — the same check enforces that.
- Testers and users read this text verbatim, so write it for them, not as a changelog.
