usage="$(basename "$0") --username USERNAME --directory DIRECTORY -- installs Parliament as a System D service

where:
	--username		the account under which to run Parliament
	--directory		the absolute path to Parliament's installation
"
username=
directory=

while [ "$1" != "" ]; do
	case $1 in
		-u | --username )	shift
							username=$1
							;;
		-d | --d )			shift
							directory=$1
							;;
		-h | --help )		echo "$usage"> &2
							exit
							;;
		* )					echo "$usage" >&2
							exit 1
							;;
	esac
	shift
done

if [ username == "" -o directory == "" ] ; then
	echo "$usage" >&2
	exit 1
fi

sed -i -e "s/USERNAME/$username/g" $directory/parliament.service
cp parliament.service /etc/systemd/system/parliament.service
systemctl enable parliament
systemctl start parliament
