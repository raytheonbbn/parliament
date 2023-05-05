// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.IteratorConcat;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraphTriplesQuads;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.graph.GraphUtils;
import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.client.StreamUtil;
import com.bbn.parliament.core.jni.KbConfig;
import com.bbn.parliament.kb_graph.index.IndexManager;
import com.bbn.parliament.kb_graph.union.KbUnionGraph;
import com.bbn.parliament.kb_graph.union.KbUnionableGraph;

public class KbGraphStore extends DatasetGraphTriplesQuads {
	public static final String PARLIAMENT_NS = "http://parliament.semwebcentral.org/parliament#";
	public static final Node MASTER_GRAPH = createNode("MasterGraph");
	public static final Node GRAPH_CLASS = createNode("NamedGraph");
	public static final Node GRAPH_DIR_PROPERTY = createNode("graphDirectory");
	public static final Node UNION_GRAPH_CLASS = createNode("UnionGraph");
	public static final Node LEFT_GRAPH_PROPERTY = createNode("leftGraph");
	public static final Node RIGHT_GRAPH_PROPERTY = createNode("rightGraph");
	public static final Node INDEXED_GRAPH = createNode("IndexedGraph");
	public static final Node DEFAULT_GRAPH_NODE = Quad.defaultGraphIRI;

	public static final String MASTER_GRAPH_DIR = "master";
	public static final String OLD_MASTER_GRAPH_DIR = "graphs";
	public static final String DEFAULT_GRAPH_BASENAME = "Default Graph";

	private static final Logger LOG = LoggerFactory.getLogger(KbGraphStore.class);

	private static Node createNode(String localName) {
		return NodeFactory.createURI(PARLIAMENT_NS + localName);
	}

	private KbGraph defaultGraph ;
	private final Map<Node, Graph> graphs;

	public KbGraphStore(KbGraph defaultGraph) {
		this.defaultGraph = defaultGraph;
		graphs = new HashMap<>();
	}

	public void initialize() {
		@SuppressWarnings("resource")
		var masterGraph = KbGraphFactory.createMasterGraph();
		addGraph(MASTER_GRAPH, masterGraph, MASTER_GRAPH_DIR, false);

		if (isIndexingEnabled(DEFAULT_GRAPH_NODE)) {
			if (!IndexManager.getInstance().hasIndexes(defaultGraph)) {
				IndexManager.getInstance().createAndRegisterAll(defaultGraph, DEFAULT_GRAPH_NODE);
				IndexManager.getInstance().rebuild(defaultGraph);
			}
		}
		// Load all of the existing named graphs
		var it = masterGraph.find(null, RDF.Nodes.type, GRAPH_CLASS);
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
		it = masterGraph.find(null, RDF.Nodes.type, UNION_GRAPH_CLASS) ;
		try {
			while (it.hasNext()) {
				Triple triple = it.next();

				Node graphName = triple.getSubject();
				Node leftGraphName = getOneTriple(masterGraph, graphName, LEFT_GRAPH_PROPERTY, null).getObject();
				Node rightGraphName = getOneTriple(masterGraph, graphName, RIGHT_GRAPH_PROPERTY, null).getObject();

				addUnionGraph(graphName, leftGraphName, rightGraphName, false);
			}
		} finally {
			closeQuietly(it);
		}
	}

	@Override
	public Iterator<Node> listGraphNodes() {
		return graphs.keySet().iterator();
	}

	@Override
	public boolean supportsTransactions() {
		return false;
	}

	@Override
	public void begin(ReadWrite readWrite) {
	}

	@Override
	public void commit() {
	}

	@Override
	public void abort() {
	}

	@Override
	public void end() {
	}

	@Override
	public boolean isInTransaction() {
		return false;
	}

	@Override
	protected void addToDftGraph(Node s, Node p, Node o) {
		defaultGraph.add(Triple.create(s, p, o));
	}

	@Override
	protected void addToNamedGraph(Node g, Node s, Node p, Node o) {
		getGraph(g).add(Triple.create(s, p, o));
	}

	@Override
	protected void deleteFromDftGraph(Node s, Node p, Node o) {
		defaultGraph.delete(Triple.create(s, p, o));
	}

	@Override
	protected void deleteFromNamedGraph(Node g, Node s, Node p, Node o) {
		getGraph(g).delete(Triple.create(s, p, o));
	}

	@Override
	protected Iterator<Quad> findInDftGraph(Node s, Node p, Node o) {
		var iter = defaultGraph.find(s, p, o);
		return GraphUtils.triples2quadsDftGraph(iter);
	}

	@Override
	protected Iterator<Quad> findInSpecificNamedGraph(Node g, Node s, Node p, Node o) {
		var iter = getGraph(g).find(s, p, o);
		return GraphUtils.triples2quads(g, iter);
	}

	@Override
	protected Iterator<Quad> findInAnyNamedGraphs(Node s, Node p, Node o) {
		var iter = new IteratorConcat<Quad>();
		StreamUtil.asStream(listGraphNodes())
			.map(graphName -> findInSpecificNamedGraph(graphName, s, p, o))
			.filter(Objects::nonNull)
			.forEach(iterWithinGraph -> iter.add(iterWithinGraph));
		return iter;
	}

	@Override
	public KbGraph getDefaultGraph() {
		return defaultGraph;
	}

	public KbConfig getDefaultGraphConfig() {
		return defaultGraph.getConfig();
	}

	/** Get the master graph, which contains references to all named graphs. */
	public Graph getMasterGraph() {
		return getGraph(MASTER_GRAPH);
	}

	@Override
	public Graph getGraph(Node graphNode) {
		if (Quad.isDefaultGraph(graphNode)) {
			return defaultGraph;
		}
		return graphs.computeIfAbsent(graphNode, key -> KbGraphFactory.createNamedGraph());
	}

	@Override
	public boolean containsGraph(Node graphNode) {
		return graphs.containsKey(graphNode);
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
		if (!(toAdd instanceof KbGraph) && !(toAdd instanceof KbUnionGraph)) {
			@SuppressWarnings("resource")
			KbGraph kbGraph = KbGraphFactory.createNamedGraph();
			GraphUtil.addInto(kbGraph, toAdd);
			toAdd = kbGraph;
		}

		if (toAdd instanceof KbGraph kbGraph) {
			addGraph(graphName, kbGraph, kbGraph.getRelativeDirectory(), true);
		} else if (toAdd instanceof KbUnionGraph kbUnionGraph) {
			addUnionGraph(graphName, kbUnionGraph.getLeftGraphName(), kbUnionGraph.getRightGraphName(), true);
		}
	}

	private void addGraph(Node graphName, Graph graph, String graphDir, boolean addStatementsToMasterGraph) {
		// Only add a graph if it doesn't exist
		if (!containsGraph(graphName)) {
			LOG.debug("Adding named graph: <{}> (graphDir = '{}')", graphName.getURI(), graphDir);
			graphs.put(graphName, graph);
			if (addStatementsToMasterGraph) {
				Graph masterGraph = getMasterGraph();
				masterGraph.add(Triple.create(graphName, RDF.Nodes.type, GRAPH_CLASS));
				masterGraph.add(Triple.create(graphName, GRAPH_DIR_PROPERTY, NodeFactory.createLiteral(graphDir)));
			}
		}
	}

	public boolean addUnionGraph(Node graphName, Node leftGraphName, Node rightGraphName) {
		return addUnionGraph(graphName, leftGraphName, rightGraphName, true);
	}

	private boolean addUnionGraph(Node graphName, Node leftGraphName, Node rightGraphName,
			boolean addStatementsToMasterGraph) {
		// Only add a graph if it doesn't exist
		if (!containsGraph(graphName)) {
			Graph leftGraph = getGraph(leftGraphName);
			Graph rightGraph = getGraph(rightGraphName);

			if (leftGraph != null && rightGraph != null) {
				if (leftGraph instanceof KbUnionableGraph leftKbUnionableGraph
					&& rightGraph instanceof KbUnionableGraph rightKbUnionableGraph) {

					KbUnionGraph unionGraph = KbGraphFactory.createKbUnionGraph(
						leftKbUnionableGraph, leftGraphName, rightKbUnionableGraph, rightGraphName);

					LOG.debug("Adding union graph: <{}> (left: <{}>  right: <{}>)",
						graphName.getURI(), leftGraphName.getURI(), rightGraphName.getURI());

					if (addStatementsToMasterGraph) {
						Graph masterGraph = getMasterGraph();
						masterGraph.add(Triple.create(graphName, RDF.type.asNode(), UNION_GRAPH_CLASS));
						masterGraph.add(Triple.create(graphName, LEFT_GRAPH_PROPERTY, leftGraphName));
						masterGraph.add(Triple.create(graphName, RIGHT_GRAPH_PROPERTY, rightGraphName));
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
	@Override
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
			.filter(graphName -> !MASTER_GRAPH.equals(graphName))
			.filter(graphName -> getGraph(graphName) instanceof KbGraph)
			.forEach(graphName -> removeGraph(graphName));

		// Remove the default graph
		removeGraph(null);

		// Finally, delete the master graph:
		removeGraph(MASTER_GRAPH);

		if (deleteContainingDirectory) {
			deleteDirectory(containingDir);
		}
	}

	/**
	 * Deletes any of the contained graphs, including the default graph and master
	 * graph. All aspects of the graph will be deleted, including its entry in the
	 * master graph, its model, its files, and its sub-directory. Attempting to
	 * delete the master graph will throw an exception if named graphs still exist.
	 *
	 * @param graphName The URI of the graph to delete, or the empty string for the
	 *                  default graph.
	 */
	@Override
	public void removeGraph(Node graphName) {
		Graph graphToDelete = null;

		if (isDefaultGraphName(graphName)) {
			// Remove the default graph
			graphToDelete = defaultGraph;
			defaultGraph = null;
		} else if (MASTER_GRAPH.equals(graphName)) {
			// Check for any graph name other than MASTER_GRAPH:
			if (StreamUtil.asStream(listGraphNodes()).anyMatch(gn -> !MASTER_GRAPH.equals(gn))) {
				throw new JenaException(
					"You cannot delete the master graph while other named graphs still exist!");
			}

			graphToDelete = graphs.remove(graphName);
		} else {
			// Make sure we don't delete a member of a union graph
			StreamUtil.asStream(listGraphNodes())
				.filter(gn -> isMemberOfUnion(graphName, gn))
				.findAny()
				.ifPresent(gn -> {
					throw new JenaException("""
						Cannot delete a named graph while it is a member of a union. \
						Delete union graph <%1$s> first.""".formatted(gn.getURI()));
				});

			graphToDelete = graphs.remove(graphName);
			removeGraphFromMaster(graphName);
		}

		if (graphToDelete != null) {
			if (LOG.isDebugEnabled()) {
				String graphDisplayName = isDefaultGraphName(graphName) ? "default-graph" : graphName.getURI();
				if (graphToDelete instanceof KbGraph kbGraphToDelete) {
					LOG.debug("Deleting KbGraph <{}> (graphDir = \"{}\")", graphDisplayName,
						kbGraphToDelete.getConfig().m_kbDirectoryPath);
				} else {
					LOG.debug("Deleting union graph <{}>", graphDisplayName);
				}
			}

			// Close the graph
			graphToDelete.close();

			// Delete its indexes
			IndexManager.getInstance().unregisterAll(graphToDelete, graphName);

			// Delete its files
			if (graphToDelete instanceof KbGraph kbGraphToDelete) {
				kbGraphToDelete.deleteUnderlyingFiles(!isDefaultGraphName(graphName));
			}
		}
	}

	private boolean isMemberOfUnion(Node graphName, Node candidateUnionGraphName) {
		if (getGraph(candidateUnionGraphName) instanceof KbUnionGraph unionGraph) {
			if (graphName.equals(unionGraph.getLeftGraphName())
					|| graphName.equals(unionGraph.getRightGraphName())) {
				return true;
			}
		}
		return false;
	}

	private void removeGraphFromMaster(Node graphName) {
		Graph masterGraph = getMasterGraph();
		List<Triple> triplesToRemoveFromMaster = new ArrayList<>();
		ExtendedIterator<Triple> it = masterGraph.find(graphName, null, null);
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

	private static void deleteDirectory(File dir) {
		try {
			// Sometimes Linux takes a short time to fully execute file deletions from
			// this directory, so give it a little breathing room:
			Thread.sleep(250);
		} catch (InterruptedException ex) {
			LOG.error("InterruptedException:", ex);
		}

		// Delete the directory:
		String[] dirContents = dir.list();
		if (dirContents == null) {
			LOG.debug("'{}' is not a directory, or an I/O error occurred", dir.getAbsolutePath());
		} else if (dirContents.length != 0) {
			LOG.debug("'{}' is not empty:  {}", dir.getAbsolutePath(),
				Stream.of(dirContents).collect(Collectors.joining("', '", "'", "'")));
		} else {
			boolean success = dir.delete();
			LOG.debug("Deleted graph dir '{}':  {}", dir.getAbsolutePath(), success);
		}
	}

	/**
	 * Returns the relative directory where the graph's files are stored.
	 * Will return null for the default graph.
	 */
	public String getGraphDir(Node graphName) {
		if (isDefaultGraphName(graphName)) {
			return null;
		} else if (MASTER_GRAPH.equals(graphName)) {
			return MASTER_GRAPH_DIR;
		} else {
			return getOneTriple(getMasterGraph(), graphName, GRAPH_DIR_PROPERTY, null)
				.getObject().getLiteralLexicalForm();
		}
	}

	/** Flush all graphs. */
	public void flush() {
		if (null != defaultGraph) {
			flushGraph(defaultGraph);
		}

		StreamUtil.asStream(listGraphNodes())
			.forEach(graphName -> flushGraph(getGraph(graphName)));
	}

	private void flushGraph(Graph graph) {
		@SuppressWarnings("resource")
		KbGraph kbGraph = getInnerKbGraph(graph);
		if (kbGraph != null) {
			kbGraph.flush();
			//IndexManager.getInstance().flush(graph);
		}
	}

	private KbGraph getInnerKbGraph(Graph graph) {
		if (graph instanceof KbGraph kbGraph) {
			return kbGraph;
		} else if (graph instanceof InfGraph infGraph) {
			Graph rawGraph = infGraph.getRawGraph();
			return getInnerKbGraph(rawGraph);
		} else {
			return null;
		}
	}

	public void setIndexingEnabled(Node graphName, boolean enabled) {
		Node name = isDefaultGraphName(graphName) ? DEFAULT_GRAPH_NODE : graphName;
		Graph masterGraph = getMasterGraph();
		Triple t = Triple.create(name, RDF.Nodes.type, INDEXED_GRAPH);
		if (enabled) {
			masterGraph.add(t);
		} else {
			masterGraph.delete(t);
		}
	}

	private boolean isIndexingEnabled(Node graphName) {
		Node name = isDefaultGraphName(graphName) ? DEFAULT_GRAPH_NODE : graphName;
		Graph masterGraph = getMasterGraph();
		ExtendedIterator<Triple> it = masterGraph.find(name, RDF.Nodes.type, INDEXED_GRAPH);
		try {
			return it.hasNext();
		} finally {
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
	 * Retrieves a single triple matching the given pattern. If there is more than
	 * one matching triple, the first one found is returned and a warning is logged.
	 * If the triple doesn't exist, then null is returned and a warning is logged.
	 *
	 * @param graph The graph to search
	 * @param s     The subject Node
	 * @param p     The predicate Node
	 * @param o     The object Node
	 * @return A triple matching the pattern or null if it doesn't exist.
	 */
	public static Triple getOneTriple(Graph graph, Node s, Node p, Node o) {
		Triple toReturn = null;

		ExtendedIterator<Triple> it = null;
		try {
			it = graph.find(s, p, o);

			if (it.hasNext()) {
				toReturn = it.next();

				// Sanity check
				if (it.hasNext()) {
					LOG.warn("Found multiple triples matching the pattern: {} {} {}", s, p, o);
				}
			} else {
				LOG.warn("Found zero triples matching the pattern: {} {} {}", s, p, o);
			}
		} finally {
			closeQuietly(it);
		}

		return toReturn;
	}

	/** Quietly closes the given iterator.  Does nothing if the iterator is null. */
	public static void closeQuietly(ClosableIterator<?> it) {
		if (null != it) {
			it.close();
		}
	}

	public Dataset toDataset() {
		return DatasetImpl.wrap(this);
	}

	@Override
	public void close() {
		StreamUtil.asStream(listGraphNodes())
			.map(graphName -> getGraph(graphName))
			.forEach(KbGraphStore::closeGraph);
		closeGraph(defaultGraph);
	}

	private static void closeGraph(Graph graph) {
		if (graph != null) {
			IndexManager.getInstance().closeAll(graph);
			graph.close();
		}
	}
}
