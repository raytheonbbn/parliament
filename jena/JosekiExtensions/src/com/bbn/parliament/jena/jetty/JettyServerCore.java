// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyServerCore {
	private static final String JETTY_CONF_SYS_PROP_NAME = "jettyConfig";
	private static final String CONF_JETTY_XML = "conf/jetty.xml";
	private static final Logger LOG = LoggerFactory.getLogger(JettyServerCore.class);

	private static JettyServerCore instance = null;

	private boolean timeToExit;
	private Object lock;
	private Server server;

	//================================================================
	//
	// Static JettyServerCore API
	//
	//================================================================

	/** Initialize this instance using the given command-line arguments. */
	public static void initialize() throws ServerInitException {
		instance = new JettyServerCore();
	}

	/**
	 * Returns the singleton instance of the JettyServerCore class.
	 *
	 * @throws IllegalStateException if the initialize method has not been called yet.
	 */
	public static JettyServerCore getInstance() {
		if (instance == null) {
			throw new IllegalStateException("The JettyServerCore class has not been initialized yet");
		}
		return instance;
	}

	//================================================================
	//
	// Non-static JettyServerCore API
	//
	//================================================================

	public boolean isStarted() {
		return server.isStarted();
	}

	public void start() throws Exception {
		try {
			server.start();
			LOG.info("Starting Parliament server");
			synchronized (lock) {
				while (!timeToExit) {
					try {
						lock.wait(5000);
					} catch (InterruptedException ex) {
						// Do nothing
					}
				}
			}
			LOG.info("Shutting down Parliament server");
		} finally {
			server.stop();
			LOG.info("Parliament server stopped");
		}
	}

	public void stop() {
		synchronized (lock) {
			timeToExit = true;
			lock.notifyAll();
		}
		LOG.info("Parliament server shutdown requested");
	}

	//================================================================
	//
	// JettyServerCore implementation
	//
	//================================================================

	/** This is private because JettyServerCore is a singleton class. */
	private JettyServerCore() throws ServerInitException {
		timeToExit = false;
		lock = new Object();
		server = new Server();

		String configPath = System.getProperty(JETTY_CONF_SYS_PROP_NAME, CONF_JETTY_XML);
		try (InputStream strm = new FileInputStream(configPath))
		{
			XmlConfiguration configuration = new XmlConfiguration(strm);
			configuration.configure(server);

			File tempDir = getTempDir();
			if (tempDir == null) {
				LOG.warn("No temp dir found in Jetty configuration.");
			} else if (tempDir.exists() && !tempDir.isDirectory()) {
				LOG.warn("Temp dir specified in Jetty configuration exists, but is not a directory:  {}", tempDir.getCanonicalPath());
			} else if (tempDir.exists()) {
				LOG.info("Using existing temp dir:  {}", tempDir.getCanonicalPath());
			} else {
				LOG.info("Creating temp dir as specified in Jetty configuration:  {}", tempDir.getCanonicalPath());
				tempDir.mkdirs();
			}
		} catch (Exception ex) {
			throw new ServerInitException(ex, "Unable to apply server configuration \"%1$s\"", configPath);
		}
	}

	private File getTempDir() {
		List<File> tmpDirs = server.getBeans(DeploymentManager.class).stream()
			.flatMap(dm -> dm.getBeans(WebAppProvider.class).stream())
			.map(wap -> wap.getTempDir())
			.collect(Collectors.toList());
		if (tmpDirs.size() < 1) {
			return null;
		} else {
			if (tmpDirs.size() > 1) {
				String delimiter = String.format("%n   ");
				String tmpDirListing = tmpDirs.stream()
					.map(File::getPath)
					.collect(Collectors.joining(delimiter, delimiter, ""));
				LOG.warn("Multiple temp dirs found in Jetty configuration:{}", tmpDirListing);
			}
			return tmpDirs.get(0);
		}
	}
}
