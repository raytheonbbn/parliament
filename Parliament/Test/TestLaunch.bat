@echo off

echo cwd:  %cd%
echo arg:  %1
echo dir:  %~dp1
echo.

copy ..\KbCore\Parliament*Config.txt %~dp1
%1
