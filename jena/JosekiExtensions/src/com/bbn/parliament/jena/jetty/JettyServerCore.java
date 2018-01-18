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
import java.util.Collection;

import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyServerCore {
	private static final String JETTY_CONF_SYS_PROP_NAME = "jettyConfig";
	private static final String CONF_JETTY_XML = "conf/jetty.xml";

	private static Logger _log = LoggerFactory.getLogger(JettyServerCore.class);
	private static JettyServerCore _instance = null;

	private boolean _timeToExit;
	private Object _lock;
	private Server _server;

	//================================================================
	//
	// Static JettyServerCore API
	//
	//================================================================

	public static class InitException extends Exception {
		private static final long serialVersionUID = 876026899096741546L;

		public InitException(String message) {
			super(message);
		}

		public InitException(String fmt, Object... args) {
			super(String.format(fmt, args));
		}

		public InitException(Throwable cause, String message) {
			super(message, cause);
		}

		public InitException(Throwable cause, String fmt, Object... args) {
			super(String.format(fmt, args), cause);
		}
	}

	/** Initialize this instance using the given command-line arguments. */
	public static void initialize() throws InitException {
		_instance = new JettyServerCore();
	}

	/**
	 * Returns the singleton instance of the JettyServerCore class.
	 *
	 * @throws IllegalStateException if the initialize method has not been called yet.
	 */
	public static JettyServerCore getInstance() {
		if (_instance == null) {
			throw new IllegalStateException("The JettyServerCore class has not been initialized yet");
		}
		return _instance;
	}

	//================================================================
	//
	// Non-static JettyServerCore API
	//
	//================================================================

	public boolean isStarted() {
		return _server.isStarted();
	}

	public void start() throws Exception {
		try {
			_server.start();
			_log.info("Starting Parliament server");
			synchronized (_lock) {
				while (!_timeToExit) {
					try {
						_lock.wait(5000);
					} catch (InterruptedException ex) {
						// Do nothing
					}
				}
			}
			_log.info("Shutting down Parliament server");
		} finally {
			_server.stop();
			_log.info("Parliament server stopped");
		}
	}

	public void stop() {
		synchronized (_lock) {
			_timeToExit = true;
			_lock.notifyAll();
		}
		_log.info("Parliament server shutdown requested");
	}

	//================================================================
	//
	// JettyServerCore implementation
	//
	//================================================================

	/** This is private because JettyServerCore is a singleton class. */
	private JettyServerCore() throws InitException {
		_timeToExit = false;
		_lock = new Object();
		_server = new Server();

		String configPath = System.getProperty(JETTY_CONF_SYS_PROP_NAME, CONF_JETTY_XML);
		try (InputStream strm = new FileInputStream(configPath))
		{
			XmlConfiguration configuration = new XmlConfiguration(strm);
			configuration.configure(_server);

			File tempDir = getTempDir();
			if (tempDir == null) {
				_log.warn("Unable to find temp dir in Jetty configuration.");
			} else if (tempDir.exists() && !tempDir.isDirectory()) {
				_log.warn("Temp dir specified in Jetty configuration exists, but is not a directory:  {}", tempDir.getCanonicalPath());
			} else if (tempDir.exists()) {
				_log.info("Using existing temp dir:  {}", tempDir.getCanonicalPath());
			} else {
				_log.info("Creating temp dir as specified in Jetty configuration:  {}", tempDir.getCanonicalPath());
				tempDir.mkdirs();
			}
		} catch (Exception ex) {
			throw new InitException(ex, "Unable to apply server configuration \"%1$s\"", configPath);
		}
	}

	private File getTempDir() throws IOException {
		File result = null;
		Collection<DeploymentManager> dmList = _server.getBeans(DeploymentManager.class);
		_log.trace("DeploymentManager list on Jetty Server object has length {}", dmList.size());
		for (DeploymentManager dm : dmList) {
			Collection<WebAppProvider> wapList = dm.getBeans(WebAppProvider.class);
			_log.trace("WebAppProvider list on DeploymentManager has length {}", wapList.size());
			for (WebAppProvider wap : wapList) {
				result = wap.getTempDir();
				_log.trace("Temp directory:  '{}'", result.getPath());
				_log.trace("Absolute temp directory:  '{}'", result.getCanonicalPath());
			}
		}
		return result;
	}
}
