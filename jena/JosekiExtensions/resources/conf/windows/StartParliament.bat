@echo off

setlocal enableextensions enabledelayedexpansion

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

echo Using the following version of Java:
java -version
echo.

set MIN_MEM=128m
set MAX_MEM=512m
rem Set JETTY_HOST to 0.0.0.0 to make it accessible from other machines on the network:
set JETTY_HOST=localhost
set JETTY_PORT=8089

set DEBUG_ARG=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

rem The Java property "java.library.path" below is supposed to take care
rem of this, but sometimes it doesn't work, so set up the Path as well:
set Path=%KBROOT%bin;%Path%

rem set PARLIAMENT_CONFIG_PATH=%KBROOT%ParliamentConfig.txt

set LCP=
for /r "%KBROOT%lib" %%i in (*.jar) do set LCP=!LCP!;%%i

set EXEC=java -server -Xms%MIN_MEM% -Xmx%MAX_MEM% -cp "%LCP%"
rem Uncomment this line to enable remote debugging:
rem set EXEC=%EXEC% %DEBUG_ARG%
set EXEC=%EXEC% -Dcom.sun.management.jmxremote
set EXEC=%EXEC% -Dlog4j.configuration="conf/log4j.properties"
set EXEC=%EXEC% -Djetty.host=%JETTY_HOST% -Djetty.port=%JETTY_PORT%
set EXEC=%EXEC% -Djava.library.path="%KBROOT%bin"
set EXEC=%EXEC% com.bbn.parliament.jena.jetty.CmdLineJettyServer
set EXEC=%EXEC% %*

rem Debugging statements:
rem echo KBROOT = "%KBROOT%."
rem echo cd = "%cd%"
rem path
rem echo EXEC = %EXEC%
rem pause

%EXEC%

:finished
endlocal
