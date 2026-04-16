Write-Host "===================================================" -ForegroundColor Green
Write-Host "       KMP Project Template Renamer Script" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Make sure you have copied this entire project folder to" -ForegroundColor Yellow
Write-Host "your new location BEFORE running this script!" -ForegroundColor Yellow
Write-Host ""

$CurrentDir = $PSScriptRoot
Set-Location $CurrentDir

# ── Auto-detect old project name ──
$SettingsFile = Join-Path $CurrentDir "settings.gradle.kts"
$OldProjectName = "Unknown"
if (Test-Path $SettingsFile) {
    $SettingsContent = Get-Content $SettingsFile -Raw
    if ($SettingsContent -match 'rootProject\.name\s*=\s*"([^"]+)"') {
        $OldProjectName = $matches[1]
    }
}

# ── Auto-detect old package name ──
$AndroidGradleFile = Join-Path $CurrentDir "androidApp/build.gradle.kts"
$OldPackageName = "Unknown"
if (Test-Path $AndroidGradleFile) {
    $AndroidGradleContent = Get-Content $AndroidGradleFile -Raw
    if ($AndroidGradleContent -match 'namespace\s*=\s*"([^"]+)"') {
        $OldPackageName = $matches[1]
    }
}

Write-Host "Detected Existing Project Name:  $OldProjectName" -ForegroundColor Cyan
Write-Host "Detected Existing Package Name:  $OldPackageName" -ForegroundColor Cyan
Write-Host ""

if ($OldProjectName -eq "Unknown" -or $OldPackageName -eq "Unknown") {
    Write-Host "Error: Could not detect old project or package name." -ForegroundColor Red
    exit 1
}

# ── Read new names from user ──
$NewProjectName = Read-Host "Enter NEW Project Name (e.g. MyNewApp)      "
$NewPackageName = Read-Host "Enter NEW Package Name (e.g. com.sanket.tool)"

if ([string]::IsNullOrWhiteSpace($NewProjectName) -or [string]::IsNullOrWhiteSpace($NewPackageName)) {
    Write-Host "Error: New project name and package name cannot be empty." -ForegroundColor Red
    exit 1
}

$OldAppNameLower = $OldProjectName.ToLower()
$NewAppNameLower = $NewProjectName.ToLower()

$OldPackagePath = $OldPackageName.Replace(".", "\")
$NewPackagePath = $NewPackageName.Replace(".", "\")
$OldPackagePathUnix = $OldPackageName.Replace(".", "/")
$NewPackagePathUnix = $NewPackageName.Replace(".", "/")

$IgnoreDirs = @("\\\.git\\", "\\\.gradle\\", "\\\.idea\\", "\\build\\", "\\\.kotlin\\")
$AllowedExtensions = @(".kt", ".kts", ".xml", ".properties", ".md", ".xcconfig", ".pbxproj", ".java", ".yaml", ".yml", ".json", ".toml", ".gradle", ".html", ".swift", ".plist")
$AllowedFiles = @("gradlew", "gradlew.bat")

Write-Host ""
Write-Host "Renaming project to '$NewProjectName'..." -ForegroundColor Green
Write-Host "Renaming package to '$NewPackageName'..." -ForegroundColor Green

function Should-Ignore($path) {
    foreach ($dir in $IgnoreDirs) {
        if ($path -match $dir) { return $true }
    }
    return $false
}

# ── [1/3] Replace text inside files ──
Write-Host "`n[1/3] Modifying file contents..." -ForegroundColor Yellow
$AllFiles = Get-ChildItem -Path $CurrentDir -Recurse -File | Where-Object { -not (Should-Ignore $_.FullName) }

foreach ($file in $AllFiles) {
    if (($AllowedExtensions -contains $file.Extension) -or ($AllowedFiles -contains $file.Name)) {
        if ($file.Name -match "rename_project") { continue }

        try {
            $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
            $newContent = $content
            $modified = $false

            # Replace Package Name first (more specific)
            if ($newContent.Contains($OldPackageName)) {
                $newContent = $newContent.Replace($OldPackageName, $NewPackageName)
                $modified = $true
            }
            if ($newContent.Contains($OldPackagePathUnix)) {
                $newContent = $newContent.Replace($OldPackagePathUnix, $NewPackagePathUnix)
                $modified = $true
            }

            # Replace Project Name second
            if ($newContent.Contains($OldProjectName)) {
                $newContent = $newContent.Replace($OldProjectName, $NewProjectName)
                $modified = $true
            }
            if ($newContent.Contains($OldAppNameLower)) {
                $newContent = $newContent.Replace($OldAppNameLower, $NewAppNameLower)
                $modified = $true
            }

            if ($modified) {
                [System.IO.File]::WriteAllText($file.FullName, $newContent, [System.Text.Encoding]::UTF8)
            }
        }
        catch {
            Write-Host "  Skipped: $($file.FullName)" -ForegroundColor DarkGray
        }
    }
}

# ── [2/3] Move source files to new package structure ──
Write-Host "`n[2/3] Moving source code to new package structure..." -ForegroundColor Yellow

$OldPackageParts = $OldPackageName.Split(".")
$SourceDirs = @(
    "androidApp\src\main\kotlin", "androidApp\src\test\kotlin", "androidApp\src\androidTest\kotlin",
    "shared\src\commonMain\kotlin", "shared\src\commonTest\kotlin", "shared\src\androidMain\kotlin",
    "shared\src\iosMain\kotlin", "shared\src\jvmMain\kotlin", "shared\src\jsMain\kotlin", "shared\src\wasmJsMain\kotlin",
    "server\src\main\kotlin", "server\src\test\kotlin",
    "composeApp\src\commonMain\kotlin", "composeApp\src\commonTest\kotlin", "composeApp\src\androidMain\kotlin",
    "composeApp\src\desktopMain\kotlin", "composeApp\src\iosMain\kotlin", "composeApp\src\jvmMain\kotlin",
    "composeApp\src\wasmJsMain\kotlin", "composeApp\src\jsMain\kotlin", "composeApp\src\webMain\kotlin"
)

foreach ($src in $SourceDirs) {
    $basePath = Join-Path $CurrentDir $src
    if (Test-Path $basePath) {
        $oldPath = Join-Path $basePath $OldPackagePath
        $newPath = Join-Path $basePath $NewPackagePath

        if (Test-Path $oldPath) {
            if (-not (Test-Path $newPath)) {
                New-Item -ItemType Directory -Force -Path $newPath | Out-Null
            }
            Get-ChildItem -Path $oldPath | Move-Item -Destination $newPath -Force
            Write-Host "  Moved: $src" -ForegroundColor Green

            # Clean up empty old directories
            $currentCleanPath = $oldPath
            for ($i = $OldPackageParts.Length - 1; $i -ge 0; $i--) {
                if ((Test-Path $currentCleanPath) -and (Get-ChildItem -Path $currentCleanPath).Count -eq 0) {
                    Remove-Item -Path $currentCleanPath -Force
                    $currentCleanPath = Split-Path $currentCleanPath
                }
                else { break }
            }
        }
    }
}

# ── [3/3] Rename directories and files ──
Write-Host "`n[3/3] Renaming directories and files..." -ForegroundColor Yellow

# Directories (deepest first)
$AllDirs = Get-ChildItem -Path $CurrentDir -Recurse -Directory |
    Where-Object { -not (Should-Ignore $_.FullName) } |
    Sort-Object @{Expression = { $_.FullName.Length }; Descending = $true }

foreach ($item in $AllDirs) {
    if ($item.Name.Contains($OldProjectName) -or $item.Name.Contains($OldAppNameLower)) {
        $newName = $item.Name.Replace($OldProjectName, $NewProjectName).Replace($OldAppNameLower, $NewAppNameLower)
        try {
            Rename-Item -Path $item.FullName -NewName $newName
            Write-Host "  Renamed Dir: $($item.Name) -> $newName" -ForegroundColor Green
        }
        catch {
            Write-Host "  Failed: $($item.FullName)" -ForegroundColor Red
        }
    }
}

# Files
$AllFilesToRename = Get-ChildItem -Path $CurrentDir -Recurse -File |
    Where-Object { -not (Should-Ignore $_.FullName) }

foreach ($item in $AllFilesToRename) {
    if ($item.Name -match "rename_project") { continue }
    if ($item.Name.Contains($OldProjectName) -or $item.Name.Contains($OldAppNameLower)) {
        $newName = $item.Name.Replace($OldProjectName, $NewProjectName).Replace($OldAppNameLower, $NewAppNameLower)
        try {
            Rename-Item -Path $item.FullName -NewName $newName
            Write-Host "  Renamed File: $($item.Name) -> $newName" -ForegroundColor Green
        }
        catch {
            Write-Host "  Failed: $($item.FullName)" -ForegroundColor Red
        }
    }
}

# ── [4/4] Clean up old build caches ──
Write-Host "`n[4/4] Cleaning build caches to prevent path errors..." -ForegroundColor Yellow

Remove-Item -Path ".gradle", ".idea", ".kotlin" -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path . -Recurse -Directory -Filter "build" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "✅ Success! Project renamed to '$NewProjectName' with package '$NewPackageName'." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Open in Android Studio / Fleet"
Write-Host "  2. File -> Sync Project with Gradle Files"
Write-Host "  3. Build -> Clean Project & Rebuild Project"
