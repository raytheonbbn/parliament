# Building Parliament Docker Images

The build script here creates Docker images to run Parliament.  It takes one
command-line argument and a target, like so:

	ant -Ddistro=parliament-distribution target

Here `parliament-distribution` is mandatory and takes one of the following forms:

* The URL of a Parliament release on GitHub, e.g.,
  https://github.com/SemWebCentral/parliament/releases/download/release-2.8.1/Parliament-v2.8.1-gcc-ubuntu20-64.zip

* The absolute or relative path to an already-downloaded Parliament release, e.g.,
  somedir/Parliament-v2.8.1-gcc-ubuntu20-64.zip

Note that the distribution you use must be version 2.8.1 or later.

Valid values for `target` are:

* `build-ubuntu` (the default) creates an Ubuntu-based container.
* `build-rhel8` creates a RHEL-based container.  Note that the scripts for this option are not yet complete, as they do not provide a way to pass in the user name and password for a Red Hat developer account.
* `build-win` creates a Windows-based container.  Again, the scripts for this option are not yet complete, because they do not include provision for authentication to Microsoft's base images.

If you are behind a firewall and need a proxy, you may need to fiddle with ant's proxy settings.  Generally, however, ant will properly use your system settings for this.

Once the build finishes, the Docker image will appear in the `target/distro` directory.

# Using Parliament Docker Images

You can load the factory built images by downloading one of the docker image *-docker.tar.bz2 files from github, or if you built a new container, it will be found in the `target/distro` directory.  Load locally using:

	docker load -i parliament-2.8.1-ubuntu-docker.tar.bz2

You can verify it was loaded properly with:

	docker image ls

Parliament operates on port 8089 in the container and exposes a volume at the destination `/var/parliament-data`. You will need to map those to a local port and a volume on the host using the run command.  E.g., to run it locally on port 80 with a local volume called `parliament-data` you would use the following command:

	docker run -d -p 80:8089 --name=parliament \
		--mount type=volume,src=parliament-data,dst=/var/parliament-data \
		parliament-2.8.1

To stop the running container use:

	docker stop parliament
