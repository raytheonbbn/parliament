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
	private static class JettyServerCoreHolder {
		private static final JettyServerCore INSTANCE;

		static {
			try {
				INSTANCE = new JettyServerCore();
			} catch (Exception ex) {
				LOG.error("Parliament server encountered an exception", ex);
				throw new IllegalStateException("Parliament server encountered an exception", ex);
			}
		}
	}

	private static final String JETTY_CONF_SYS_PROP_NAME = "jettyConfig";
	private static final String CONF_JETTY_XML = "conf/jetty.xml";
	private static final Logger LOG = LoggerFactory.getLogger(JettyServerCore.class);

	private final Server server;

	//================================================================
	//
	// Static JettyServerCore API
	//
	//================================================================

	/**
	 * Get the singleton instance of JettyServerCore. This follows the "lazy
	 * initialization holder class" idiom for lazy initialization of a static field.
	 * See Item 83 of Effective Java, Third Edition, by Joshua Bloch for details.
	 *
	 * @return the instance
	 */
	public static JettyServerCore getInstance() {
		return JettyServerCoreHolder.INSTANCE;
	}

	//================================================================
	//
	// Non-static JettyServerCore API
	//
	//================================================================

	public void start() throws Exception {
		server.start();
		LOG.info("Starting Parliament server");
	}

	public void stop() {
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
	private JettyServerCore() throws Exception {
		server = new Server();

		String configPath = System.getProperty(JETTY_CONF_SYS_PROP_NAME, CONF_JETTY_XML);
		try (InputStream strm = new FileInputStream(configPath))
		{
			XmlConfiguration configuration = new XmlConfiguration(strm);
			configuration.configure(server);
			validateAndCreateTempDir(getTempDir());
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
