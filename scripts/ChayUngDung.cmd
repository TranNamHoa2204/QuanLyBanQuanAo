@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0ChayUngDung.ps1" %*
set "runExitCode=%ERRORLEVEL%"
if not "%runExitCode%"=="0" pause
exit /b %runExitCode%
