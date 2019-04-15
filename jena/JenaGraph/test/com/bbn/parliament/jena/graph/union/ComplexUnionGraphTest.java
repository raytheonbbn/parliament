// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2019, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.union;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.OptimizationMethod;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jni.KbConfig;
import com.bbn.parliament.jni.KbInstance;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryCancelledException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@RunWith(JUnitPlatform.class)
public class ComplexUnionGraphTest {
	private static final String ONT_RSRC = "univ-bench.owl";
	private static final File INPUT_DATA_FILE = new File(
		System.getProperty("test.data.path"), "univ-bench-03.zip");
	private static final File KB_DATA_DIR = new File("./union-test-kb-data");
	private static final String PREFIXES = "prefix ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> ";

	private static final String QUERY0 = PREFIXES +
		"select ?x ?y ?z where { " +
		"	?x a ub:GraduateStudent . " +
		"	?y a ub:University . " +
		"	?z a ub:Department . " +
		"	?x ub:memberOf ?z . " +
		"	?z ub:subOrganizationOf ?y . " +
		"	?x ub:undergraduateDegreeFrom ?y . " +
		"}";
	private static final String QUERY1 = PREFIXES +
		"select ?x ?y1 ?y2 ?y3 where { " +
		"	?x a ub:Professor ; " +
		"		ub:worksFor <http://www.Department0.University0.edu> ; " +
		"		ub:name ?y1 ; " +
		"		ub:emailAddress ?y2 ; " +
		"		ub:telephone ?y3 . " +
		"}";
	private static final String QUERY2 = PREFIXES +
		"select ?x ?y ?z where { " +
		"	?x a ub:Student . " +
		"	?y a ub:Faculty . " +
		"	?z a ub:Course . " +
		"	?x ub:advisor ?y . " +
		"	?y ub:teacherOf ?z . " +
		"	?x ub:takesCourse ?z . " +
		"}";
	private static final String QUERY3 = PREFIXES +
		"select ?x ?y where { " +
		"	?x a ub:GraduateStudent ; " +
		"		ub:name ?y . " +
		"}";
	private static final String[] QUERYS = {
		QUERY0,
		QUERY1,
		QUERY2,
		QUERY3
	};

	private static final Node graph0Name = Node.createURI("http://example.org/graph0");
	private static final Node graph1Name = Node.createURI("http://example.org/graph1");
	private static final Node graph2Name = Node.createURI("http://example.org/graph2");
	private static final Node innerUnionName = Node.createURI("http://example.org/innner-union-graph");

	private static final Logger LOG = LoggerFactory.getLogger(ComplexUnionGraphTest.class);

	private static KbGraph bigGraph;
	private static KbGraph graph0;
	private static KbGraph graph1;
	private static KbGraph graph2;

	private static Model bigModel;
	private static Model model0;
	private static Model model1;
	private static Model model2;

	private static KbUnionGraph innerUnionGraph;
	private static KbUnionGraph unionGraph;
	private static Model unionModel;

	@BeforeAll
	public static void beforeAll() {
		try {
			bigGraph = createGraph(KB_DATA_DIR, "big");
			graph0 = createGraph(KB_DATA_DIR, "0");
			graph1 = createGraph(KB_DATA_DIR, "1");
			graph2 = createGraph(KB_DATA_DIR, "2");

			bigModel = createModel(bigGraph);
			model0 = createModel(graph0);
			model1 = createModel(graph1);
			model2 = createModel(graph2);

			//Load data
			loadUniversityData(model0, 0);
			loadUniversityData(model1, 1);
			loadUniversityData(model2, 2);

			LOG.info("Big model contains {} statements", bigModel.size());
			LOG.info("Model 0 contains {} statements", model0.size());
			LOG.info("Model 1 contains {} statements", model1.size());
			LOG.info("Model 2 contains {} statements", model2.size());

			innerUnionGraph = new KbUnionGraph(graph0, graph0Name, graph1, graph1Name);
			unionGraph = new KbUnionGraph(innerUnionGraph, innerUnionName, graph2, graph2Name);
			unionModel = ModelFactory.createModelForGraph(unionGraph);
		} catch (IOException ex) {
			ex.printStackTrace();
			assertTrue(false, "beforeAll() failure");
		}
	}

	@AfterAll
	public static void afterAll() {
		closeGraph(bigGraph);
		closeGraph(graph0);
		closeGraph(graph1);
		closeGraph(graph2);
	}

	private static void closeGraph(Graph g) {
		if (g != null) {
			g.close();
		}
	}

	@BeforeEach
	public void beforeEach() {
	}

	@AfterEach
	public void afterEach() {
	}

	//TODO: Queries 0 and 2 time out.  This should be fixed.
	@SuppressWarnings("static-method")
	@ParameterizedTest
	@ValueSource(ints = { /*0,*/ 1, /*2,*/ 3 })
	public void testComplexUnionGraph(int queryNum) {
		LOG.info("Running query #{}...", queryNum);
		long bigCount = timedCount("big", queryNum, QUERYS[queryNum], bigModel);
		long unionCount = timedCount("union", queryNum, QUERYS[queryNum], unionModel);
		assertEquals(bigCount, unionCount);
	}

	private static KbGraph createGraph(File rootDir, String relativeDirectory) {
		File dir = new File(rootDir, relativeDirectory);
		dir.mkdirs();
		KbConfig config = new KbConfig();
		KbConfig defaultConfig = new KbConfig();
		config.readFromFile();
		config.m_kbDirectoryPath = dir.getPath();
		config.m_initialRsrcCapacity = defaultConfig.m_initialRsrcCapacity;
		config.m_initialStmtCapacity = defaultConfig.m_initialStmtCapacity;
		config.m_rsrcGrowthIncrement = defaultConfig.m_rsrcGrowthIncrement;
		config.m_stmtGrowthIncrement = defaultConfig.m_stmtGrowthIncrement;
		KbInstance.deleteKb(config, null);
		return new KbGraph(config, relativeDirectory, OptimizationMethod.DefaultOptimization);
	}

	private static Model createModel(KbGraph g) throws IOException {
		Model m = ModelFactory.createModelForGraph(g);
		RDFFormat rdfFmt = RDFFormat.parseFilename(ONT_RSRC);
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try (InputStream strm = cl.getResourceAsStream(ONT_RSRC)) {
			if (strm == null) {
				throw new FileNotFoundException("Unable to load resource " + ONT_RSRC);
			}
			m.read(strm, null, rdfFmt.toString());
		}
		return m;
	}

	private static void loadUniversityData(Model smallModel, int univNum) throws IOException {
		long start = System.currentTimeMillis();
		Model inMemTmpModel = readUniversityData(univNum);
		long duration = System.currentTimeMillis() - start;
		LOG.info("Read university #{} in {} ms", univNum, duration);

		start = System.currentTimeMillis();
		bigModel.add(inMemTmpModel);
		smallModel.add(inMemTmpModel);
		duration = System.currentTimeMillis() - start;
		LOG.info("Stored university #{} in {} ms", univNum, duration);
	}

	private static Model readUniversityData(int univNum) throws IOException {
		Pattern pattern = Pattern.compile(
			String.format("^.*/University%1$d_.*$", univNum), Pattern.CASE_INSENSITIVE);
		Model result = ModelFactory.createDefaultModel();
		try (ZipFile zipFile = new ZipFile(INPUT_DATA_FILE)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (!zipEntry.isDirectory() && pattern.matcher(zipEntry.getName()).matches()) {
					RDFFormat rdfFmt = RDFFormat.parseFilename(zipEntry.getName());
					try (InputStream strm = zipFile.getInputStream(zipEntry)) {
						result.read(strm, null, rdfFmt.toString());
					}
				}
			}
		}
		return result;
	}

	private static long timedCount(String modelName, int queryNum, String query, Model model) {
		long start = System.currentTimeMillis();
		long count = 0;
		try {
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			qe.setTimeout(30, TimeUnit.SECONDS);
			ResultSet results = qe.execSelect();
			while (results.hasNext()) {
				results.next();
				++count;
			}
		} catch (QueryCancelledException ex) {
			assertTrue(false, String.format(
				"Query #%1$d against the %2$s model exceeded timeout limit", queryNum, modelName));
		}
		long duration = System.currentTimeMillis() - start;
		LOG.info("{} results from {} model in {} ms", count, modelName, duration);
		return count;
	}
}
