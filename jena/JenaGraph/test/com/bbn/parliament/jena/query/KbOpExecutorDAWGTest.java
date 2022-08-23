package com.bbn.parliament.jena.query;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjena.riot.checker.CheckerLiterals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.TestingDataset;
import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.joseki.client.QuerySolutionStream;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable;
import com.hp.hpl.jena.sparql.resultset.SPARQLResult;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.vocabulary.ResultSetGraphVocab;
import com.hp.hpl.jena.vocabulary.RDF;

public class KbOpExecutorDAWGTest {
	private static final String[] DAWG_TEST_DIRS = {
		"algebra",
		"ask",
		"basic",
		//"bnode-coreference",
		"boolean-effective-value",
		"bound",
		"cast",
		"construct",
		"distinct",
		"expr-builtin",
		"expr-equals",
		"expr-ops",
		"graph",
		"i18n",
		"open-world",
		"optional",
		"optional-filter",
		"reduced",
		"regex",
		"solution-seq",
		"sort",
		//"syntax-sparql1",
		//"syntax-sparql2",
		//"syntax-sparql3",
		//"syntax-sparql4",
		//"syntax-sparql5",
		"triple-match",
		"type-promotion",
	};
	private static final List<String> INVALID_TESTS = List.of(
		// This test has data and query identical to that for
		// "optional-filter/dawg-optional-filter-005-not-simplified", but
		// different results.  Not sure how that's supposed to work.
		"optional-filter/dawg-optional-filter-005-simplified",

		// These added because DAWG is not compliant with RDF 1.1:
		"reduced/SELECT REDUCED ?x with strings",
		"distinct/Strings: Distinct",
		"distinct/All: Distinct",

		// These fail because the master graph adds extra results not expected by the test:
		"graph/graph-03",
		"graph/graph-04",
		"graph/graph-06",
		"graph/graph-07",
		"graph/graph-11",

		// Did the query parse change in SPARQL 1.1?  Or did ARQ fix a bug after 2.9.4?
		"basic/Basic - Term 6",
		"basic/Basic - Term 7"
	);
	private static final String MANIFEST_ENTRY_QUERY = """
		prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
		prefix mf:  <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
		prefix qt:  <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>
		select distinct ?entry ?name ?result ?query ?data ?graphData where {
			?manifest a mf:Manifest ;
				mf:entries/rdf:rest*/rdf:first ?entry .
			?entry mf:name ?name ;
				mf:result ?result ;
				mf:action ?action .
			?action qt:query ?query .
			optional { ?action qt:data ?data }
			optional { ?action qt:graphData ?graphData }
		}
		""";
	private static final File DAWG_ROOT_DIR = new File("data/data-r2");
	private static final Pattern FILE_URI_FIXER = Pattern.compile("^(file:/)([^/].*)$");
	private static final Pattern FILE_URI_FIXER_2 = Pattern.compile("^(file:///[A-Za-z]):(.*)$");
	private static final Logger log = LoggerFactory.getLogger(KbOpExecutorDAWGTest.class);

	private static TestingDataset dataset;

	@BeforeAll
	public static void beforeAll() {
		dataset = new TestingDataset();
	}

	@AfterAll
	public static void afterAll() {
		dataset.clear();
	}

	private boolean oldWarningFlag = false;
	private ExecutionContext execCxt;
	private KbOpExecutor opExecutor;

	@BeforeEach
	public void beforeEach() {
		oldWarningFlag = CheckerLiterals.WarnOnBadLiterals;
		CheckerLiterals.WarnOnBadLiterals = false;
		log.debug("AbstractDAWGTestCase.setUp:  Set CheckerLiterals.WarnOnBadLiterals to false.");

		Context params = ARQ.getContext();
		@SuppressWarnings("resource")
		KbGraph defaultGraph = dataset.getDefaultGraph();
		execCxt = new ExecutionContext(params, defaultGraph, dataset.getGraphStore(),
			KbOpExecutor.KbOpExecutorFactory);
		opExecutor = new KbOpExecutor(execCxt);
	}

	@AfterEach
	public void afterEach() {
		dataset.reset();
		CheckerLiterals.WarnOnBadLiterals = oldWarningFlag;
	}

	private static Stream<DAWGManifestEntry> testDawgTest() {
		Map<Resource, DAWGManifestEntry> testDataMap = new HashMap<>();
		for (String relTestDir : DAWG_TEST_DIRS) {
			File testDir = new File(DAWG_ROOT_DIR, relTestDir);
			File manifestFile = new File(testDir, "manifest.ttl");
			log.debug("Loading manifest file '{}' with base '{}'",
				manifestFile.getPath(), testDir.getPath());
			Model manifestModel = QueryTestUtil.loadModel(
				manifestFile.getPath(), testDir.getPath());
			try (QuerySolutionStream stream = new QuerySolutionStream(
					MANIFEST_ENTRY_QUERY, manifestModel)) {
				stream.forEach(qs -> {
					Resource entry = qs.getResource("entry");
					testDataMap
						.computeIfAbsent(entry, DAWGManifestEntry::new)
						.addQuerySolution(qs, testDir);
				});
			}
		}
		log.info("Found {} DAWG tests to be run", testDataMap.size());
		return testDataMap.values().stream();
	}

	@ParameterizedTest
	@MethodSource
	public void testDawgTest(DAWGManifestEntry me) throws IOException {
		if (INVALID_TESTS.contains(me.getCurrentTest())) {
			log.warn("Skipping DAWG test '{}'", me.getCurrentTest());
			return;
		}
		for (File dataFile : me.getData()) {
			@SuppressWarnings("resource")
			KbGraph defaultGraph = dataset.getDefaultGraph();
			QueryTestUtil.loadResource(dataFile.getPath(), defaultGraph);
		}
		for (File graphDataFile : me.getGraphData()) {
			//String uri = graphDataFile.toURI().toString();
			String fileName = graphDataFile.getName();
			String uri = new File(fileName).toURI().toString();
			Matcher m = FILE_URI_FIXER.matcher(uri);
			if (m.matches()) {
				uri = m.replaceAll("$1//$2");
			}
			Matcher m2 = FILE_URI_FIXER_2.matcher(uri);
			if (m2.matches()) {
				uri = m2.replaceAll("$1%3A$2");
			}
			log.debug("Graph URI for test '{}' is '{}'", me.getName(), uri);
			@SuppressWarnings("resource")
			KbGraph namedGraph = dataset.getNamedGraph(uri);
			QueryTestUtil.loadResource(graphDataFile.getPath(), namedGraph);
		}
		try {
			Query q = QueryFactory.read(me.getQuery().getPath());
			if (q.isSelectType()) {
				ResultSetRewindable rs = QueryTestUtil.loadResultSet(me.getResult().getPath());
				runDAWGTest(q, rs, me);
			} else if (q.isAskType()) {
				SPARQLResult expectedResultSet = ResultSetFactory.result(me.getResult().getPath());
				boolean answer = false;
				if (expectedResultSet.isBoolean()) {
					answer = expectedResultSet.getBooleanResult();
				} else {
					Model resultsAsModel = expectedResultSet.getModel();
					StmtIterator sIter = resultsAsModel.listStatements(null, RDF.type,
						ResultSetGraphVocab.ResultSet);
					if (!sIter.hasNext()) {
						fail("Could not find ASK result for '%1$s'".formatted(me.getName()));
					}
					Statement s = sIter.next();
					if (sIter.hasNext()) {
						fail("More than one ASK result for '%1$s'".formatted(me.getName()));
					}
					answer = s.getSubject().getRequiredProperty(ResultSetGraphVocab.p_boolean).getBoolean();
				}
				runDAWGTest(q, answer, me);
			}
		} catch (QueryParseException ex) {
			fail("'%1$s': query parse exception:  %2$s".formatted(me.getCurrentTest(), ex.getMessage()));
		}
	}

	private void runDAWGTest(Query query, ResultSetRewindable expectedResultSet, DAWGManifestEntry me) {
		Op op = Algebra.compile(query);
		QueryIterator input = QueryIterRoot.create(execCxt);
		QueryIterator it = opExecutor.executeOp(op, input);
		ResultSetRewindable actualResultSet = ResultSetFactory.makeRewindable(
			ResultSetFactory.create(it, query.getResultVars()));

		StringBuilder message = new StringBuilder();
		message.append("%n'%1$s': Result sets are not equal:%n%n".formatted(me.getCurrentTest()));
		boolean matches = QueryTestUtil.equals(expectedResultSet, actualResultSet, query, message);
		assertTrue(matches, message.toString());
	}

	private void runDAWGTest(Query query, boolean answer, DAWGManifestEntry me) {
		Op op = Algebra.compile(query);
		QueryIterator input = QueryIterRoot.create(execCxt);
		QueryIterator it = opExecutor.executeOp(op, input);
		assertTrue(answer == it.hasNext(),
			"'%1$s': result sets are not equal".formatted(me.getCurrentTest()));
	}
}
