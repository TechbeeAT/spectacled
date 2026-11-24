#!/usr/bin/env python3
"""Set release texts in App Store Connect for .github/workflows/upload-ios-to-appstore.yml.

  what-to-test  TestFlight "What to Test" of an uploaded build. App Store Connect lists
                a build a few minutes after the upload and accepts its test notes once
                processing has finished, so this waits for the build to become VALID.
  whats-new     App Store "What's New in This Version" ("Neue Funktionen in dieser
                Version"). Finds or creates the App Store version and sets the text of
                an existing localization. Skipped for an app's first version, which
                has no such field.

Environment: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (the .p8 file).
Needs PyJWT with the crypto extra (pip install "pyjwt[crypto]").
"""
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

import jwt

API = "https://api.appstoreconnect.apple.com/v1"
MAX_LENGTH = 4000  # limit of both whatToTest and whatsNew

# appStoreState values meaning a version reached the App Store at some point.
RELEASED_STATES = {"READY_FOR_SALE", "REPLACED_WITH_NEW_VERSION",
                   "REMOVED_FROM_SALE", "DEVELOPER_REMOVED_FROM_SALE"}
# appStoreState values in which the version's metadata can still be edited.
EDITABLE_STATES = {"PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED",
                   "METADATA_REJECTED", "INVALID_BINARY"}


def fail(message):
    print(f"::error::{message}")
    sys.exit(1)


class AppStoreConnect:
    def __init__(self, key_id, issuer_id, private_key):
        self.key_id = key_id
        self.issuer_id = issuer_id
        self.private_key = private_key

    def _token(self):
        # Tokens may live at most 20 minutes; a fresh one per request is simplest
        # and keeps a long wait from outliving its token.
        now = int(time.time())
        return jwt.encode(
            {"iss": self.issuer_id, "iat": now, "exp": now + 600, "aud": "appstoreconnect-v1"},
            self.private_key,
            algorithm="ES256",
            headers={"kid": self.key_id, "typ": "JWT"},
        )

    def call(self, method, path, params=None, body=None):
        url = API + path + ("?" + urllib.parse.urlencode(params) if params else "")
        request = urllib.request.Request(
            url,
            data=json.dumps(body).encode() if body is not None else None,
            method=method,
            headers={"Authorization": f"Bearer {self._token()}", "Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                raw = response.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as error:
            fail(f"{method} {path} -> HTTP {error.code}: {error.read().decode(errors='replace')[:2000]}")

    def app_id(self, bundle_id):
        apps = self.call("GET", "/apps", {"filter[bundleId]": bundle_id, "fields[apps]": "bundleId"})["data"]
        # Do not rely on the filter alone being exact; pick the matching app.
        app = next((a for a in apps if a["attributes"]["bundleId"] == bundle_id), None)
        if app is None:
            fail(f"No app with bundle ID {bundle_id} in App Store Connect.")
        return app["id"]


def read_notes(path):
    with open(path, encoding="utf-8") as f:
        text = f.read().strip()
    if not text:
        fail(f"{path} is empty.")
    if len(text) > MAX_LENGTH:
        fail(f"{path} has {len(text)} characters; App Store Connect allows {MAX_LENGTH}.")
    return text


def wait_for_build(asc, app_id, version, build, timeout_minutes, poll_seconds):
    deadline = time.monotonic() + timeout_minutes * 60
    while True:
        builds = asc.call("GET", "/builds", {
            "filter[app]": app_id,
            "filter[version]": build,
            "filter[preReleaseVersion.version]": version,
            "fields[builds]": "version,processingState",
            "limit": "1",
        })["data"]
        state = builds[0]["attributes"]["processingState"] if builds else "NOT LISTED YET"
        print(f"Build {version} ({build}): {state}")
        if state == "VALID":
            return builds[0]["id"]
        if state in ("FAILED", "INVALID"):
            fail(f"Build {version} ({build}) is {state} in App Store Connect.")
        if time.monotonic() > deadline:
            fail(f"Build {version} ({build}) not processed after {timeout_minutes} minutes.")
        time.sleep(poll_seconds)


def what_to_test(asc, args):
    text = read_notes(args.notes_file)
    build_id = wait_for_build(asc, asc.app_id(args.bundle_id), args.version, args.build,
                              args.timeout_minutes, args.poll_seconds)

    localizations = asc.call("GET", f"/builds/{build_id}/betaBuildLocalizations")["data"]
    existing = next((l for l in localizations if l["attributes"]["locale"] == args.locale), None)
    if existing:
        asc.call("PATCH", f"/betaBuildLocalizations/{existing['id']}", body={"data": {
            "type": "betaBuildLocalizations",
            "id": existing["id"],
            "attributes": {"whatToTest": text},
        }})
    else:
        asc.call("POST", "/betaBuildLocalizations", body={"data": {
            "type": "betaBuildLocalizations",
            "attributes": {"locale": args.locale, "whatToTest": text},
            "relationships": {"build": {"data": {"type": "builds", "id": build_id}}},
        }})
    print(f"Set What to Test ({args.locale}, {len(text)} characters) for "
          f"{args.bundle_id} {args.version} ({args.build}).")


def whats_new(asc, args):
    text = read_notes(args.notes_file)
    app_id = asc.app_id(args.bundle_id)

    versions = asc.call("GET", f"/apps/{app_id}/appStoreVersions", {
        "filter[platform]": "IOS",
        "fields[appStoreVersions]": "versionString,appStoreState",
        "limit": "200",
    })["data"]
    others = [v for v in versions if v["attributes"]["versionString"] != args.version]

    if not any(v["attributes"]["appStoreState"] in RELEASED_STATES for v in others):
        print(f"::notice::{args.bundle_id} has no released App Store version yet. A first "
              f"version has no What's New field; the description introduces the app instead.")
        return

    version = next((v for v in versions if v["attributes"]["versionString"] == args.version), None)
    if version is None:
        blocking = next((v for v in others if v["attributes"]["appStoreState"] in EDITABLE_STATES), None)
        if blocking:
            fail(f"{args.bundle_id}: version {blocking['attributes']['versionString']} is still "
                 f"{blocking['attributes']['appStoreState']}, so App Store Connect cannot create "
                 f"{args.version}. Delete or rename that version, then re-run this job.")
        version = asc.call("POST", "/appStoreVersions", body={"data": {
            "type": "appStoreVersions",
            "attributes": {"platform": "IOS", "versionString": args.version},
            "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
        }})["data"]
        print(f"Created App Store version {args.version}.")
    elif version["attributes"]["appStoreState"] not in EDITABLE_STATES:
        print(f"::warning::{args.bundle_id} {args.version} is "
              f"{version['attributes']['appStoreState']}; its What's New can no longer be changed.")
        return

    localizations = asc.call("GET", f"/appStoreVersions/{version['id']}/appStoreVersionLocalizations",
                             {"fields[appStoreVersionLocalizations]": "locale"})["data"]
    existing = next((l for l in localizations if l["attributes"]["locale"] == args.locale), None)
    if existing is None:
        # Creating it would add a new language to the App Store listing.
        fail(f"{args.bundle_id} {args.version} has no {args.locale} localization "
             f"(has: {', '.join(l['attributes']['locale'] for l in localizations) or 'none'}).")
    asc.call("PATCH", f"/appStoreVersionLocalizations/{existing['id']}", body={"data": {
        "type": "appStoreVersionLocalizations",
        "id": existing["id"],
        "attributes": {"whatsNew": text},
    }})
    print(f"Set What's New ({args.locale}, {len(text)} characters) for {args.bundle_id} {args.version}.")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("what-to-test", "whats-new"):
        command = commands.add_parser(name)
        command.add_argument("--bundle-id", required=True)
        command.add_argument("--version", required=True, help="MARKETING_VERSION, e.g. 1.0.78")
        command.add_argument("--notes-file", required=True)
        command.add_argument("--locale", default="en-US")
        if name == "what-to-test":
            command.add_argument("--build", required=True, help="CURRENT_PROJECT_VERSION, e.g. 78")
            command.add_argument("--timeout-minutes", type=int, default=60)
            command.add_argument("--poll-seconds", type=int, default=60)
    args = parser.parse_args()

    with open(os.environ["ASC_KEY_PATH"], encoding="utf-8") as f:
        asc = AppStoreConnect(os.environ["ASC_KEY_ID"], os.environ["ASC_ISSUER_ID"], f.read())
    {"what-to-test": what_to_test, "whats-new": whats_new}[args.command](asc, args)


if __name__ == "__main__":
    main()
