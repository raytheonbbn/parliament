// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge;

import java.io.File;
import java.util.List;

/*
import org.joseki.Joseki;
import org.joseki.http.ResponseHttpInitializer;
*/

import com.bbn.parliament.jena.bridge.configuration.ConfigurationHandler;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.query.PrefixRegistry;
import com.bbn.parliament.jena.util.JsonLdRdfReader;
import com.bbn.parliament.jena.util.JsonLdRdfWriter;

/**
 * A server for Parliament.
 *
 * @author rbattle
 * @author dkolas
 */
public class ParliamentBridge {
	private static ParliamentBridge _instance = null;

	private ParliamentBridgeConfiguration _config;

	/**
	 * Returns the singleton instance of the ParliamentBridge class.
	 *
	 * @throws IllegalStateException if the initialize method has not been called yet.
	 */
	public static ParliamentBridge getInstance() {
		if(_instance == null) {
			throw new IllegalStateException(
				"The ParliamentBridge class has not been initialized yet");
		}
		return _instance;
	}

	/** Initialize this instance using the given config file and the system tmp dir. */
	public static void initialize(String configurationFile) throws ParliamentBridgeException {
		initialize(configurationFile, null);
	}

	/** Initialize this instance using the given config file and tmp dir.  If the tmp dir is null, the system's tmp dir will be used instead. */
	public static void initialize(String configurationFile, File tmpDir) throws ParliamentBridgeException {
		// Add JSON-LD to the set of recognized RDF serializations:

		/*
		ResponseHttpInitializer.fixupHttpAcceptTypes();
		Joseki.setWriterType(JsonLdRdfWriter.contentType, JsonLdRdfWriter.formatName);
		Joseki.setReaderType(JsonLdRdfReader.contentType, JsonLdRdfReader.formatName);
		*/

		_instance = new ParliamentBridge(
			ParliamentBridgeConfiguration.readConfiguration(configurationFile, tmpDir));
	}

	/** This is private because ParliamentBridge is a singleton class. */
	private ParliamentBridge(ParliamentBridgeConfiguration config) {
		_config = config;

		/*
		PrefixRegistry.getInstance().registerPrefixes(config.getPrefixes());
		*/

		// initialize the model
		ModelManager modelManager = ModelManager.inst();
		for(ConfigurationHandler handler : _config.getConfigurationHandlers()) {
			handler.preModelInitialization(this, modelManager);
		}

		// load the model and any baseline files
		modelManager.initialize();

		for(ConfigurationHandler handler : _config.getConfigurationHandlers()) {
			handler.postModelInitialization(this, modelManager);
		}

		if(_config.isClearDataOnStartup()) {
			modelManager.clearKb();
		}
		for(String dir : _config.getBaselineDirs()) {
			modelManager.loadDirectory(dir);
		}
	}

	/** Returns the server's list of configuration handlers. */
	public List<ConfigurationHandler> getConfigurationHandlers() {
		return _config.getConfigurationHandlers();
	}

	public ParliamentBridgeConfiguration getConfiguration() {
		return _config;
	}

	@SuppressWarnings("static-method")
	public void stop() {
		Tracker.getInstance().shutdown();
		ModelManager.inst().closeKb();
	}
}
