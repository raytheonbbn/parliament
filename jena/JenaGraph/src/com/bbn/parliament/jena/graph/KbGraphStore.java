// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.union.KbUnionGraph;
import com.bbn.parliament.jena.graph.union.KbUnionableGraph;
import com.bbn.parliament.jena.joseki.client.StreamUtil;
import com.bbn.parliament.jni.KbConfig;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.core.DatasetGraphMap;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/** @author sallen */
public class KbGraphStore extends DatasetGraphMap implements GraphStore {
	public static final String PARLIAMENT_NS          = "http://parliament.semwebcentral.org/parliament#";
	public static final String MASTER_GRAPH           = PARLIAMENT_NS + "MasterGraph";
	public static final String GRAPH_CLASS            = PARLIAMENT_NS + "NamedGraph";
	public static final String GRAPH_DIR_PROPERTY     = PARLIAMENT_NS + "graphDirectory";
	public static final String UNION_GRAPH_CLASS      = PARLIAMENT_NS + "UnionGraph";
	public static final String LEFT_GRAPH_PROPERTY    = PARLIAMENT_NS + "leftGraph";
	public static final String RIGHT_GRAPH_PROPERTY   = PARLIAMENT_NS + "rightGraph";
	public static final String INDEXED_GRAPH          = PARLIAMENT_NS + "IndexedGraph";

	public static final String MASTER_GRAPH_DIR       = "master";
	public static final String OLD_MASTER_GRAPH_DIR   = "graphs";

	public static final Node   DEFAULT_GRAPH_NODE     = Quad.defaultGraphIRI;
	public static final String DEFAULT_GRAPH_URI      = DEFAULT_GRAPH_NODE.getURI();
	public static final String DEFAULT_GRAPH_BASENAME = "Default Graph";

	private static Logger log = LoggerFactory.getLogger(KbGraphStore.class);

	public KbGraphStore(KbGraph defaultGraph) {
		super(defaultGraph);
	}

	public void initialize() {
		@SuppressWarnings("resource")
		Graph masterGraph = KbGraphFactory.createMasterGraph();
		addGraph(Node.createURI(MASTER_GRAPH), masterGraph, MASTER_GRAPH_DIR, false);

		if (isIndexingEnabled(DEFAULT_GRAPH_NODE)) {
			@SuppressWarnings("resource")
			Graph graph = getDefaultGraph();
			if (!IndexManager.getInstance().hasIndexes(graph)) {
				IndexManager.getInstance().createAndRegisterAll(graph, DEFAULT_GRAPH_NODE);
				IndexManager.getInstance().rebuild(graph);
			}
		}
		// Load all of the existing named graphs
		ExtendedIterator<Triple> it = masterGraph.find(null, RDF.Nodes.type, Node.createURI(GRAPH_CLASS));
		try {
			while (it.hasNext()) {
				Triple triple = it.next();

				Node graphName = triple.getSubject();
				String graphDir = getGraphDir(graphName);
				@SuppressWarnings("resource")
				Graph graph = KbGraphFactory.createNamedGraph(graphDir);
				if (isIndexingEnabled(graphName)) {
					IndexManager.getInstance().createAndRegisterAll(graph, graphName);
					//IndexManager.getInstance().rebuild(graph);
				}

				addGraph(graphName, graph, graphDir, false);
			}
		} finally {
			closeQuietly(it);
		}

		// Load any existing union graphs
		it = masterGraph.find(null, RDF.Nodes.type, Node.createURI(UNION_GRAPH_CLASS));
		try {
			while (it.hasNext()) {
				Triple triple = it.next();

				Node graphName = triple.getSubject();
				Node leftGraphName = getOneTriple(masterGraph, graphName, Node.createURI(LEFT_GRAPH_PROPERTY), null).getObject();
				Node rightGraphName = getOneTriple(masterGraph, graphName, Node.createURI(RIGHT_GRAPH_PROPERTY), null).getObject();

				addUnionGraph(graphName, leftGraphName, rightGraphName, false);
			}
		} finally {
			closeQuietly(it);
		}
	}

	/** Get the default graph's configuration. */
	@SuppressWarnings("resource")
	public KbConfig getDefaultGraphConfig() {
		return getDefaultGraph().getConfig();
	}

	/** Get the default graph. */
	@Override
	public KbGraph getDefaultGraph() {
		return (KbGraph) super.getDefaultGraph();
	}

	/** Get the master graph, which contains references to all named graphs. */
	public Graph getMasterGraph() {
		return getGraph(Node.createURI(MASTER_GRAPH));
	}

	/** {@inheritDoc} */
	@Override
	protected Graph getGraphCreate() {
		return KbGraphFactory.createNamedGraph();
	}

	/** {@inheritDoc} */
	@Override
	public void addGraph(Node graphName, Graph graph) {
		Graph toAdd = graph;
		// If the graph already exists, then we delete it and replace it with the new graph
		if (containsGraph(graphName)) {
			removeGraph(graphName);
		}

		// If we are passed a non-Parliament graph, then create a new KbGraph and copy all the statements into it
		if (!((toAdd instanceof KbGraph) || (toAdd instanceof KbUnionGraph))) {
			//throw new JenaException(String.format("KbGraphStore can only accept KbGraphs or KbUnionGraphs.  You tried to add: %1$s", graph.getClass().getName()));
			Graph kbGraph = getGraphCreate();
			kbGraph.getBulkUpdateHandler().add(toAdd);
			toAdd = kbGraph;
		}

		if (toAdd instanceof KbGraph) {
			@SuppressWarnings("resource")
			KbGraph kbGraph = (KbGraph)toAdd;
			addGraph(graphName, kbGraph, kbGraph.getRelativeDirectory(), true);
		}
		else if (toAdd instanceof KbUnionGraph) {
			KbUnionGraph kbUnionGraph = (KbUnionGraph)toAdd;
			addUnionGraph(graphName, kbUnionGraph.getLeftGraphName(), kbUnionGraph.getRightGraphName(), true);
		}
	}

	protected void addGraph(Node graphName, Graph graph, String graphDir, boolean addStatementsToMasterGraph) {
		// Only add a graph if it doesn't exist
		if (!containsGraph(graphName)) {
			log.debug(String.format("Adding named graph: <%1$s> (graphDir = \"%2$s\")", graphName.getURI(), graphDir));
			super.addGraph(graphName, graph);
			if (addStatementsToMasterGraph) {
				Graph masterGraph = getMasterGraph();
				masterGraph.add(Triple.create(graphName, RDF.Nodes.type, Node.createURI(GRAPH_CLASS)));
				masterGraph.add(Triple.create(graphName, Node.createURI(GRAPH_DIR_PROPERTY), Node.createLiteral(graphDir)));
			}
		}
	}

	public boolean addUnionGraph(Node graphName, Node leftGraphName, Node rightGraphName) {
		return addUnionGraph(graphName, leftGraphName, rightGraphName, true);
	}

	private boolean addUnionGraph(Node graphName, Node leftGraphName, Node rightGraphName, boolean addStatementsToMasterGraph)
	{
		// Only add a graph if it doesn't exist
		if (!containsGraph(graphName))
		{
			Graph leftGraph = getGraph(leftGraphName);
			Graph rightGraph = getGraph(rightGraphName);

			if (leftGraph != null && rightGraph != null)
			{
				if (leftGraph instanceof KbUnionableGraph
					&& rightGraph instanceof KbUnionableGraph)
				{
					KbUnionableGraph leftKbUnionableGraph = (KbUnionableGraph) leftGraph;
					KbUnionableGraph rightKbUnionableGraph = (KbUnionableGraph) rightGraph;

					KbUnionGraph unionGraph = KbGraphFactory.createKbUnionGraph(leftKbUnionableGraph, leftGraphName, rightKbUnionableGraph, rightGraphName);

					log.debug(String.format("Adding union graph: <%1$s> (left: <%2$s>  right: <%3$s>)", graphName.getURI(), leftGraphName.getURI(), rightGraphName.getURI()));

					if (addStatementsToMasterGraph) {
						Graph masterGraph = getMasterGraph();
						masterGraph.add(Triple.create(graphName, RDF.Nodes.type, Node.createURI(UNION_GRAPH_CLASS)));
						masterGraph.add(Triple.create(graphName, Node.createURI(LEFT_GRAPH_PROPERTY), leftGraphName));
						masterGraph.add(Triple.create(graphName, Node.createURI(RIGHT_GRAPH_PROPERTY), rightGraphName));
					}

					addGraph(graphName, unionGraph, null, false);
				}
				return true;
			}
			return false;
		}
		return true;
	}

	/** Clear the store and remove all graphs. */
	public void clear() {
		clear(false);
	}

	/** Clear the store and remove all graphs. */
	public void clear(boolean deleteContainingDirectory) {
		File containingDir = new File(getDefaultGraphConfig().m_kbDirectoryPath);

		// We create a clone of the graph names so that as the iteration below removes
		// elements from the named graphs collection, the iteration is not affected.
		List<Node> graphNames = StreamUtil.asStream(listGraphNodes())
			.collect(Collectors.toList());

		// Delete all of the KbUnionGraphs first
		graphNames.stream()
			.filter(graphName -> getGraph(graphName) instanceof KbUnionGraph)
			.forEach(graphName -> removeGraph(graphName));

		// Then delete all the named graphs except the master graph:
		graphNames.stream()
			.filter(graphName -> !MASTER_GRAPH.equals(graphName.getURI()))
			.filter(graphName -> getGraph(graphName) instanceof KbGraph)
			.forEach(graphName -> removeGraph(graphName));

		// Remove the default graph
		removeGraph(null);

		// Finally, delete the master graph:
		removeGraph(Node.createURI(MASTER_GRAPH));

		if (deleteContainingDirectory) {
			deleteDirectory(containingDir);
		}
	}

	/**
	 * Deletes any of the named graphs, including the default graph and
	 * master graph.  All aspects of the graph will be deleted, including its
	 * entry in the master graph, its model, its files, and its sub-
	 * directory.  Note that attempting to delete the master graph if named
	 * graphs still exist will throw an exception.
	 *
	 * @param graphName The URI of the graph to delete, or the empty string
	 * for the default graph.
	 */
	@SuppressWarnings("resource")
	@Override
	public void removeGraph(Node graphName) {
		Graph toReturn = null;
		boolean isKbGraph = true;

		if (isDefaultGraphName(graphName)) {
			// Remove the default graph
			toReturn = getDefaultGraph();
			setDefaultGraph(null);
		} else if (MASTER_GRAPH.equals(graphName.getURI())) {
			if (StreamUtil.asStream(listGraphNodes()).anyMatch(gn -> !MASTER_GRAPH.equals(gn.getURI()))) {
				throw new JenaException("You cannot delete the master graph while other named graphs still exist!");
			}

			toReturn = getGraph(graphName);
			super.removeGraph(graphName);
		} else {
			Graph graph = getGraph(graphName);
			isKbGraph = (graph instanceof KbGraph);

			// Make sure we don't delete a member of a union graph
			for (Iterator<Node> iter = listGraphNodes(); iter.hasNext(); )
			{
				Node gName = iter.next();
				Graph g = getGraph(gName);
				if (g instanceof KbUnionGraph) {
					KbUnionGraph unionGraph = (KbUnionGraph)g;
					if (graphName.equals(unionGraph.getLeftGraphName()) || graphName.equals(unionGraph.getRightGraphName())) {
						throw new JenaException(String.format(
							"Cannot delete a named graph while it is a member of a KbUnionGraph.  Delete union graph <%1$s> first.",
							gName));
					}
				}
			}

			toReturn = getGraph(graphName);
			super.removeGraph(graphName);

			Graph masterGraph = getMasterGraph();
			List<Triple> triplesToRemoveFromMaster = new ArrayList<>(2);

			ExtendedIterator<Triple> it = getMasterGraph().find(graphName, null, null);
			try {
				while (it.hasNext()) {
					Triple t = it.next();
					triplesToRemoveFromMaster.add(t);
				}
			} finally {
				closeQuietly(it);
			}

			for (Triple t : triplesToRemoveFromMaster) {
				masterGraph.delete(t);
			}
		}

		if (log.isDebugEnabled()) {
			String graphDisplayName = (null == graphName) ? "default-graph" : graphName.getURI();
			if (isKbGraph) {
				KbConfig config = ((KbGraph)toReturn).getConfig();
				File kbDir = new File(config.m_kbDirectoryPath);
				log.debug("Deleting KbGraph <{}> (graphDir = \"{}\")", graphDisplayName, kbDir.getPath());
			} else {
				log.debug("Deleting union graph <{}>", graphDisplayName);
			}
		}

		// Close the graph
		toReturn.close();

		// delete indexes
		IndexManager.getInstance().unregisterAll(toReturn, graphName);

		// Delete the files
		if (isKbGraph) {
			KbConfig config = ((KbGraph)toReturn).getConfig();
			File kbDir = new File(config.m_kbDirectoryPath);
			(new File(kbDir, config.m_rsrcFileName)).delete();
			(new File(kbDir, config.m_stmtFileName)).delete();
			(new File(kbDir, config.m_uriTableFileName)).delete();
			(new File(kbDir, config.m_uriToIntFileName)).delete();

			if (!isDefaultGraphName(graphName)) {
				deleteDirectory(kbDir);
			}
		}
	}

	private static void deleteDirectory(File dir) {
		try {
			// Sometimes Linux takes a short time to fully execute file deletions from
			// this directory, so give it a little breathing room:
			Thread.sleep(250);
		} catch (InterruptedException ex) {
			log.error("InterruptedException:", ex);
		}

		// Delete the directory:
		String[] dirContents = dir.list();
		if (dirContents == null) {
			log.debug("'{}' is not a directory, or an I/O error occurred", dir.getAbsolutePath());
		} else if (dirContents.length != 0) {
			log.debug("'{}' is not empty:  {}", dir.getAbsolutePath(),
				Arrays.stream(dirContents).collect(Collectors.joining("', '", "'", "'")));
		} else {
			boolean success = dir.delete();
			log.debug("Deleted graph dir '{}':  {}", dir.getAbsolutePath(), success);
		}
	}

	/**
	 * Returns the relative directory where the graph's files are stored.
	 * Will return null for the default graph.
	 */
	public String getGraphDir(Node graphName) {
		String toReturn = null;
		if (!isDefaultGraphName(graphName) && MASTER_GRAPH.equals(graphName.getURI())) {
			toReturn = MASTER_GRAPH_DIR;
		} else {
			toReturn = getOneTriple(getMasterGraph(), graphName, Node.createURI(GRAPH_DIR_PROPERTY), null).getObject().getLiteralLexicalForm();
		}
		return toReturn;
	}

	/** Flush all graphs. */
	public void flush() {
		@SuppressWarnings("resource")
		Graph defaultGraph = getDefaultGraph();
		if (null != defaultGraph) {
			flushGraph(defaultGraph, null);
		}

		StreamUtil.asStream(listGraphNodes())
			.forEach(graphName -> flushGraph(getGraph(graphName), graphName.getURI()));
	}

	private KbGraph getInnerKbGraph(Graph graph) {
		if (graph instanceof KbGraph) {
			return (KbGraph) graph;
		} else if (graph instanceof InfGraph) {
			Graph rawGraph = ((InfGraph) graph).getRawGraph();
			return getInnerKbGraph(rawGraph);
		} else {
			return null;
		}
	}

	private void flushGraph(Graph graph, String graphName) {
		@SuppressWarnings("resource")
		KbGraph kbGraph = getInnerKbGraph(graph);
		if (kbGraph != null) {
			long start = 0;
			if (log.isDebugEnabled()) {
				start = System.currentTimeMillis();
			}
			kbGraph.flush();
			//IndexManager.getInstance().flush(graph);
			if (log.isDebugEnabled()) {
				long duration = System.currentTimeMillis() - start;
				String graphDisplayName = (graphName == null || graphName.isEmpty())
					? "default" : graphName;
				log.debug("Flushed graph <{}> in {} ms", graphDisplayName, duration);
			}
		}
	}

	public void setIndexingEnabled(Node graphName, boolean enabled) {
		Node name = graphName;
		if (isDefaultGraphName(name)) {
			name = Node.createURI(DEFAULT_GRAPH_URI);
		}

		Graph masterGraph = getMasterGraph();
		Triple t = Triple.create(name, RDF.Nodes.type, Node.createURI(INDEXED_GRAPH));
		if (enabled) {
			masterGraph.add(t);
		}
		else {
			masterGraph.delete(t);
		}
	}

	private boolean isIndexingEnabled(Node graphName) {
		Node name = graphName;
		if (isDefaultGraphName(name)) {
			name = Node.createURI(DEFAULT_GRAPH_URI);
		}
		Graph masterGraph = getMasterGraph();
		ExtendedIterator<Triple> it = masterGraph.find(name, RDF.Nodes.type, Node.createURI(INDEXED_GRAPH));
		try {
			return it.hasNext();
		}
		finally {
			closeQuietly(it);
		}
	}

	private static boolean isDefaultGraphName(Node graphName) {
		return (null == graphName)
			|| (graphName.getURI() == null)
			|| ("".equals(graphName.getURI()))
			|| (DEFAULT_GRAPH_NODE.equals(graphName));
	}

	/**
	 * Retrieves a single triple matching the pattern given.  If there is more than one
	 * triple matching the pattern, the first one found is returned, and a warning is logged.
	 *
	 * If the triple doesn't exist, then null is returned and a warning is logged.
	 *
	 * @param graph The graph to search
	 * @param s The subject Node
	 * @param p The predicate Node
	 * @param o The object Node
	 * @return A triple matching the pattern or null if it doesn't exist.
	 */
	public static Triple getOneTriple(Graph graph, Node s, Node p, Node o)
	{
		Triple toReturn = null;

		ExtendedIterator<Triple> it = null;
		try
		{
			it = graph.find(s, p, o);

			if (it.hasNext())
			{
				toReturn = it.next();

				// Sanity check
				if (it.hasNext())
				{
					log.warn(String.format("Found multiple triples matching the pattern: %1$s %2$s %3$s", s, p, o));
				}
			}
			else
			{
				log.warn(String.format("Found zero triples matching the pattern: %1$s %2$s %3$s", s, p, o));
			}
		}
		finally
		{
			closeQuietly(it);
		}

		return toReturn;
	}

	/** Quietly closes the given iterator.  Does nothing if the iterator is null. */
	public static void closeQuietly(ClosableIterator<?> it)
	{
		if (null != it)
		{
			it.close();
		}
	}


	// Note: the implementation of the following 4 methods comes from GraphStoreBasic

	/** {@inheritDoc} */
	@Override
	public Dataset toDataset() {
		return DatasetImpl.wrap(this);
	}

	/** {@inheritDoc} */
	@Override
	public void startRequest(UpdateRequest request) {
	}

	/** {@inheritDoc} */
	@Override
	public void finishRequest(UpdateRequest request) {
	}

	/** {@inheritDoc} */
	@SuppressWarnings("resource")
	@Override
	public void close() {
		StreamUtil.asStream(listGraphNodes())
			.map(graphName -> getGraph(graphName))
			.forEach(KbGraphStore::closeGraph);
		KbGraph defaultGraph = getDefaultGraph();
		closeGraph(defaultGraph);
	}

	private static void closeGraph(Graph graph) {
		if (graph != null) {
			IndexManager.getInstance().closeAll(graph);	// close all indexes
			graph.close();
		}
	}
}
