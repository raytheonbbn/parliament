# Parliament Docker Container Notes

### Windows 
#### Building
If you have already built parliament in a Windows OS
Just call ant:

    ant
Otherwise specify the url to the Parliament RedHat build first in the variable "parl_url" (Note: for the above, make sure parl_url is not set!)

    set parl_url=https://github.com/SemWebCentral/parliament/releases/download/release-2.7.13/Parliament-v2.7.13-msvc-64.zip
    ant build-win

If "https_proxy" has been set as an environment variable, the ant script will use the proxy for building (only).

#### Running
Parliament publishes to port 8089 in the container. You will need to map whichever local port you need to run it in the run command.  E.g., to run it locally on port 80 you would use the following:

    docker run --name parliament -rm -dip 80:8089 parliament-win

Run it with a local directory used for persistent storage (recommended, otherwise it will reset when the container shuts down), create your directory and map it like the following.

    mkdir -p C:\data\kb-data
    docker run --name parliament --rm -dip 80:8089 -v C:\data\kb-data:C:\Parliament\kb-data parliament-win

If you need to connect to running parliament container:

    docker exec -it parliament cmd.exe

Stop the running container

    docker stop parliament

### RHEL8 build
#### Building
If you have already built parliament in a RedHat 8 OS
Just call ant:

    ant
Otherwise specify the url to the Parliament RedHat build first in the variable "parl_url" (Note: for the above, make sure parl_url is not set!)

    export parl_url=https://github.com/SemWebCentral/parliament/releases/download/release-2.7.13/Parliament-v2.7.13-gcc-centos7-64.zip
    ant build-rhel8

#### Running
Same instructions as above, but use "parliament-rhel8" as the container name instead and use platform specific paths for the volumes.
E.g, 

    mkdir -p C:\data\kb-data
    docker run --name parliament --rm -dip 80:8089 -v C:\data\kb-data:/usr/local/Parliament/kb-data parliament-rhel8
    
### Ubuntu build
#### Building    
Specify the url to the Parliament RedHat build first in the variable "parl_url" (Note: for the above, make sure parl_url is not set!)

    export parl_url=https://github.com/SemWebCentral/parliament/releases/download/release-2.7.13/Parliament-v2.7.13-gcc-ubuntu18-64.zip
    ant build-ubuntu

#### Running
Same instructions as above, but use "parliament-ubuntu" as the container name instead and use platform specific paths for the volumes.
E.g, 

    mkdir -p C:\data\kb-data
    docker run --name parliament --rm -dip 80:8089 -v C:\data\kb-data:/usr/local/Parliament/kb-data parliament-ubuntu
  
