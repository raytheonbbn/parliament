#!/bin/sh

usage="
Installs Parliament as a System D service.  Usage:

	$(basename "$0") --username USERNAME --directory DIRECTORY --javahome JAVAHOME

where:
	--username	the account under which to run Parliament
	--directory	the absolute path to Parliament's installation
	--javahome	the absolute path to the desired Java installation
"

username=
directory=
javahome=

while [ "$1" != "" ]; do
	case $1 in
		-u|--username)	shift
						username=$1
						;;
		-d|--directory)	shift
						directory=$1
						;;
		-j|--javahome)	shift
						javahome=$1
						;;
		-h|--help)	echo "$usage" >&2
						exit
						;;
		*)				echo "$usage" >&2
						exit 1
						;;
	esac
	shift
done

if [ -z "$username" -o -z "$directory" -o -z "$javahome" ] ; then
	echo "$usage" >&2
	exit 1
fi

cp -f parliament.service /etc/systemd/system/parliament.service
sed -i -e "s/USERNAME/$username/g" /etc/systemd/system/parliament.service
sed -i -e "s|DIRECTORY|$directory|g" /etc/systemd/system/parliament.service
sed -i -e "s|JAVAHOME|$javahome|g" /etc/systemd/system/parliament.service
systemctl start parliament
systemctl enable parliament
