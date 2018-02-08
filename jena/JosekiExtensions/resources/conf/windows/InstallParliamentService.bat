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

set EXEC="bin\ParliamentService.exe" install --DisplayName "Parliament Triple Store"
set EXEC=%EXEC% --Description "Parliament triple store and SPARQL endpoint, from Raytheon BBN Technologies"
rem set EXEC=%EXEC% --ServiceUser "domain\your_user_name_here"
rem set EXEC=%EXEC% --ServicePassword "your_user_password_here"
set EXEC=%EXEC% --LibraryPath "%KBROOT%bin"
set EXEC=%EXEC% --Jvm "%JVM_DLL%" --JvmMs "%MIN_MEM%" --JvmMx "%MAX_MEM%"
set EXEC=%EXEC% --Classpath "%LCP%"
set EXEC=%EXEC% ++JvmOptions -Dcom.sun.management.jmxremote
set EXEC=%EXEC% ++JvmOptions -Dlog4j.configuration=conf/log4j.properties
set EXEC=%EXEC% ++JvmOptions -Djetty.host=%JETTY_HOST%
set EXEC=%EXEC% ++JvmOptions -Djetty.port=%JETTY_PORT%
set EXEC=%EXEC% --Startup auto  --StartPath "%KBROOT%."
set EXEC=%EXEC% --StartMode jvm
set EXEC=%EXEC% --StartClass com.bbn.parliament.jena.jetty.JettyService
set EXEC=%EXEC% --StartMethod startWindowsService
set EXEC=%EXEC% --StopMode jvm
set EXEC=%EXEC% --StopClass com.bbn.parliament.jena.jetty.JettyService
set EXEC=%EXEC% --StopMethod stopWindowsService
rem Error, Info, Warn, or Debug:
set EXEC=%EXEC% --LogLevel Info
set EXEC=%EXEC% --LogPath "%KBROOT%" --StdOutput auto --StdError auto

rem Debugging statements:
rem echo KBROOT = "%KBROOT%."
rem echo cd = "%cd%"
rem echo EXEC = %EXEC%
rem pause

%EXEC%

:finished
endlocal
