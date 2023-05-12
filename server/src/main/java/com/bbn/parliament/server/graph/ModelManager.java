// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.graph;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.client.StreamUtil;
import com.bbn.parliament.core.jni.KbConfig;
import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.KbGraphFactory;
import com.bbn.parliament.kb_graph.KbGraphStore;
import com.bbn.parliament.kb_graph.index.IndexFactoryRegistry;
import com.bbn.parliament.kb_graph.index.IndexManager;
import com.bbn.parliament.kb_graph.modify.KbUpdateEngine;
import com.bbn.parliament.kb_graph.union.KbUnionableGraph;
import com.bbn.parliament.server.configuration.ReasonerConfigurationHandler;
import com.bbn.parliament.server.util.ConcurrentRequestController;
import com.bbn.parliament.server.util.ConcurrentRequestLock;

public class ModelManager {
	private static class ModelManagerHolder {
		private static final ModelManager INSTANCE = new ModelManager();
	}

	private static final String AUTO_GRAPH_DIR = "autoGraph-%1$08d";
	private static final Pattern AUTO_GRAPH_PATTERN = Pattern.compile(
		Pattern.quote(KbGraphStore.PARLIAMENT_NS) + "autoGraph-([0-9]+)");
	private static final Logger LOG = LoggerFactory.getLogger(ModelManager.class);

	private List<ReasonerConfigurationHandler> reasonerHandlers;
	private KbGraphStore kbGraphStore;
	private Dataset dataSet;
	private FlushTimerTask flushTimerTask = null;

	/**
	 * Get the singleton instance of the model manager. This follows the "lazy
	 * initialization holder class" idiom for lazy initialization of a static field.
	 * See Item 83 of Effective Java, Third Edition, by Joshua Bloch for details.
	 *
	 * @return the instance
	 */
	public static ModelManager inst() {
		return ModelManagerHolder.INSTANCE;
	}

	private ModelManager() {
	}

	// -----------------------------------------------------------------
	// Public Methods
	// -----------------------------------------------------------------

	/**
	 * Gets the default model from the graph map. The default model is the model
	 * with an empty named graph URI.
	 */
	public Model getDefaultModel() {
		return dataSet.getDefaultModel();
	}

	/** Gets the model corresponding to the master graph. */
	public Model getMasterModel() {
		return dataSet.getNamedModel(KbGraphStore.MASTER_GRAPH.getURI());
	}

	/** Gets the model corresponding to the given named graph URI. */
	public Model getModel(String namedGraphUri) {
		return dataSet.getNamedModel(namedGraphUri);
	}

	private void addNamedModel(String graphName, Model model) {
		dataSet.addNamedModel(graphName, model);
	}

	public String getGraphDir(String namedGraphUri) {
		return kbGraphStore.getGraphDir(NodeFactory.createURI(namedGraphUri));
	}

	/** Get the default graph's configuration. */
	public KbConfig getDefaultGraphConfig() {
		return kbGraphStore.getDefaultGraphConfig();
	}

	@SuppressWarnings("resource")
	public Model createAndAddNamedModel(String graphName, String graphDir, boolean indexEnabled) {
		KbGraph graph = KbGraphFactory.createNamedGraph(graphDir);
		if (indexEnabled) {
			Node graphNode = null;
			if (null != graphName) {
				graphNode = NodeFactory.createURI(graphName);
			}

			IndexManager.getInstance().createAndRegisterAll(graph, graphNode);
		}
		Model toReturn = ModelFactory.createModelForGraph(graph);
		this.addNamedModel(graphName, toReturn);
		return toReturn;
	}

	public Model createAndAddKbUnionGraph(String graphName, String leftGraphName, String rightGraphName) {
		var leftGraphNode = NodeFactory.createURI(leftGraphName);
		var rightGraphNode = NodeFactory.createURI(rightGraphName);
		var leftKbUnionableGraph = (KbUnionableGraph) kbGraphStore.getGraph(leftGraphNode);
		var rightKbUnionableGraph = (KbUnionableGraph) kbGraphStore.getGraph(rightGraphNode);

		Model toReturn = ModelFactory.createModelForGraph(
			KbGraphFactory.createKbUnionGraph(
				leftKbUnionableGraph, leftGraphNode,
				rightKbUnionableGraph, rightGraphNode));

		this.addNamedModel(graphName, toReturn);
		return toReturn;
	}

	public String createAutoNamedGraph() {
		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			var maxAutoGraphNumber = StreamUtil.asStream(kbGraphStore.listGraphNodes())
				.map(Node::getURI)
				.map(AUTO_GRAPH_PATTERN::matcher)
				.filter(Matcher::matches)
				.map(m -> m.group(1))
				.map(Long::getLong)
				.max(Long::compareTo)
				.orElse(0L);
			var graphDir = AUTO_GRAPH_DIR.formatted(maxAutoGraphNumber);
			var graphName = KbGraphStore.PARLIAMENT_NS + graphDir;
			var indexEnabled = IndexFactoryRegistry.getInstance().isIndexingEnabledByDefault();
			@SuppressWarnings("unused")
			var m = createAndAddNamedModel(graphName, graphDir, indexEnabled);
			return graphName;
		}
	}

	/** Checks whether we have a model of the given name. */
	public boolean containsModel(String namedGraphUri) {
		return dataSet.containsNamedModel(namedGraphUri);
	}

	/** Gets the list of model names, sorted lexicographically */
	public List<String> getSortedModelNames() {
		return StreamUtil.asStream(dataSet.listNames())
			.sorted()
			.collect(Collectors.toUnmodifiableList());
	}

	public Dataset getDataset() {
		return dataSet;
	}

	public void clearKb() {
		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			stopFlushTimer();

			kbGraphStore.clear();
			dataSet = null;

			initialize();
		}

		LOG.info("Finished clearing and reinitializing the knowledge base");
	}

	public void closeKb() {
		stopFlushTimer();

		dataSet.close();
		dataSet = null;

		LOG.info("Flushed and closed the knowledge base");
	}

	public void flushKb() {
		// We need a read-lock here to prevent a write operation during
		// flushing that might cause a reallocation of a memory-mapped file.
		// However, other readers may continue during the flush, so we don't
		// need a write lock.
		try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			kbGraphStore.flush();
		}

		LOG.debug("Flushed the KB models to disk");
	}

	/** Load all RDF files in the given directory and all its sub-directories. */
	public void loadDirectory(File dir) {
		File[] children = dir.listFiles();
		if (children != null) {
			for (File file : children) {
				if (file.isDirectory()) {
					loadDirectory(file);
				} else {
					loadFile(file);
				}
			}
		}
	}

	/** Load all RDF files in the given directory and all its sub-directories. */
	public void loadDirectory(String dir) {
		loadDirectory(new File(dir));
	}

	/** Load the given file into the model. */
	public void loadFile(File file) {
		loadFile(file.getAbsolutePath());
	}

	/** Load the given file into the model. */
	public void loadFile(String filename) {
		if (dataSet == null) {
			initialize();
		}

		var lang = RDFLanguages.pathnameToLang(filename);
		if (lang == null) {
			LOG.warn("Ignoring {}", filename);
			return;
		}

		try (Reader reader = new FileReader(filename)) {
			LOG.info("Importing model data from: {}", filename);
			getDefaultModel().read(reader, null, lang.getName());
		} catch (Exception ex) {
			LOG.error("Could not read file: " + filename, ex);
		}
	}

	/** Adds a reasoner handler. */
	public void addReasonerHandler(ReasonerConfigurationHandler handler) {
		if (reasonerHandlers == null) {
			reasonerHandlers = new ArrayList<>();
		}
		reasonerHandlers.add(handler);
	}

	public synchronized void initialize() {
		if (dataSet != null) {
			LOG.info("Ignoring loadModel call -- model already loaded.");
		} else {
			LOG.info("Loading Model");

			// Don't remove the following two lines. They cause Java to fix
			// its working path, which is essential before the config file loads
			File cwd = new File(".");
			LOG.info("Loading Parliament configuration with working directory \"{}\"",
				cwd.getAbsolutePath());

			@SuppressWarnings("resource")
			KbGraph defaultGraph = KbGraphFactory.createDefaultGraph();
			if (IndexFactoryRegistry.getInstance().isIndexingEnabledByDefault()) {
				IndexManager.getInstance().createAndRegisterAll(defaultGraph, null);
			}

			kbGraphStore = new KbGraphStore(defaultGraph);
			kbGraphStore.initialize();

			dataSet = kbGraphStore.toDataset();

			// Register our own update processor
			KbUpdateEngine.register();

			startFlushTimer(defaultGraph.getConfig().m_fileSyncTimerDelay);
		}
	}

	private void startFlushTimer(long delay) {
		stopFlushTimer();
		if (delay > 0) {
			flushTimerTask = new FlushTimerTask(this, delay);
		}
	}

	private void stopFlushTimer() {
		if (flushTimerTask != null) {
			flushTimerTask.cancel();
			flushTimerTask = null;
		}
	}

	private static class FlushTimerTask extends TimerTask {
		private static Logger _log = LoggerFactory
			.getLogger(FlushTimerTask.class);

		private Timer _timer;
		private ModelManager _modelMgr;
		private long _delay;

		public FlushTimerTask(ModelManager modelMgr, long delay) {
			_timer = new Timer("FlushTimerThread", true);
			_modelMgr = modelMgr;
			_delay = delay;
			_timer.schedule(this, _delay, _delay);
		}

		@Override
		public boolean cancel() {
			boolean result = super.cancel();
			if (_timer != null) {
				_timer.cancel();
				_timer = null;
			}
			return result;
		}

		@Override
		public void run() {
			if (System.currentTimeMillis() - scheduledExecutionTime() >= _delay) {
				_log.warn("""
					Skipping overly delayed FlushTimerTask execution. \
					Is your flush timer delay setting too short?""");
			} else {
				_modelMgr.flushKb();
			}
		}
	}
}