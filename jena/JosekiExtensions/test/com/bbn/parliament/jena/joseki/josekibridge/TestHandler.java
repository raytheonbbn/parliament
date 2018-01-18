// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.josekibridge;

import com.bbn.parliament.jena.joseki.bridge.ParliamentBridge;
import com.bbn.parliament.jena.joseki.bridge.configuration.ConfigurationException;
import com.bbn.parliament.jena.joseki.bridge.configuration.ConfigurationHandler;
import com.bbn.parliament.jena.joseki.graph.ModelManager;
import com.hp.hpl.jena.rdf.model.Resource;

/** @author Robert Battle */
public class TestHandler implements ConfigurationHandler {
	public static int COUNT = 0;

	public TestHandler() {
		++COUNT;
	}

	/** {@inheritDoc} */
	@Override
	public void initialize(Resource handle) throws ConfigurationException {
	}

	/** {@inheritDoc} */
	@Override
	public void postModelInitialization(ParliamentBridge server, ModelManager manager) {
	}

	/** {@inheritDoc} */
	@Override
	public void preModelInitialization(ParliamentBridge server, ModelManager manager) {
	}
}
