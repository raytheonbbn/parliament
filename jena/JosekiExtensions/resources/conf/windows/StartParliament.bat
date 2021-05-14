@echo off

setlocal enableextensions enabledelayedexpansion

rem ######### Find the Parliament installation directory: #########
set PMNT_DIR=%~dp0

rem ######### User-settable configuration parameters: #########
powershell -Command "%PMNT_DIR%parliament.ps1 -foreground"

endlocal
