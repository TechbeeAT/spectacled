#!/usr/bin/env bash
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}===================================================${NC}"
echo -e "${GREEN}       KMP Project Template Renamer Script${NC}"
echo -e "${GREEN}===================================================${NC}"
echo ""
echo -e "${YELLOW}Make sure you have copied this entire project folder to${NC}"
echo -e "${YELLOW}your new location BEFORE running this script!${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── Auto-detect old project name ──
OLD_PROJECT_NAME="Unknown"
if [ -f "settings.gradle.kts" ]; then
    OLD_PROJECT_NAME=$(grep -oP 'rootProject\.name\s*=\s*"\K[^"]+' settings.gradle.kts || echo "Unknown")
fi

# ── Auto-detect old package name ──
OLD_PACKAGE_NAME="Unknown"
if [ -f "androidApp/build.gradle.kts" ]; then
    OLD_PACKAGE_NAME=$(grep -oP 'namespace\s*=\s*"\K[^"]+' androidApp/build.gradle.kts | head -1 || echo "Unknown")
fi

echo -e "${CYAN}Detected Existing Project Name:  ${OLD_PROJECT_NAME}${NC}"
echo -e "${CYAN}Detected Existing Package Name:  ${OLD_PACKAGE_NAME}${NC}"
echo ""

if [ "$OLD_PROJECT_NAME" = "Unknown" ] || [ "$OLD_PACKAGE_NAME" = "Unknown" ]; then
    echo -e "${RED}Error: Could not detect old project or package name.${NC}"
    exit 1
fi

# ── Read new names from user ──
read -rp "Enter NEW Project Name (e.g. MyNewApp)      : " NEW_PROJECT_NAME
read -rp "Enter NEW Package Name (e.g. com.sanket.tool): " NEW_PACKAGE_NAME

if [ -z "$NEW_PROJECT_NAME" ] || [ -z "$NEW_PACKAGE_NAME" ]; then
    echo -e "${RED}Error: New project name and package name cannot be empty.${NC}"
    exit 1
fi

OLD_NAME_LOWER=$(echo "$OLD_PROJECT_NAME" | tr '[:upper:]' '[:lower:]')
NEW_NAME_LOWER=$(echo "$NEW_PROJECT_NAME" | tr '[:upper:]' '[:lower:]')

OLD_PACKAGE_PATH=$(echo "$OLD_PACKAGE_NAME" | tr '.' '/')
NEW_PACKAGE_PATH=$(echo "$NEW_PACKAGE_NAME" | tr '.' '/')

echo ""
echo -e "${GREEN}Renaming project to '${NEW_PROJECT_NAME}'...${NC}"
echo -e "${GREEN}Renaming package to '${NEW_PACKAGE_NAME}'...${NC}"

# ── [1/3] Replace text inside files ──
echo ""
echo -e "${YELLOW}[1/3] Modifying file contents...${NC}"

ALLOWED_EXTENSIONS="kt|kts|xml|properties|md|xcconfig|pbxproj|java|yaml|yml|json|toml|gradle|html|swift|plist"

find . -type f \
    ! -path './.git/*' \
    ! -path './.gradle/*' \
    ! -path './.idea/*' \
    ! -path '*/build/*' \
    ! -path './.kotlin/*' \
    ! -name 'rename_project.*' \
    -regextype posix-extended \
    -regex ".*\.(${ALLOWED_EXTENSIONS})" \
    -print0 | while IFS= read -r -d '' file; do
    if grep -q "$OLD_PACKAGE_NAME\|$OLD_PROJECT_NAME\|$OLD_NAME_LOWER" "$file" 2>/dev/null; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS sed requires empty string for -i
            sed -i '' \
                -e "s|${OLD_PACKAGE_NAME}|${NEW_PACKAGE_NAME}|g" \
                -e "s|${OLD_PACKAGE_PATH}|${NEW_PACKAGE_PATH}|g" \
                -e "s|${OLD_PROJECT_NAME}|${NEW_PROJECT_NAME}|g" \
                -e "s|${OLD_NAME_LOWER}|${NEW_NAME_LOWER}|g" \
                "$file"
        else
            # Linux sed
            sed -i \
                -e "s|${OLD_PACKAGE_NAME}|${NEW_PACKAGE_NAME}|g" \
                -e "s|${OLD_PACKAGE_PATH}|${NEW_PACKAGE_PATH}|g" \
                -e "s|${OLD_PROJECT_NAME}|${NEW_PROJECT_NAME}|g" \
                -e "s|${OLD_NAME_LOWER}|${NEW_NAME_LOWER}|g" \
                "$file"
        fi
    fi
done

# ── [2/3] Move source files to new package structure ──
echo ""
echo -e "${YELLOW}[2/3] Moving source code to new package structure...${NC}"

SOURCE_DIRS=(
    "androidApp/src/main/kotlin"
    "androidApp/src/test/kotlin"
    "androidApp/src/androidTest/kotlin"
    "shared/src/commonMain/kotlin"
    "shared/src/commonTest/kotlin"
    "shared/src/androidMain/kotlin"
    "shared/src/iosMain/kotlin"
    "shared/src/jvmMain/kotlin"
    "shared/src/jsMain/kotlin"
    "shared/src/wasmJsMain/kotlin"
    "server/src/main/kotlin"
    "server/src/test/kotlin"
    "composeApp/src/commonMain/kotlin"
    "composeApp/src/commonTest/kotlin"
    "composeApp/src/androidMain/kotlin"
    "composeApp/src/desktopMain/kotlin"
    "composeApp/src/iosMain/kotlin"
    "composeApp/src/jvmMain/kotlin"
    "composeApp/src/wasmJsMain/kotlin"
    "composeApp/src/jsMain/kotlin"
    "composeApp/src/webMain/kotlin"
)

for src in "${SOURCE_DIRS[@]}"; do
    OLD_PATH="$src/$OLD_PACKAGE_PATH"
    NEW_PATH="$src/$NEW_PACKAGE_PATH"

    if [ -d "$OLD_PATH" ]; then
        mkdir -p "$NEW_PATH"
        # Move all contents
        find "$OLD_PATH" -maxdepth 1 -mindepth 1 -exec mv -f {} "$NEW_PATH/" \;
        echo -e "${GREEN}  Moved: $src${NC}"

        # Clean up empty old directories
        CLEAN_PATH="$OLD_PATH"
        while [ -d "$CLEAN_PATH" ] && [ -z "$(ls -A "$CLEAN_PATH" 2>/dev/null)" ]; do
            rmdir "$CLEAN_PATH"
            CLEAN_PATH=$(dirname "$CLEAN_PATH")
        done
    fi
done

# ── [3/3] Rename directories and files that contain old name ──
echo ""
echo -e "${YELLOW}[3/3] Renaming directories and files...${NC}"

# Rename directories (deepest first)
find . -depth -type d \
    ! -path './.git/*' \
    ! -path './.gradle/*' \
    ! -path './.idea/*' \
    ! -path '*/build/*' \
    ! -path './.kotlin/*' \
    \( -name "*${OLD_PROJECT_NAME}*" -o -name "*${OLD_NAME_LOWER}*" \) \
    -print0 | while IFS= read -r -d '' dir; do
    PARENT=$(dirname "$dir")
    OLD_DIR_NAME=$(basename "$dir")
    NEW_DIR_NAME=$(echo "$OLD_DIR_NAME" | sed -e "s|${OLD_PROJECT_NAME}|${NEW_PROJECT_NAME}|g" -e "s|${OLD_NAME_LOWER}|${NEW_NAME_LOWER}|g")
    if [ "$OLD_DIR_NAME" != "$NEW_DIR_NAME" ]; then
        mv "$dir" "$PARENT/$NEW_DIR_NAME"
        echo -e "${GREEN}  Renamed Dir: $OLD_DIR_NAME -> $NEW_DIR_NAME${NC}"
    fi
done

# Rename files
find . -type f \
    ! -path './.git/*' \
    ! -path './.gradle/*' \
    ! -path './.idea/*' \
    ! -path '*/build/*' \
    ! -path './.kotlin/*' \
    ! -name 'rename_project.*' \
    \( -name "*${OLD_PROJECT_NAME}*" -o -name "*${OLD_NAME_LOWER}*" \) \
    -print0 | while IFS= read -r -d '' file; do
    PARENT=$(dirname "$file")
    OLD_FILE_NAME=$(basename "$file")
    NEW_FILE_NAME=$(echo "$OLD_FILE_NAME" | sed -e "s|${OLD_PROJECT_NAME}|${NEW_PROJECT_NAME}|g" -e "s|${OLD_NAME_LOWER}|${NEW_NAME_LOWER}|g")
    if [ "$OLD_FILE_NAME" != "$NEW_FILE_NAME" ]; then
        mv "$file" "$PARENT/$NEW_FILE_NAME"
        echo -e "${GREEN}  Renamed File: $OLD_FILE_NAME -> $NEW_FILE_NAME${NC}"
    fi
done

# ── [4/4] Clean up old build caches ──
echo ""
echo -e "${YELLOW}[4/4] Cleaning build caches to prevent path errors...${NC}"

rm -rf .gradle .idea .kotlin
find . -name "build" -type d -prune -exec rm -rf '{}' +

echo ""
echo -e "${GREEN}✅ Success! Project renamed to '${NEW_PROJECT_NAME}' with package '${NEW_PACKAGE_NAME}'.${NC}"
echo ""
echo -e "${CYAN}Next steps:${NC}"
echo "  1. Open in Android Studio / Fleet"
echo "  2. File -> Sync Project with Gradle Files"
echo "  3. Build -> Clean Project & Rebuild Project"
