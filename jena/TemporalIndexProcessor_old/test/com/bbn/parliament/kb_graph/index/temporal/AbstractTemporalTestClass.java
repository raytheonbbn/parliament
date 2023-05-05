package com.bbn.parliament.kb_graph.index.temporal;

import java.io.File;
import java.util.Properties;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jni.KbConfig;
import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.OptimizationMethod;
import com.bbn.parliament.kb_graph.index.IndexManager;
import com.bbn.parliament.kb_graph.util.FileUtil;

import junit.framework.TestCase;

public abstract class AbstractTemporalTestClass extends TestCase {
	private static final String DATA_PATH = "test_data";

	protected static final Logger LOG = LoggerFactory.getLogger(AbstractTemporalTestClass.class);

	protected TemporalIndex index;
	protected Model model;
	protected KbGraph graph;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		KbConfig c = new KbConfig();
		c.m_kbDirectoryPath = DATA_PATH;
		File f = new File(DATA_PATH);
		f.mkdirs();
		graph = new KbGraph(c, "", OptimizationMethod.DefaultOptimization);

		Properties properties = new Properties();
		properties.put(Constants.INDEX_TYPE, Constants.INDEX_PERSISTENT);

		TemporalIndexFactory factory = new TemporalIndexFactory();
		factory.configure(properties);

		index = factory.createIndex(graph, null);
		long start = System.currentTimeMillis();
		index.delete();
		index.open();
		IndexManager.getInstance().register(graph, null, factory, index);
		model = ModelFactory.createModelForGraph(graph);

		LOG.debug("Prep time: {}", (System.currentTimeMillis() - start));
	}

	@Override
	protected void tearDown() throws Exception {
		model.removeAll();
		model.close();
		index.delete();
		FileUtil.delete(new File(DATA_PATH));
		super.tearDown();
	}
}
