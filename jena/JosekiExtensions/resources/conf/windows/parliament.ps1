<#
.synopsis
Starts Parliament directly, or installs it as a service
.description
Get-Function displays the name and syntax of all functions in the session.
#>

param (
	# Starts Parliament as an attached process in the current shell
	[switch][Alias("attached","inter")]$interactive,
	# Sets up Parliament as a service, so that it can be controlled via the service control panel or the sc command
	[switch]$install,
	# Removes the Parliament service definition
	[switch]$uninstall
)

######### Find the Parliament installation directory: #########
$pmntDir = $PSScriptRoot
$pmntDirFwdSlash = echo $pmntDir | %{$_ -replace '\\', '/'}


######### User-settable configuration parameters: #########
# Set jettyHost to 0.0.0.0 to make it accessible from other machines on the network:
$jettyHost = 'localhost'
#$jettyHost = '0.0.0.0'
$jettyPort = '8089'
$javaHeapSize = '512'	# must be in MB

$env:PARLIAMENT_KB_CONFIG_PATH = [System.IO.Path]::Combine($pmntDir, 'ParliamentKbConfig.txt')
$env:PARLIAMENT_LOG_CONFIG_PATH = [System.IO.Path]::Combine($pmntDir, 'ParliamentLogConfig.txt')

$debugArgs = '-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n'
$remoteMgmt = '-Dcom.sun.management.jmxremote'


######### Validate command line: #########
$switchCount = 0
if ($interactive) {
	$switchCount += 1
}
if ($install) {
	$switchCount += 1
}
if ($uninstall) {
	$switchCount += 1
}
if ($switchCount -lt 1) {
	echo 'Please specify one of -interactive, -install, or -uninstall.'
	echo ('(Type "help {0}" for documentation.)' -f [System.IO.Path]::GetFileName($PSCommandPath))
	exit 1
}
if ($switchCount -gt 1) {
	echo 'Please choose only one of -interactive, -install, or -uninstall.'
	echo ('(Type "help {0}" for documentation.)' -f [System.IO.Path]::GetFileName($PSCommandPath))
	exit 1
}


######### Error checking & environment detection: #########
function isMissing($relativeFilePath) {
	$absPath = [System.IO.Path]::Combine($pmntDir, $relativeFilePath)
	return !(test-path -pathtype leaf $absPath)
}

$jvmDll = [System.IO.Path]::Combine($env:JAVA_HOME, 'jre\bin\server\jvm.dll')
if (($install -or $uninstall) -and !(test-path -pathtype leaf $jvmDll)) {
	echo "The JAVA_HOME environment variable is not valid -- can't find '$jvmDll'"
	exit 1
}
if (isMissing('webapps\parliament.war') -or isMissing('lib\ParliamentServer.jar')) {
	echo "$pmntDir does not appear to be a valid Parliament installation"
	exit 1
}
if (!(test-path -pathtype leaf $env:PARLIAMENT_KB_CONFIG_PATH)) {
	echo 'Unable to find Parliament configuration file'
	exit 1
}
if (!(test-path -pathtype leaf $env:PARLIAMENT_LOG_CONFIG_PATH)) {
	echo 'Unable to find Parliament log configuration file'
	exit 1
}


######### Get the data directory location: #########

$kbDir = cat $env:PARLIAMENT_KB_CONFIG_PATH `
	| ?{$_ -notmatch '^[ \\t]*#'} `
	| ?{$_ -match 'kbDirectoryPath'} `
	| %{$_ -replace 'kbDirectoryPath[ \\t]*=(.*)$', '$1'} `
	| %{$_.Trim()}
pushd $pmntDir
$kbDir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($kbDir)
popd
$kbDirFwdSlash = echo $kbDir | %{$_ -replace '\\', '/'}


######### Set env vars: #########
$env:PARLIAMENT_LOG_PATH_BASE = $kbDir

# The Java property "java.library.path" below is supposed to take care
# of this, but sometimes it doesn't work, so set up the Path as well:
$env:Path += ";$pmntDir\bin"


######### Set up the command line: #########
if ($interactive) {
	$argList = @(
		'-server',
		("-Xmx$javaHeapSize" + 'm'),
		'-cp', "$pmntDirFwdSlash/lib/*",
		#$debugArgs,	# Uncomment to enable remote debugging
		#$remoteMgmt,	# Uncomment to enable remote management
		"-Dlog.path.base=$kbDirFwdSlash",
		"-Dlog4j.configuration=file:$pmntDirFwdSlash/conf/log4j.interactive.properties",
		"-Djetty.host=$jettyHost",
		"-Djetty.port=$jettyPort",
		"-Djava.library.path=$pmntDirFwdSlash/bin",
		'com.bbn.parliament.jena.jetty.CmdLineJettyServer'
	)

	######### Debugging statements: #########
	#echo $executable
	#echo $argList

	Start-Process -FilePath 'java' -ArgumentList $argList -Wait -NoNewWindow -WorkingDirectory $pmntDir
} else {
	$executable = [System.IO.Path]::Combine($pmntDir, 'bin\ParliamentService.exe')
	if ($install) {
		$mainClass = 'com.bbn.parliament.jena.jetty.JettyService'
		$argList = @(
			'//US//Parliament',
			'--DisplayName', 'Parliament',
			'--Description', '"Parliament triple store and SPARQL endpoint, from Raytheon BBN Technologies"',
			#'--ServiceUser' 'domain\your_user_name_here',
			#'--ServicePassword' 'your_user_password_here',
			'--LibraryPath', """$pmntDir\bin""",
			'--Jvm', """$jvmDll""",
			'--JvmMx', $javaHeapSize,
			'--Classpath', """$pmntDirFwdSlash/lib/*""",
			'++JvmOptions', """-Dlog4j.configuration=file:$pmntDirFwdSlash/conf/log4j.daemon.properties""",
			'++JvmOptions', "-Djetty.host=$jettyHost",
			'++JvmOptions', "-Djetty.port=$jettyPort",
			'++JvmOptions', """-Dlog.path.base=$kbDirFwdSlash""",
			#'++JvmOptions', $debugArgs,	# Uncomment to enable remote debugging
			#'++JvmOptions', $remoteMgmt,	# Uncomment to enable remote management
			'--Startup', 'auto',
			'--StartPath', """$pmntDir""",
			'--StartMode', 'jvm',
			'--StartClass', $mainClass,
			'--StartMethod', 'start',
			'--StopMode', 'jvm',
			'--StopClass', $mainClass,
			'--StopMethod', 'stop',
			'--StdOutput', 'auto',
			'--StdError', 'auto',
			'--LogLevel', 'Info',	# Error, Info, Warn, or Debug
			'--LogPath', """$kbDir\log""",
			'++Environment', """PARLIAMENT_KB_CONFIG_PATH=$env:PARLIAMENT_KB_CONFIG_PATH""",
			'++Environment', """PARLIAMENT_LOG_CONFIG_PATH=$env:PARLIAMENT_LOG_CONFIG_PATH""",
			'++Environment', """PARLIAMENT_LOG_PATH_BASE=$env:PARLIAMENT_LOG_PATH_BASE"""
		)
	} elseif ($uninstall) {
		$argList = @('//DS//Parliament')
	}

	######### Debugging statements: #########
	#echo $executable
	#echo $argList

	Start-Process -FilePath $executable -ArgumentList $argList -Wait -WorkingDirectory $pmntDir -Verb 'RunAs'
}
