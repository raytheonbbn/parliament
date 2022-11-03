// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import java.io.File;

import org.apache.jena.graph.Node;
import org.apache.jena.shared.uuid.JenaUUID;

import com.bbn.parliament.jena.graph.union.KbUnionGraph;
import com.bbn.parliament.jena.graph.union.KbUnionableGraph;
import com.bbn.parliament.jni.KbConfig;

/** @author sallen */
public class KbGraphFactory {
	private static final OptimizationMethod OPT_METHOD = OptimizationMethod
		//.DynamicOptimization;
		.DefaultOptimization;

	/** Create a Parliament default graph. */
	public static KbGraph createDefaultGraph() {
		KbConfig config = new KbConfig();
		config.readFromFile();
		return new KbGraph(config, null, OPT_METHOD);
	}

	/** Create a Parliament master graph. */
	public static KbGraph createMasterGraph() {
		return new KbGraph(getKbConfigForMasterGraph(), KbGraphStore.MASTER_GRAPH_DIR,
			OPT_METHOD);
	}

	/** Creates a Parliament graph with a GUID backing directory. */
	public static KbGraph createNamedGraph() {
		String guid = JenaUUID.generate().asString();
		return new KbGraph(getKbConfigForNamedGraph(guid), guid, OPT_METHOD);
	}

	/** Create a Parliament graph with files in the specified backing directory. */
	public static KbGraph createNamedGraph(String graphDir) {
		return new KbGraph(getKbConfigForNamedGraph(graphDir), graphDir, OPT_METHOD);
	}

	/** Create a Parliament union graph from the specified graphs. */
	public static KbUnionGraph createKbUnionGraph(
			KbUnionableGraph leftKbUnionableGraph, Node leftGraphName,
			KbUnionableGraph rightKbUnionableGraph, Node rightGraphName) {
		return new KbUnionGraph(leftKbUnionableGraph, leftGraphName, rightKbUnionableGraph, rightGraphName);
	}

	private static KbConfig getKbConfigForMasterGraph() {
		KbConfig config = new KbConfig();
		config.readFromFile();
		File kbDir = new File(config.m_kbDirectoryPath);
		File masterDir = new File(kbDir, KbGraphStore.MASTER_GRAPH_DIR);
		File oldMasterDir = new File(kbDir, KbGraphStore.OLD_MASTER_GRAPH_DIR);
		if (oldMasterDir.exists() && oldMasterDir.isDirectory() && !masterDir.exists()) {
			// The master directory used to be called "graphs", so rename it:
			oldMasterDir.renameTo(masterDir);
		}
		config.m_kbDirectoryPath = masterDir.getAbsolutePath();
		config.m_initialRsrcCapacity = 1000;
		config.m_initialStmtCapacity = 1000;
		return config;
	}

	private static KbConfig getKbConfigForNamedGraph(String namedGraphSubDir) {
		KbConfig config = new KbConfig();
		config.readFromFile();
		File kbDir = new File(config.m_kbDirectoryPath, namedGraphSubDir);
		config.m_kbDirectoryPath = kbDir.getAbsolutePath();
		return config;
	}
}
