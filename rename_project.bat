@echo off
color 0A
echo ===================================================
echo        KMP Project Template Renamer Script
echo ===================================================
echo.
echo    Windows detected. Launching PowerShell script...
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0rename_project.ps1"
pause
