# News

**June 9, 2022:**  Released Parliament™ version 2.8.1.  Changes of note:

- This release includes a Docker image on Docker Hub: idemmons/parliament:2.8.1

- Introduced a clean separation between Parliament's software and data files.
  The software files may now be read-only to the account under which Parliament
  is running, and the data files are easily moved to a writable location
  suitable for mass storage, such as an alternate partition, network mount,
  NAS or SAN device.  All data files, including log files, are now stored under
  the directory given by the kbDirectoryPath setting in ParliamentKbConfig.txt
  (default is kb-data under the installation directory).

- The startup script on Linux/UNIX properly supports systemd service installation.

- The startup script on Windows parallels the Linux/UNIX script, and is now a
  PowerShell script.

- Fixed an issue on UNIX/Linux platforms that prevented the Parliament daemon
  from receiving SIGTERM signals, making Parliament difficult to use in a
  Docker container.

- Made the bdbCacheSize configuration parameter in ParliamentKbConfig.txt
  global across all graphs. Previously, a cache of this size was dedicated to
  each named graph. With this change, a single cache of this size is shared by
  all graphs. This is particularly beneficial to deployments that have a large
  number of graphs.  Also, raised the default cache size from 32 MB to 512 MB.

- Reduced the volume of logging when Parliament is running in daemon mode.

- Numerous updates to bring in line with Java 11 & 17 (but still using Java 8)

- Several documentation improvements, including a new section detailing how to
  secure a Parliament instance.



**May 7, 2019:**  Released Parliament™ version 2.7.13.  Changes of note:

- Added reserved predicates par:directType and par:directSubClassOf to speed
  queries for most-derived types.

- Fixed subsumption inference so that properties are always sub-properties of
  themselves.



**April 17, 2019:**  Released Parliament™ version 2.7.12.  Changes of note:

- Parliament was recently moved to this GitHub project from its long-time home
  on [SemWebCentral](http://parliament.semwebcentral.org/)

- Added a SWRL rules engine

- Added a script to install Parliament as a service/daemon on systemd-based
  Linux distributions, including CentOS and Ubuntu

- Long-running queries now time out

- Added linear growth settings for Parliament's resource and statement tables

- The inference engine now recognizes classes to be subclasses of themselves

- Decreased the likelihood that an ungraceful shutdown will corrupt the data files

- Fixed numerous bugs



# Parliament™ Introduction

Parliament™ is a high-performance triple store and reasoner designed for the
[Semantic Web](http://www.w3.org/2001/sw/).  Parliament's initial development
was funded by DARPA through the DAML program under the name
[DAML DB](http://www.daml.org/2001/09/damldb/) and was extended by RTX
BBN Technologies (BBN) for internal use in its R&D programs.  BBN released
Parliament as an open source project under the
[BSD license](http://opensource.org/licenses/bsd-license.php) on
[SemWebCentral](http://parliament.semwebcentral.org/) in 2009.  In 2018, BBN
migrated the Parliament open source project to
[GitHub](https://github.com/raytheonbbn/parliament) under the same license.

Parliament™ is a trademark of RTX BBN Technologies.  It is so named
because a group of owls is properly called a _parliament_ of owls.
