<#
.synopsis
Parliament start-up script
.description
This script starts the Parliament semantic graph store (a.k.a. triple
store) in the current shell.
#>


######### Set command line parameters: #########
param (
	[Parameter(HelpMessage="Starts Parliament in the current shell (this is the default)")]
	[Alias("f")]
	[switch]$foreground,
	[Parameter(HelpMessage="Starts Parliament in a container environment (like Docker)")]
	[Alias("c")]
	[switch]$container
)


######### User-settable configuration parameters: #########
# Set springBootHost to 0.0.0.0 to make it accessible from other machines on the network:
$springBootHost = 'localhost'
#$springBootHost = '0.0.0.0'
$springBootPort = '8089'
$defaultJavaHeapSize = '1g'


######### Find the Parliament installation directory, etc.: #########
$pmntDir = $PSScriptRoot
$pmntDirFwdSlash = echo $pmntDir | %{$_ -replace '\\', '/'}
$serverJarWildcard = [System.IO.Path]::Combine($pmntDir, 'lib\server-*.jar')
$serverJar = Resolve-Path $serverJarWildcard | Select -ExpandProperty Path
$debugArgs = '-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n'
$remoteMgmt = '-Dcom.sun.management.jmxremote'

######### Validate command line and installation: #########
if ($foreground -and $container) {
	echo 'Please choose only one of -foreground (-f) or -container (-c).'
	echo ('"get-help .\{0} -detailed" for help.' -f [System.IO.Path]::GetFileName($PSCommandPath))
	exit 1
} elseif (! $foreground -and ! $container) {
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
$javaHeapSize = if ([string]::IsNullOrEmpty($env:PARLIAMENT_JAVA_HEAP_SIZE))
	{$defaultJavaHeapSize} else {"$env:PARLIAMENT_JAVA_HEAP_SIZE"}
$logType = if ($container) {'container'} else {'foreground'}
$argList = @(
	'-server',
	#'-Djdk.xml.enableExtensionFunctions=true',
	'--enable-native-access=ALL-UNNAMED',
	"-Xmx$javaHeapSize",
	#$debugArgs,	# Uncomment to enable remote debugging
	#$remoteMgmt,	# Uncomment to enable remote management
	"-Dlog.path.base=$kbDirFwdSlash",
	"-Dlogging.config=$pmntDirFwdSlash/config/logback.$logType.xml",
	"-Djava.library.path=$pmntDirFwdSlash/bin",
	'-jar', $serverJar,
	"--server.address=$springBootHost",
	"--server.port=$springBootPort"
)

######### Debugging statements: #########
#echo 'Java command line arguments:'
#echo $argList
#exit 1

Start-Process -FilePath 'java' -ArgumentList $argList -Wait -NoNewWindow -WorkingDirectory $pmntDir
