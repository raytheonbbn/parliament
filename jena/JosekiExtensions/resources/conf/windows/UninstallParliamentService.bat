@echo off

setlocal

set EXEC="bin\ParliamentService.exe" -uninstall "Parliament KB"

rem echo %EXEC%

%EXEC%

endlocal
