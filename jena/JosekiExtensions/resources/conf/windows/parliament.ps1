<#
.synopsis
Starts Parliament directly, or installs it as a service
.description
This script either starts the Parliament semantic graph store (or triple
store) directly in the current shell or it installs and uninstalls it as
a Windows service, to be controlled via the Services management console.
#>

param (
	# (-f or -fg) Starts Parliament directly in the current shell
	[Alias("f","fg")]
	[switch]$foreground,
	# (-i) Installs Parliament as a service, so that it can be controlled
	# via the Services management console or the sc command
	[Alias("i")]
	[switch]$install,
	# (-u) Removes the Parliament service definition
	[Alias("u")]
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
$defaultJavaHeapSize = '512'	# must be in MB

$debugArgs = '-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n'
$remoteMgmt = '-Dcom.sun.management.jmxremote'


######### Validate command line: #########
$switchCount = 0
if ($foreground) {
	$switchCount += 1
}
if ($install) {
	$switchCount += 1
}
if ($uninstall) {
	$switchCount += 1
}
if ($switchCount -lt 1) {
	echo 'Please specify one of -foreground, -install, or -uninstall.'
	echo ('(Type "help {0}" for documentation.)' -f [System.IO.Path]::GetFileName($PSCommandPath))
	exit 1
}
if ($switchCount -gt 1) {
	echo 'Please choose only one of -foreground, -install, or -uninstall.'
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


######### Get the data directory location: #########

$parliamentKbConfigPath = [System.IO.Path]::Combine($pmntDir, 'ParliamentKbConfig.txt')
$kbDir = cat $parliamentKbConfigPath `
	| ?{$_ -notmatch '^[ \\t]*#'} `
	| ?{$_ -match 'kbDirectoryPath'} `
	| %{$_ -replace 'kbDirectoryPath[ \\t]*=(.*)$', '$1'} `
	| %{$_.Trim()}
pushd $pmntDir
$kbDir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($kbDir)
popd
$kbDirFwdSlash = echo $kbDir | %{$_ -replace '\\', '/'}


######### Set up the command line: #########
if ("$env:PARLIAMENT_JAVA_HEAP_SIZE" -ne "") {
	$javaHeapSize = "$env:PARLIAMENT_JAVA_HEAP_SIZE"
} else {
	$javaHeapSize = $defaultJavaHeapSize
}
if ($foreground) {
	$argList = @(
		'-server',
		("-Xmx$javaHeapSize" + 'm'),
		'-cp', "$pmntDirFwdSlash/lib/*",
		#$debugArgs,	# Uncomment to enable remote debugging
		#$remoteMgmt,	# Uncomment to enable remote management
		"-Dlog.path.base=$kbDirFwdSlash",
		"-Dlog4j.configuration=file:$pmntDirFwdSlash/conf/log4j.foreground.properties",
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
			'--LogPath', """$kbDir\log"""
		)
	} elseif ($uninstall) {
		$argList = @('//DS//Parliament')
	}

	######### Debugging statements: #########
	#echo $executable
	#echo $argList

	Start-Process -FilePath $executable -ArgumentList $argList -Wait -WorkingDirectory $pmntDir -Verb 'RunAs'
}
