// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import java.io.File;

import com.bbn.parliament.jena.graph.union.KbUnionGraph;
import com.bbn.parliament.jena.graph.union.KbUnionableGraph;
import com.bbn.parliament.jni.Config;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.uuid.JenaUUID;

/**
 * Ways to make graphs
 *
 * @author sallen
 */
public class KbGraphFactory {
	private static final OptimizationMethod OPT_METHOD = OptimizationMethod
	//.DynamicOptimization;
	.DefaultOptimization;

   /**
    * Create a Parliament default graph.
    */
   public static KbGraph createDefaultGraph() {
      return new KbGraph(getKbConfigForDefaultGraph(), null, OPT_METHOD);
   }

   /**
    * Create a Parliament default graph.
    */
   public static KbGraph createDefaultGraph(String graphDir) {
      return new KbGraph(getKbConfigForDefaultGraph(graphDir), null, OPT_METHOD);
   }

   /**
    * Create a Parliament master graph.
    */
   public static KbGraph createMasterGraph() {
      return new KbGraph(getKbConfigForMasterGraph(), KbGraphStore.MASTER_GRAPH_DIR,
            OPT_METHOD);
   }

   /**
    * Creates a Parliament graph with a GUID backing directory.
    */
   public static KbGraph createNamedGraph() {
      String guid = JenaUUID.generate().asString();
      return new KbGraph(getKbConfigForNamedGraph(guid), guid, OPT_METHOD);
   }

   /**
    * Create a Parliament graph with the specified backing directory.
    * @param graphDir the directory where the Parliament files are kept
    */
   public static KbGraph createNamedGraph(String graphDir) {
      return new KbGraph(getKbConfigForNamedGraph(graphDir), graphDir, OPT_METHOD);
   }

   /**
    * Create a Parliament union graph from the specified graphs.
    * @param leftKbUnionableGraph The left graph
    * @param rightKbUnionableGraph The right graph
    */
   public static KbUnionGraph createKbUnionGraph(KbUnionableGraph leftKbUnionableGraph, Node leftGraphName, KbUnionableGraph rightKbUnionableGraph, Node rightGraphName) {
      return new KbUnionGraph(leftKbUnionableGraph, leftGraphName, rightKbUnionableGraph, rightGraphName);
   }

   private static Config getKbConfigForDefaultGraph() {
      Config config = Config.readFromFile();
      new File(config.m_kbDirectoryPath).mkdirs();
      return config;
   }

   private static Config getKbConfigForDefaultGraph(String dir) {
      Config config = Config.readFromFile();
      config.m_kbDirectoryPath = dir;
      new File(config.m_kbDirectoryPath).mkdirs();
      return config;
   }

   private static Config getKbConfigForMasterGraph() {
      Config config = Config.readFromFile();
      File kbDir = new File(config.m_kbDirectoryPath);
      File masterDir = new File(kbDir, KbGraphStore.MASTER_GRAPH_DIR);
      File graphsDir = new File(kbDir, KbGraphStore.OLD_MASTER_GRAPH_DIR);
      if (graphsDir.exists() && graphsDir.isDirectory() && !masterDir.exists())
      {
      	// The master directory used to be called "graphs", so rename it
      	// so that we can correctly open the KB:
      	graphsDir.renameTo(masterDir);
      }
      else if (!masterDir.exists())
      {
	      masterDir.mkdirs();
      }
      config.m_kbDirectoryPath = masterDir.getAbsolutePath();
      config.m_initialRsrcCapacity = 1000;
      config.m_initialStmtCapacity = 1000;
      return config;
   }

   private static Config getKbConfigForNamedGraph(String namedGraphSubDir) {
      Config config = Config.readFromFile();
      File kbDir = new File(config.m_kbDirectoryPath, namedGraphSubDir);
      kbDir.mkdirs();
      config.m_kbDirectoryPath = kbDir.getAbsolutePath();
      return config;
   }
}
