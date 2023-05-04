@echo off

setlocal enableextensions enabledelayedexpansion

if not exist "%JAVA_HOME%\bin\java.exe" (
	echo.
	echo JAVA_HOME is not set properly
	goto end
)

set EXEC="%JAVA_HOME%\bin\java" -cp lib/default/*;../../target/artifacts/*
set EXEC=%EXEC% com.bbn.parliament.client.RemoteInserter %*

rem echo EXEC = %EXEC%
%EXEC%

:end
endlocal
