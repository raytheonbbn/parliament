#!/bin/bash

######### Usage of this script: #########
function printUsage {
	echo ""
	echo "Usage: $(basename $0) {start|stop|restart|install|uninstall|interactive}"
	echo ""
	echo "where:"
	echo ""
	echo "   'start' runs Parliament as a detached process"
	echo ""
	echo "   'stop' stops the detached Parliament process"
	echo ""
	echo "   'restart' stops and then immediately starts Parliament"
	echo ""
	if [ "$IS_SYSTEM_D" = "true" ]; then
		echo "   'install' sets Parliament up as a systemd service, so that it"
		echo "      can be controlled via the systemctl command"
		echo ""
		echo "   'uninstall' removes the Parliament systemd service definition"
	else
		echo "   'install' [not available on this platform]"
		echo ""
		echo "   'uninstall' [not available on this platform]"
	fi
	echo ""
	echo "   'interactive' starts Parliament as an attached process in the"
	echo "      current shell"
	echo ""
	exit 3
}


######### Find the Parliament installation directory: #########
cd "`dirname ""$0""`"
PMNT_DIR="`pwd`"
cd - > /dev/null


######### User-settable configuration parameters: #########
# Set JETTY_HOST to 0.0.0.0 to make it accessible from other machines on the network:
JETTY_HOST=localhost
#JETTY_HOST=0.0.0.0
JETTY_PORT=8089
JAVA_HEAP_SIZE=512m
#DAEMON_USER=iemmons

export PARLIAMENT_KB_CONFIG_PATH=$PMNT_DIR/ParliamentKbConfig.txt
export PARLIAMENT_LOG_CONFIG_PATH=$PMNT_DIR/ParliamentLogConfig.txt

LOG_FILE=$PMNT_DIR/log/jsvc.log

# Uncomment this line to enable remote debugging:
#DEBUG_ARG=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n -Dcom.sun.management.jmxremote


######### Error checking & environment detection: #########
if [ -d "/etc/systemd/system" -a -f "`which systemctl`" ]; then
	IS_SYSTEM_D="true"
fi

if [ "$1" != "interactive" -o "$1" != "install" -o "$1" != "uninstall" ]; then
	# Do nothing
	echo -n ''
elif [ ! -d "$JAVA_HOME" ]; then
	echo The JAVA_HOME environment variable is not set.
	exit 1
fi

if [ ! -f "$PMNT_DIR/lib/ParliamentServer.jar" -o ! -f "$PMNT_DIR/webapps/parliament.war" ]; then
	echo "$PMNT_DIR does not appear to be a valid Parliament installation."
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


######### Compute the PID file location: #########
KB_DIR=`sed -n 's/kbDirectoryPath[ \t]*=\(.*\)$/\1/p' $PARLIAMENT_KB_CONFIG_PATH | tail -n 1 | tr -d '[:space:]'`
SAVED_DIR=`pwd`
cd "$PMNT_DIR"
if [ ! -d "$KB_DIR" ]; then
	#echo "Creating missing KB directory ""$KB_DIR""."
	mkdir -p $KB_DIR
fi
cd "$KB_DIR"
KB_DIR="`pwd`"
cd "$SAVED_DIR"
PID_FILE=$KB_DIR/pid.txt


######### Set up the command line: #########
SERVICE_FILE="$PMNT_DIR/conf/parliament.service"
SERVICE_LINK='/etc/systemd/system/parliament.service'
if [ "$1" = 'interactive' ]; then
	MAIN_CLASS=com.bbn.parliament.jena.jetty.CmdLineJettyServer
	LOG4J_CONFIG=interactive
	EXEC='java -server'
elif [ "$1" = 'install' -o "$1" = 'uninstall' -o "$1" = 'test' ]; then
	MAIN_CLASS=com.bbn.parliament.jena.jetty.CmdLineJettyServer
	LOG4J_CONFIG=daemon
	EXEC=`which java`
	EXEC="$EXEC -server"
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

EXEC="$EXEC -Xmx$JAVA_HEAP_SIZE -cp $CLASSPATH:$PMNT_DIR/lib/*"
EXEC="$EXEC -Dlog4j.configuration=file:$PMNT_DIR/conf/log4j.$LOG4J_CONFIG.properties"
EXEC="$EXEC -Djetty.host=$JETTY_HOST -Djetty.port=$JETTY_PORT"
EXEC="$EXEC -DjettyConfig=$PMNT_DIR/conf/jetty.xml -Djava.library.path=$PMNT_DIR/bin"
if [ -n "$DEBUG_ARG" ]; then
	EXEC="$EXEC ""$DEBUG_ARG"""
fi


######### Debugging statements: #########
# echo EXEC = $EXEC [-stop] $MAIN_CLASS
# echo LD_LIBRARY_PATH = $LD_LIBRARY_PATH
# echo PID_FILE = $PID_FILE


######### Set the shared lib path: #########
function setSharedLibPath {
	# The Java property "java.library.path" is supposed to take care of this,
	# but sometimes it doesn't work, so set up the shared lib path as well:
	case "`uname -s`" in
		Darwin)
			# Do nothing -- on Macintosh java.library.path is all that's needed.
			;;
		*)
			export LD_LIBRARY_PATH=$PMNT_DIR/bin:$LD_LIBRARY_PATH
			;;
	esac
}


######### Create systemd service file: #########
# Takes path of file as an argument
function createSystemDServiceFile {
	if [ -z "$DAEMON_USER" ]; then
		echo 'Please set the DAEMON_USER variable (near the top of the script).'
		exit 1
	fi

	if [ -f "$1" -o -h "$1" ]; then
		rm $1
	fi

	echo '[Unit]' >> $1
	echo 'Description=Parliament Semantic Graph Service' >> $1
	echo 'After=network.target syslog.target' >> $1
	echo '' >> $1
	echo '[Service]' >> $1
	echo 'Type=simple' >> $1
	echo "WorkingDirectory=$PMNT_DIR" >> $1
	echo "User=$DAEMON_USER" >> $1
	echo 'Restart=always' >> $1
	#echo 'RestartSec=30' >> $1
	#echo 'SuccessExitStatus=143' >> $1
	echo '' >> $1
	echo "Environment= \\" >> $1
	echo "   LD_LIBRARY_PATH=$PMNT_DIR/bin:$LD_LIBRARY_PATH \\" >> $1
	echo "   PARLIAMENT_KB_CONFIG_PATH=$PMNT_DIR/ParliamentKbConfig.txt \\" >> $1
	echo "   PARLIAMENT_LOG_CONFIG_PATH=$PMNT_DIR/ParliamentLogConfig.txt" >> $1
	echo '' >> $1
	echo "ExecStart=$EXEC $MAIN_CLASS" >> $1
	echo '' >> $1
	echo '[Install]' >> $1
	echo 'WantedBy=multi-user.target' >> $1
}


######### Create systemd service file: #########
# Takes path of file as an argument
function installSystemDService {
	createSystemDServiceFile "$SERVICE_FILE"

	if [ -f "$SERVICE_LINK" -o -h "$SERVICE_LINK" ]; then
		rm "$SERVICE_LINK"
	fi

	ln -s "$SERVICE_FILE" "$SERVICE_LINK"
	systemctl daemon-reload
	# Does this next command need to be preceded by 'systemctl start parliament'?
	systemctl enable parliament
}


######### Execute: #########
if [ "$1" = "start" -o "$1" = "interactive" ]; then
	setSharedLibPath
	$EXEC $MAIN_CLASS
elif [ "$1" = "stop" ]; then
	if [ -f "$PID_FILE" ]; then
		setSharedLibPath
		$EXEC -stop $MAIN_CLASS
	else
		echo 'The Parliament daemon is not running.'
		exit 1
	fi
elif [ "$1" = "restart" ]; then
	if [ -f "$PID_FILE" ]; then
		setSharedLibPath
		$EXEC -stop $MAIN_CLASS
	fi
	$EXEC $MAIN_CLASS
elif [ "$1" = "install" -a "$IS_SYSTEM_D" = "true" ]; then
	installSystemDService
	echo 'You can now control Parliament via the command'
	echo '   systemctl \{ start | stop | restart \} parliament'
elif [ "$1" = "uninstall" -a "$IS_SYSTEM_D" = "true" ]; then
	systemctl stop parliament
	systemctl disable parliament
	rm "$SERVICE_FILE"
	echo 'Parliament has been uninstalled as a systemd service'
elif [ "$1" = "test" ]; then
	if [ "$IS_SYSTEM_D" = "true" ]; then
		echo "Running on a systemd operating system"
	else
		echo "This is not a systemd operating system"
	fi
	createSystemDServiceFile "$SERVICE_FILE.test"
else
	printUsage
fi
