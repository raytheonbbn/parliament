# Building Parliament Docker Images

The `buildDockerImage` target in the gradle build script here creates a Docker image for Parliament. It assumes that Parliament (version 3.0.0 or later) has been built for Ubuntu Linux and that the resulting distribution is in the directory `target/parliament-3.0.0-ubuntu26/`. Once the target finishes, the Docker image will appear in your Docker Desktop images inventory.

# Using Parliament Docker Images

When you create a Parliament container from the image in Docker Desktop, expand the Optional Settings section in the dialog box and fill in the following fields:

- Container name
- Host port: The port on your host that is mapped to Parliament's port inside the container (8089)
- Host path: The location where Parliament's data files will be stored on your host
- Container path: Set this to `/var/parliament-data`
- Set the `PARLIAMENT_JAVA_HEAP_SIZE` environment variable to a suitable value. (It defaults to 1 GB.)

Alternately, you can do this at the command line. E.g., to run it locally on port 80 with the data files in the local directory `~/kb-data`, you would use the following command:

```
docker run -d -p 80:8089 --name=parliament \
	--env PARLIAMENT_JAVA_HEAP_SIZE=8g \
	--mount type=volume,src=~/kb-data,dst=/var/parliament-data \
	parliament-3.0.0-a64
```

To stop the running container use:

```
docker stop parliament
```
