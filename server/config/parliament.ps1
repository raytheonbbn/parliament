<#
.synopsis
Parliament start-up script
.description
This script starts the Parliament semantic graph store (a.k.a. triple
store) in the current shell.
#>


######### User-settable configuration parameters: #########
# Set springBootHost to 0.0.0.0 to make it accessible from other machines on the network:
$springBootHost = 'localhost'
#$springBootHost = '0.0.0.0'
$springBootPort = '8089'
$defaultJavaHeapSize = '512m'


######### Set command line parameters: #########
param (
	# (-f) Starts Parliament in the current shell (this is the default)
	[parameter(ParameterSetName="foregroundParameters")]
	[Alias("f")]
	[switch]$foreground,
	# (-c) Starts Parliament in a container environment (like Docker)
	[parameter(ParameterSetName="containerParameters")]
	[Alias("c")]
	[switch]$container
)


######### Find the Parliament installation directory, etc.: #########
$pmntDir = $PSScriptRoot
$pmntDirFwdSlash = echo $pmntDir | %{$_ -replace '\\', '/'}
$serverJarWildcard = [System.IO.Path]::Combine($pmntDir, 'lib\server-*.jar')
$serverJar = Resolve-Path $serverJarWildcard | Select -ExpandProperty Path
$debugArgs = '-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n'
$remoteMgmt = '-Dcom.sun.management.jmxremote'

######### Validate command line and installation: #########
if ($foreground -and $container) {
	echo 'Please choose only one of -foreground or -container.'
	echo ('(Type "help {0}" for documentation.)' -f [System.IO.Path]::GetFileName($PSCommandPath))
	exit 1
} else if (! $foreground -and ! $container) {
	$foreground = $true
}
if (!(test-path -pathtype leaf $serverJar)) {
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
$javaHeapSize = ([string]::IsNullOrEmpty($env:PARLIAMENT_JAVA_HEAP_SIZE)) `
	? $defaultJavaHeapSize : "$env:PARLIAMENT_JAVA_HEAP_SIZE"
$logType = $container ? 'container' : 'foreground'
$argList = @(
	'-server',
	"-Xmx$javaHeapSize",
	#$debugArgs,	# Uncomment to enable remote debugging
	#$remoteMgmt,	# Uncomment to enable remote management
	"-Dlog.path.base=$kbDirFwdSlash",
	"-Dlog4j.configuration=file:$pmntDirFwdSlash/config/logback.$logType.xml",
	"-Djava.library.path=$pmntDirFwdSlash/bin",
	'-jar', $serverJar,
	"--server.address=$springBootHost",
	"--server.port=$springBootPort"
)

######### Debugging statements: #########
#echo Java command line arguments:
#echo $argList
#exit 1

Start-Process -FilePath 'java' -ArgumentList $argList -Wait -NoNewWindow -WorkingDirectory $pmntDir
