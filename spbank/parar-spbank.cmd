@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\spbank.ps1" -Stop
if errorlevel 1 pause
