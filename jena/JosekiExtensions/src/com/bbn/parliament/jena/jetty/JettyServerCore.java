// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

	private final Server server;

	//================================================================
	//
	// Static JettyServerCore API
	//
	//================================================================

	/** Initialize this instance. */
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

	public boolean isCoreStarted() {
		return server.isStarted();
	}

	public void startCore() throws Exception {
		server.start();
		LOG.info("Starting Parliament server");
	}

	public void stopCore() {
		try {
			LOG.info("Shutting down Parliament server");
			server.stop();
			LOG.info("Parliament server stopped");
		} catch (Exception ex) {
			LOG.error("Error while stopping Parliament server", ex);
		}
	}

	//================================================================
	//
	// JettyServerCore implementation
	//
	//================================================================

	/** This is private because JettyServerCore is a singleton class. */
	private JettyServerCore() throws ServerInitException {
		server = new Server();

		String configPath = System.getProperty(JETTY_CONF_SYS_PROP_NAME, CONF_JETTY_XML);
		try (InputStream strm = new FileInputStream(configPath))
		{
			XmlConfiguration configuration = new XmlConfiguration(strm);
			configuration.configure(server);
			validateAndCreateTempDir(getTempDir());
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

	private static void validateAndCreateTempDir(File tempDir) throws IOException {
		if (tempDir == null) {
			LOG.warn("No temp dir found in Jetty configuration.");
		} else if (tempDir.exists() && !tempDir.isDirectory()) {
			LOG.warn("Jetty configuration's temp dir exists, but is not a directory:  {}", tempDir.getCanonicalPath());
		} else if (tempDir.exists()) {
			LOG.info("Using existing temp dir:  {}", tempDir.getCanonicalPath());
		} else {
			LOG.info("Creating Jetty configuration's temp dir:  {}", tempDir.getCanonicalPath());
			tempDir.mkdirs();
		}
	}
}
