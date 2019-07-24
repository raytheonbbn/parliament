#!/bin/sh

######### Find the Parliament installation directory: #########
cd "`dirname ""$0""`"
PMNT_DIR="`pwd`"
cd - > /dev/null

######### User-settable configuration parameters: #########
export PARLIAMENT_KB_CONFIG_PATH=$PMNT_DIR/ParliamentKbConfig.txt
export PARLIAMENT_LOG_CONFIG_PATH=$PMNT_DIR/ParliamentLogConfig.txt

MIN_MEM=128m
MAX_MEM=512m
# Set JETTY_HOST to 0.0.0.0 to make it accessible from other machines on the network:
JETTY_HOST=localhost
JETTY_PORT=8089

#DAEMON_USER=jsmith
LOG_FILE=$PMNT_DIR/log/jsvc.log

# Uncomment this line to enable remote debugging:
#DEBUG_ARG=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

######### Error checking: #########
if [ "$1" != "interactive" -a ! -d "$JAVA_HOME" ]; then
	echo The JAVA_HOME environment variable is not set.
	exit 1
fi

if [ ! -f "$PARLIAMENT_KB_CONFIG_PATH" ]; then
	echo "Unable to find Parliament configuration file."
	exit 1
fi

if [ ! -f "$PARLIAMENT_LOG_CONFIG_PATH" ]; then
	echo "Unable to find Parliament log configuration file."
	exit 1
fi

# Check that PMNT_DIR really is a Parliament KB directory.
if [ ! -f "$PMNT_DIR/lib/ParliamentServer.jar" ]; then
	echo "Unable to find Parliament's lib directory."
	exit 1
fi
if [ ! -f "$PMNT_DIR/webapps/parliament.war" ]; then
	echo "Unable to find webapps/parliament.war."
	exit 1
fi

######### Compute the PID file location: #########
KB_DIR=`sed -n 's/kbDirectoryPath[ \t]*=\(.*\)$/\1/p' $PARLIAMENT_KB_CONFIG_PATH | tail -n 1 | tr -d '[:space:]'`
SAVED_DIR=`pwd`
cd "$PMNT_DIR"
if [ ! -d "$KB_DIR" ]; then
	echo "Creating missing KB directory \"$KB_DIR\"."
	mkdir -p $KB_DIR
fi
cd "$KB_DIR"
KB_DIR="`pwd`"
cd "$SAVED_DIR"
PID_FILE=$KB_DIR/pid.txt

######### Set up the shared lib path: #########
# The Java property "java.library.path" below is supposed to take care of these,
# but sometimes it doesn't work, so set up the shared lib path as well:
case "`uname -s`" in
	Darwin)
		# Do nothing -- on Macintosh java.library.path is all that's needed.
		;;
	*)
		export LD_LIBRARY_PATH=$PMNT_DIR/bin:$LD_LIBRARY_PATH
		;;
esac

######### Set up the command line: #########
if [ "$1" = "interactive" ]; then
	MAIN_CLASS=com.bbn.parliament.jena.jetty.CmdLineJettyServer
	LOG4J_CONFIG=interactive
	EXEC="java -server"
else
	MAIN_CLASS=com.bbn.parliament.jena.jetty.JettyDaemon
	LOG4J_CONFIG=daemon
	EXEC="$PMNT_DIR/bin/jsvc -jvm server -showversion -home ""$JAVA_HOME"" -cwd $PMNT_DIR"
	if [ -n "$DAEMON_USER" ]; then
		EXEC="$EXEC -user ""$DAEMON_USER"""
	fi
	EXEC="$EXEC -outfile $LOG_FILE -errfile &1 -pidfile $PID_FILE -procname Parliament"
	# Uncomment this line to enable verbose jsvc output:
	#EXEC="$EXEC -debug"
fi

EXEC="$EXEC -Xms$MIN_MEM -Xmx$MAX_MEM -cp $CLASSPATH:$PMNT_DIR/lib/*"
EXEC="$EXEC -Djava.library.path=$PMNT_DIR/bin -Dcom.sun.management.jmxremote"
EXEC="$EXEC -Dlog4j.configuration=file:$PMNT_DIR/conf/log4j.$LOG4J_CONFIG.properties"
EXEC="$EXEC -Djetty.host=$JETTY_HOST -Djetty.port=$JETTY_PORT"
EXEC="$EXEC -DjettyConfig=$PMNT_DIR/conf/jetty.xml"
if [ -n "$DEBUG_ARG" ]; then
	EXEC="$EXEC ""$DEBUG_ARG"""
fi

######### Debugging statements: #########
# echo EXEC = $EXEC [-stop] $MAIN_CLASS
# echo LD_LIBRARY_PATH = $LD_LIBRARY_PATH
# echo PID_FILE = $PID_FILE

######### Execute: #########
case "$1" in
	interactive)
		$EXEC $MAIN_CLASS
		;;
	start)
		$EXEC $MAIN_CLASS
		;;
	stop)
		if [ -f "$PID_FILE" ]; then
			$EXEC -stop $MAIN_CLASS
		else
			echo "The Parliament daemon is not running."
			exit 1
		fi
		;;
	restart)
		if [ -f "$PID_FILE" ]; then
			$EXEC -stop $MAIN_CLASS
		fi
		$EXEC $MAIN_CLASS
		;;
	*)
		echo "Usage: StartParliament.sh {interactive|start|stop|restart}" >&2
		exit 3
		;;
esac
