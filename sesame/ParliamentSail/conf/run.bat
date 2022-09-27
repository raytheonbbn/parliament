@echo off

setlocal enableextensions enabledelayedexpansion

if not exist "%JAVA_HOME%\bin\java.exe" (
	echo.
	echo JAVA_HOME is not set properly
	goto end
)

set LCP=.;.\bin
for /r ".\lib" %%i in (*.jar) do set LCP=!LCP!;%%i

set EXEC="%JAVA_HOME%\bin\java" -Xmx1g -cp "%LCP%"
set EXEC=%EXEC% com.bbn.parliament.sesame.sail.stresstest.StressTestSuite %*

rem echo EXEC = %EXEC%
%EXEC%

:end
endlocal
