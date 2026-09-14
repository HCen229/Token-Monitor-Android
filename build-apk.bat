@echo off
chcp 65001 >nul
powershell -ExecutionPolicy Bypass -File "%~dp0build-apk.ps1"
pause
