// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge.configuration;

import com.bbn.parliament.jena.bridge.ParliamentBridge;
import com.bbn.parliament.jena.graph.ModelManager;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A handler that processes a configuration.
 *
 * @author rbattle
 */
public interface ConfigurationHandler {
	/**
	 * Initialize this instance.
	 * @param handle the configuration resource.
	 * @throws ConfigurationException if an error occurs while initializing.
	 */
	public void initialize(Resource handle) throws ConfigurationException;

	/**
	 * Perform any configuration before the ModelManager initializes the model.
	 * @param server the server.
	 * @param manager the model manager.
	 */
	public void preModelInitialization(ParliamentBridge server, ModelManager manager);

	/**
	 * Perform any configuration after the ModelManager initializes the model.
	 * @param server the server.
	 * @param manager the model manager.
	 */
	public void postModelInitialization(ParliamentBridge server, ModelManager manager);
}
