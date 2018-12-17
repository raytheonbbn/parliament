usage="$(basename "$0") --username USERNAME --directory DIRECTORY -- installs Parliament as a System D service

where:
	--username		the account under which to run Parliament
	--directory		the absolute path to Parliament's installation
"
username=
directory=

while [ "$1" != "" ]; do
	case $1 in
		-u|--username)	shift
						username=$1
						;;
		-d|--d)			shift
						directory=$1
						;;
		-h|--help)		echo "$usage" >&2
						exit
						;;
		*)				echo "$usage" >&2
						exit 1
						;;
	esac
	shift
done

if [ -z "$username" -o -z "$directory" ] ; then
	echo "$usage" >&2
	exit 1
fi

if [ -z "$JAVA_HOME" ] ; then
	JAVA_HOME=$(dirname $(which java))
fi

cp -f parliament.service /etc/systemd/system/parliament.service
sed -i -e "s/USERNAME/$username/g" /etc/systemd/system/parliament.service
sed -i -e "s|DIRECTORY|$directory|g" /etc/systemd/system/parliament.service
sed -i -e "s|JAVAHOME|$JAVA_HOME|g" /etc/systemd/system/parliament.service
systemctl enable parliament
systemctl start parliament
