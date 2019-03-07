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
LOG_FILE=$PMNT_DIR/log/jsvc-log.txt
#CLASS_ARGS=

# Uncomment this line to enable remote debugging:
#DEBUG_ARG=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

######### Error checking: #########
if [ "$1" = "interactive" -a ! -d "$JAVA_HOME" ]; then
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

######### Build the Java class path: #########
CP=$CLASSPATH:$PMNT_DIR
for i in $PMNT_DIR/lib/*.jar
do
CP=$CP:$i
done

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
	EXEC_HEAD="java -server"
else
	MAIN_CLASS=com.bbn.parliament.jena.jetty.JettyDaemon
	EXEC_HEAD="$PMNT_DIR/bin/jsvc -jvm server -showversion -home ""$JAVA_HOME"" -cwd $PMNT_DIR"
	if [ -n "$DAEMON_USER" ]; then
		EXEC_HEAD="$EXEC_HEAD -user ""$DAEMON_USER"""
	fi
	EXEC_HEAD="$EXEC_HEAD -outfile $LOG_FILE -errfile &1 -pidfile $PID_FILE -procname ParliamentDaemon"
	# Uncomment this line to enable verbose jsvc output:
	#EXEC_HEAD="$EXEC_HEAD -debug"
fi

EXEC_MID="-Xms$MIN_MEM -Xmx$MAX_MEM -cp $CP -Djava.library.path=$PMNT_DIR/bin"
EXEC_MID="$EXEC_MID -Dcom.sun.management.jmxremote -Dlog4j.configuration=conf/log4j.properties"
EXEC_MID="$EXEC_MID -Djetty.host=$JETTY_HOST -Djetty.port=$JETTY_PORT"
EXEC_MID="$EXEC_MID -DjettyConfig=$PMNT_DIR/conf/jetty.xml"
if [ -n "$DEBUG_ARG" ]; then
	EXEC_MID="$EXEC_MID ""$DEBUG_ARG"""
fi

EXEC_TAIL="$MAIN_CLASS"
if [ -n "$CLASS_ARGS" ]; then
	EXEC_TAIL="$EXEC_TAIL ""$CLASS_ARGS"""
fi

# Debugging statements:
# echo EXEC = $EXEC_HEAD $EXEC_MID [-stop] $EXEC_TAIL
# echo LD_LIBRARY_PATH = $LD_LIBRARY_PATH
# echo PID_FILE = $PID_FILE

do_exec()
{
	#echo $EXEC_HEAD $EXEC_MID $1 $EXEC_TAIL | tr ' ' '\n'
	$EXEC_HEAD $EXEC_MID $1 $EXEC_TAIL
}

case "$1" in
	interactive)
		do_exec
		;;
	start)
		do_exec
		;;
	stop)
		if [ -f "$PID_FILE" ]; then
			do_exec "-stop"
		else
			echo "The Parliament daemon is not running."
			exit 1
		fi
		;;
	restart)
		if [ -f "$PID_FILE" ]; then
			do_exec "-stop"
		fi
		do_exec
		;;
	*)
		echo "Usage: StartParliamentDaemon.sh {interactive|start|stop|restart}" >&2
		exit 3
		;;
esac
