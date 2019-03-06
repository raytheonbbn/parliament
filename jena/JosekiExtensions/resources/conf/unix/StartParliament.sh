#!/bin/sh

MIN_MEM=128m
MAX_MEM=512m
# Set JETTY_HOST to 0.0.0.0 to make it accessible from other machines on the network:
JETTY_HOST=localhost
JETTY_PORT=8089

DEBUG_ARG=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

# This script assumes that it resides in the Parliament KB directory,
# and that this directory is the CWD.
if [ ! -d "./lib" ]; then
	echo The current directory does not contain the lib directory.
	exit 1
fi
if [ ! -f "./webapps/parliament.war" ]; then
	echo The current directory does not contain webapps/parliament.war.
	exit 1
fi

echo Using the following version of Java:
java -version

CP=$CLASSPATH
for i in lib/*.jar
do
CP=$CP:$i
done

# The Java property "java.library.path" below is supposed to take care of these,
# but sometimes it doesn't work, so set up the shared lib path as well:
export LD_LIBRARY_PATH=./bin:$LD_LIBRARY_PATH

EXEC="java -server -Xms$MIN_MEM -Xmx$MAX_MEM -cp $CP -Djava.library.path=./bin"
EXEC="$EXEC -Dcom.sun.management.jmxremote -Dlog4j.configuration=conf/log4j.properties"
EXEC="$EXEC -Djetty.host=$JETTY_HOST -Djetty.port=$JETTY_PORT"
# Uncomment this line to enable remote debugging:
#EXEC="$EXEC $DEBUG_ARG"
EXEC="$EXEC com.bbn.parliament.jena.jetty.CmdLineJettyServer $@"

# Debugging statements:
# echo EXEC = $EXEC
# echo LD_LIBRARY_PATH = $LD_LIBRARY_PATH

$EXEC
