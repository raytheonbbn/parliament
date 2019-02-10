@echo off

echo cwd:  %cd%
echo arg:  %1
echo dir:  %~dp1
echo.

copy ..\KbCore\ParliamentConfig.txt %~dp1\ParliamentConfig.txt
%1
