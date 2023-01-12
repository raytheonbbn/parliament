@echo off

setlocal enableextensions enabledelayedexpansion

if not exist "%JAVA_HOME%\bin\java.exe" (
	echo.
	echo JAVA_HOME is not set properly
	goto end
)

set LCP=.;.\bin;..\..\target\artifacts\ParliamentClient.jar
for /r "..\..\lib\HttpClient" %%i in (*.jar) do set LCP=!LCP!;%%i

set EXEC="%JAVA_HOME%\bin\java" -cp "%LCP%"
set EXEC=%EXEC% com.bbn.parliament.client.jena.RemoteExporter %*

rem echo EXEC = %EXEC%
%EXEC%

:end

endlocal
