// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.josekibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.joseki.bridge.ParliamentBridge;
import com.bbn.parliament.jena.joseki.bridge.ParliamentBridgeException;
import com.bbn.parliament.jena.joseki.bridge.configuration.ConfigurationHandler;
import com.bbn.parliament.jena.joseki.bridge.configuration.IndexProcessorConfigurationHandler;
import com.bbn.parliament.jena.joseki.graph.ModelManager;

/** @author Robert Battle */
@RunWith(JUnitPlatform.class)
public class ParliamentServerTestCase {
	@SuppressWarnings("static-method")
	@BeforeEach
	public void setUp() throws Exception {
		ModelManager.inst().initialize();
		ModelManager.inst().clearKb();
		ModelManager.inst().closeKb();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testSingleIndex() throws ParliamentBridgeException {
		ParliamentBridge.initialize(
			"com/bbn/parliament/jena/joseki/josekibridge/parliament-single-index.ttl");
		ParliamentBridge instance = ParliamentBridge.getInstance();
		assertEquals(1, instance.getConfigurationHandlers().size());
		ConfigurationHandler handler = instance.getConfigurationHandlers().get(0);
		assertTrue(handler instanceof IndexProcessorConfigurationHandler);
		//IndexProcessorConfigurationHandler indexHandler = (IndexProcessorConfigurationHandler) handler;
		//assertEquals(1, indexHandler.getProcessors().size());
		//(ModelManager.inst().getDefaultModel().getGraph() instanceof IndexingGraph);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNoIndex() throws ParliamentBridgeException {
		ParliamentBridge.initialize(
			"com/bbn/parliament/jena/joseki/josekibridge/parliament-no-index.ttl");
		ParliamentBridge instance = ParliamentBridge.getInstance();
		assertEquals(0, instance.getConfigurationHandlers().size());
		//assertFalse(ModelManager.inst().getDefaultModel().getGraph() instanceof IndexingGraph);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInvalidIndexHandler() throws ParliamentBridgeException {
		ParliamentBridge.initialize(
			"com/bbn/parliament/jena/joseki/josekibridge/parliament-invalid-index-handler.ttl");
		ParliamentBridge instance = ParliamentBridge.getInstance();
		assertEquals(0, instance.getConfigurationHandlers().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInvalidHandler() throws ParliamentBridgeException {
		ParliamentBridge.initialize(
			"com/bbn/parliament/jena/joseki/josekibridge/parliament-invalid-handler.ttl");
		ParliamentBridge instance = ParliamentBridge.getInstance();
		assertEquals(0, instance.getConfigurationHandlers().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testMultipleHandler() throws ParliamentBridgeException {
		ParliamentBridge.initialize(
			"com/bbn/parliament/jena/joseki/josekibridge/parliament-multiple-handlers.ttl");
		ParliamentBridge instance = ParliamentBridge.getInstance();
		assertEquals(4, instance.getConfigurationHandlers().size());
		assertEquals(3, TestHandler.COUNT);
	}
}
