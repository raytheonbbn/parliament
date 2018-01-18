@echo off

setlocal enableextensions enabledelayedexpansion

if not defined JAVA_HOME (
	echo.
	echo Please set the JAVA_HOME environment variable.
	goto finished
)
set JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
if not exist "%JVM_DLL%" (
	echo.
	echo The JAVA_HOME environment variable is not valid -- can't find "%JVM_DLL%"
	goto finished
)

rem The root of the Parliament KB directory.  The key requirement
rem is that this is the parent directory of the Parliament KB's
rem lib and webapps directories.  This script assumes that it resides
rem in the root.
set KBROOT=%~dp0
if not exist "%KBROOT%lib" (
	echo.
	echo "%KBROOT%." does not contain the lib directory.
	goto finished
)
if not exist "%KBROOT%webapps\parliament.war" (
	echo.
	echo "%KBROOT%." does not contain webapps\parliament.war.
	goto finished
)

echo Using JVM from "%JAVA_HOME%"
echo.

set MIN_MEM=128m
set MAX_MEM=512m
rem Set JETTY_HOST to 0.0.0.0 to make it accessible from other machines on the network:
set JETTY_HOST=localhost
set JETTY_PORT=8089

set LCP=
for /r "%KBROOT%lib" %%i in (*.jar) do set LCP=!LCP!;%%i

set EXEC="bin\ParliamentService.exe" -install "Parliament KB"
set EXEC=%EXEC% "%JVM_DLL%" -server "-Xms%MIN_MEM%" "-Xmx%MAX_MEM%"
set EXEC=%EXEC% "-Djava.class.path=%LCP%"
set EXEC=%EXEC% -Dcom.sun.management.jmxremote
set EXEC=%EXEC% -Dlog4j.configuration="conf/log4j.properties"
set EXEC=%EXEC% -Djetty.host=%JETTY_HOST% -Djetty.port=%JETTY_PORT%
set EXEC=%EXEC% -Djava.library.path="%KBROOT%bin"
set EXEC=%EXEC% -start com.bbn.parliament.jena.jetty.JettyService
set EXEC=%EXEC% -method startWindowsService
set EXEC=%EXEC% -stop com.bbn.parliament.jena.jetty.JettyService
set EXEC=%EXEC% -method stopWindowsService -out "%KBROOT%ParliamentService.log"
set EXEC=%EXEC% -err "%KBROOT%ParliamentService.log" -current "%KBROOT%."
set EXEC=%EXEC% -path "%KBROOT%bin"
rem set EXEC=%EXEC% -user "domain\your_user_name_here"
rem set EXEC=%EXEC% -password "your_user_password_here"
set EXEC=%EXEC% -description "Parliament knowledge base server and SPARQL endpoint, from Raytheon BBN Technologies"

rem Debugging statements:
rem echo KBROOT = "%KBROOT%."
rem echo cd = "%cd%"
rem echo EXEC = %EXEC%
rem pause

%EXEC%

:finished
endlocal
