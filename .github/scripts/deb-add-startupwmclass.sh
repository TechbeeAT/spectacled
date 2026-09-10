#!/usr/bin/env bash
#
# WORKAROUND. Adds StartupWMClass to the .desktop entry inside a .deb built by
# jpackage. Revisit whenever the toolchain gains a supported way to do this.
#
# GNOME identifies a window by matching its WM_CLASS against the .desktop files
# it knows about. Java derives WM_CLASS from the main class name -- XToolkit
# takes the class name of the bottom stack frame and replaces '.' with '-' --
# so it reads "at-techbee-spectacled-journals-MainKt" and matches nothing. The
# dock then shows a generic gear icon and that string instead of the app icon
# and name. Setting the window icon from the app does not help: GNOME takes
# both from the .desktop match and ignores _NET_WM_ICON for unmatched windows.
#
# The fix is one line in the .desktop file, StartupWMClass=<that same string>.
# jpackage has no option for it (--linux-shortcut writes the file, nothing
# customises it), and Compose does not expose jpackage's --resource-dir
# override -- the directory it passes is cleared at the start of the packaging
# task, so the entry cannot be staged from the build either. That leaves
# editing the finished package.
#
# TODO: delete this script and the release-workflow step that calls it once
# jpackage can write StartupWMClass itself, or Compose exposes --resource-dir,
# and set the value through the build instead.
#
# Usage: deb-add-startupwmclass.sh <package.deb>

set -euo pipefail

deb=$1

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

dpkg-deb --raw-extract "$deb" "$work"

# The runtime image carries directories called *.desktop (runtime/legal/java.desktop),
# hence -type f and skipping it.
desktop=$(find "$work" -type f -name '*.desktop' -not -path '*/runtime/*')
if [ "$(printf '%s' "$desktop" | grep -c '^')" != 1 ]; then
    echo "$deb: expected exactly one .desktop file, found: ${desktop:-none}" >&2
    echo "  (is linux.shortcut set in the Gradle configuration?)" >&2
    exit 1
fi

cfg=$(find "$work" -type f -name '*.cfg' -path '*/lib/app/*' | head -1)
main_class=$(sed -n 's/^app\.mainclass=//p' "$cfg" | head -1)
if [ -z "$main_class" ]; then
    echo "$cfg: no app.mainclass, cannot derive WM_CLASS" >&2
    exit 1
fi

# Exactly what the JDK does: sun.awt.X11.XToolkit.getCorrectXIDString is
# String.replace('.', '-') applied to the main class name.
wm_class=${main_class//./-}

if ! grep -q '^StartupWMClass=' "$desktop"; then
    printf 'StartupWMClass=%s\n' "$wm_class" >> "$desktop"
fi

dpkg-deb --root-owner-group --build "$work" "$deb" >/dev/null

echo "$deb: StartupWMClass=$wm_class"
