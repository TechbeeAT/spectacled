#!/usr/bin/env python3
"""Set the TestFlight "What to Test" text of an uploaded build.

Used by .github/workflows/upload-ios-to-appstore.yml after the upload. App Store
Connect only lists a build a few minutes after the upload and accepts its test
notes once processing has finished, so this waits for the build to become VALID.

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
MAX_LENGTH = 4000  # App Store Connect limit for whatToTest


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


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--bundle-id", required=True)
    parser.add_argument("--version", required=True, help="MARKETING_VERSION, e.g. 1.0.78")
    parser.add_argument("--build", required=True, help="CURRENT_PROJECT_VERSION, e.g. 78")
    parser.add_argument("--notes-file", required=True)
    parser.add_argument("--locale", default="en-US")
    parser.add_argument("--timeout-minutes", type=int, default=60)
    parser.add_argument("--poll-seconds", type=int, default=60)
    args = parser.parse_args()

    with open(args.notes_file, encoding="utf-8") as f:
        what_to_test = f.read().strip()
    if not what_to_test:
        fail(f"{args.notes_file} is empty.")
    if len(what_to_test) > MAX_LENGTH:
        fail(f"{args.notes_file} has {len(what_to_test)} characters; TestFlight allows {MAX_LENGTH}.")

    with open(os.environ["ASC_KEY_PATH"], encoding="utf-8") as f:
        asc = AppStoreConnect(os.environ["ASC_KEY_ID"], os.environ["ASC_ISSUER_ID"], f.read())

    apps = asc.call("GET", "/apps", {"filter[bundleId]": args.bundle_id, "fields[apps]": "bundleId"})["data"]
    # Do not rely on the filter alone being exact; pick the matching app.
    app = next((a for a in apps if a["attributes"]["bundleId"] == args.bundle_id), None)
    if app is None:
        fail(f"No app with bundle ID {args.bundle_id} in App Store Connect.")

    build_id = wait_for_build(asc, app["id"], args.version, args.build, args.timeout_minutes, args.poll_seconds)

    localizations = asc.call("GET", f"/builds/{build_id}/betaBuildLocalizations")["data"]
    existing = next((l for l in localizations if l["attributes"]["locale"] == args.locale), None)
    if existing:
        asc.call("PATCH", f"/betaBuildLocalizations/{existing['id']}", body={"data": {
            "type": "betaBuildLocalizations",
            "id": existing["id"],
            "attributes": {"whatToTest": what_to_test},
        }})
    else:
        asc.call("POST", "/betaBuildLocalizations", body={"data": {
            "type": "betaBuildLocalizations",
            "attributes": {"locale": args.locale, "whatToTest": what_to_test},
            "relationships": {"build": {"data": {"type": "builds", "id": build_id}}},
        }})
    print(f"Set What to Test ({args.locale}, {len(what_to_test)} characters) for "
          f"{args.bundle_id} {args.version} ({args.build}).")


if __name__ == "__main__":
    main()
