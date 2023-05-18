// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.configuration;

import org.apache.jena.rdf.model.Resource;

import com.bbn.parliament.jena.joseki.bridge.ParliamentBridge;
import com.bbn.parliament.jena.joseki.graph.ModelManager;

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
