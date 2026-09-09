#!/usr/bin/env bash
#
# Adds AppStream metadata to a .deb built by jpackage.
#
# GNOME Software and Ubuntu App Center read the license, the developer website
# and the "last updated" date from an AppStream metainfo file below
# /usr/share/metainfo; without one they show "unknown" and a dead website link.
# jpackage has no option for any of it and only writes files below
# /opt/<package>, so the finished package is unpacked, the metainfo file and a
# Homepage: control field are added, and it is packed up again. jpackage does
# not generate a md5sums file, so there is none to refresh.
#
# Usage: deb-add-appstream.sh <package.deb> <metainfo.xml>

set -euo pipefail

deb=$1
metainfo=$2

component_id=$(sed -n 's|.*<id>\(.*\)</id>.*|\1|p' "$metainfo" | head -1)
homepage=$(sed -n 's|.*<url type="homepage">\(.*\)</url>.*|\1|p' "$metainfo" | head -1)

[ -n "$component_id" ] || { echo "$metainfo: no <id>" >&2; exit 1; }
[ -n "$homepage" ] || { echo "$metainfo: no <url type=\"homepage\">" >&2; exit 1; }
if grep -qE '^[[:space:]]*<releases>' "$metainfo"; then
    echo "$metainfo: already carries a <releases> block, which this script adds" >&2
    exit 1
fi

# Taken from the package rather than from the tag, so that a manually dispatched
# run gets the same version the installer itself was built with.
version=$(dpkg-deb --field "$deb" Version)
today=$(date --utc +%Y-%m-%d)

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

dpkg-deb --raw-extract "$deb" "$work"

# The <releases> entry is what turns "Last Updated: unknown" into a real date,
# so it is generated per build instead of being checked in and going stale.
target=$work/usr/share/metainfo/$component_id.metainfo.xml
mkdir -p "$(dirname "$target")"
sed "s|</component>|  <releases>\n    <release version=\"$version\" date=\"$today\"/>\n  </releases>\n</component>|" \
    "$metainfo" > "$target"
chmod 644 "$target"

grep -q '^Homepage:' "$work/DEBIAN/control" ||
    sed -i "s|^Description:|Homepage: $homepage\nDescription:|" "$work/DEBIAN/control"

dpkg-deb --root-owner-group --build "$work" "$deb" >/dev/null

echo "$deb: added $component_id.metainfo.xml (version $version, released $today)"
