package com.bbn.parliament.jena.query;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.openjena.riot.checker.CheckerLiterals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.resultset.SPARQLResult;
import com.hp.hpl.jena.sparql.vocabulary.ResultSetGraphVocab;
import com.hp.hpl.jena.vocabulary.RDF;

public abstract class AbstractDAWGTestCase extends AbstractKbTestCase {
	private static class ManifestEntry {
		private Resource entry = null;
		private String name = null;
		private File result = null;
		private File query = null;
		private List<File> data = new ArrayList<>();
		private List<File> graphData = new ArrayList<>();

		public ManifestEntry(Resource entry) {
			this.entry = entry;
			name = null;
			result = null;
			query = null;
			data = new ArrayList<>();
			graphData = new ArrayList<>();
		}

		public Object[] getTestData(File testDir) {
			return new Object[] { testDir, name, result, query, data, graphData };
		}

		public void addQuerySolution(QuerySolution qs, File testDir) {
			Resource entryVar = qs.getResource("entry");
			if (entryVar == null) {
				throw new IllegalStateException("Entry URI is null in query result");
			} else if (!entryVar.equals(entry)) {
				throw new IllegalStateException("Entry URIs don't match");
			}
			String nameVar = getStringLiteral(qs, "name");
			if (name != null && !name.equals(nameVar)) {
				throw new IllegalStateException("Two names for one manifest entry");
			} else {
				name = nameVar;
			}
			File resultVar = getFileLiteral(qs, testDir, "result");
			if (result != null && !result.equals(resultVar)) {
				throw new IllegalStateException("Two names for one manifest entry");
			} else {
				result = resultVar;
			}
			File queryVar = getFileLiteral(qs, testDir, "query");
			if (query != null && !query.equals(queryVar)) {
				throw new IllegalStateException("Two names for one manifest entry");
			} else {
				query = queryVar;
			}
			File dataVar = getFileLiteral(qs, testDir, "data");
			if (dataVar != null) {
				data.add(dataVar);
			}
			File graphDataVar = getFileLiteral(qs, testDir, "graphData");
			if (graphDataVar != null) {
				graphData.add(graphDataVar);
			}
		}

		// This unusual implementation is due to sometimes-encoding of what should be
		// string literals as relative URIs in some of the DAWG manifest files.
		private static String getStringLiteral(QuerySolution qs, String varName) {
			RDFNode node = qs.get(varName);
			if (node == null) {
				return null;
			} else if (node.isLiteral()) {
				return node.asLiteral().getLexicalForm();
			} else if (node.isURIResource()) {
				return node.asResource().getLocalName();
			} else {
				throw new IllegalStateException("Result node has unrecognized type "
					+ node.getClass().getName());
			}
		}

		private static File getFileLiteral(QuerySolution qs, File testDir, String varName) {
			String fileName = getStringLiteral(qs, varName);
			return (fileName == null) ? null : new File(testDir, fileName);
		}
	}

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
	private static final String[] INVALID_TESTS = {
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
		"basic/Basic - Term 7",
	};
	private static final String MANIFEST_QUERY = ""
		+ "prefix mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>%n"
		+ "prefix qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>%n"
		+ "select distinct ?manifest ?entryList where {%n"
		+ "	?manifest a mf:Manifest ;%n"
		+ "		mf:entries ?entryList .%n"
		+ "}%n";
	private static final String MANIFEST_ENTRY_QUERY = ""
		+ "prefix mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>%n"
		+ "prefix qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>%n"
		+ "select distinct ?entry ?name ?result ?query ?data ?graphData where {%n"
		+ "	values ?entry {"
		+ "		%1$s%n"
		+ "	}%n"
		+ "	?entry mf:name ?name ;%n"
		+ "		mf:result ?result ;%n"
		+ "		mf:action ?action .%n"
		+ "	?action qt:query ?query .%n"
		+ "	optional { ?action qt:data ?data }%n"
		+ "	optional { ?action qt:graphData ?graphData }%n"
		+ "}%n";
	private static final File DAWG_ROOT_DIR = new File("data/data-r2");
	private static final Pattern FILE_URI_FIXER = Pattern.compile("^(file:/)([^/].*)$");
	private static final Pattern FILE_URI_FIXER_2 = Pattern.compile("^(file:///[A-Za-z]):(.*)$");
	private static final Logger log = LoggerFactory.getLogger(AbstractDAWGTestCase.class);

	private boolean oldWarningFlag;
	private File testDir;
	private String name;
	private File result;
	private File query;
	private List<File> data;
	private List<File> graphData;

	protected static Collection<Object[]> generateData() {
		List<Object[]> testDataList = new ArrayList<>();
		for (String relTestDir : DAWG_TEST_DIRS) {
			File testDir = new File(DAWG_ROOT_DIR, relTestDir);
			File manifestFile = new File(testDir, "manifest.ttl");
			log.debug("Loading manifest file '{}' with base '{}'",
				manifestFile.getPath(), testDir.getPath());
			Model manifestModel = loadModel(manifestFile.getPath(), testDir.getPath());
			QueryExecution exec = QueryExecutionFactory.create(
				String.format(MANIFEST_QUERY), manifestModel);
			ResultSet rs = exec.execSelect();
			List<RDFNode> bigEntryList = new ArrayList<>();
			while (rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				Resource entryList = qs.getResource("entryList");
				if (entryList != null) {
					RDFList entries = entryList.as(RDFList.class);
					if (entries != null) {
						bigEntryList.addAll(entries.asJavaList());
					}
				}
			}
			exec.close();

			StringBuilder values = new StringBuilder();
			for (RDFNode node : bigEntryList) {
				values.append(String.format("<%1$s>%n", node.asResource().getURI()));
			}

			exec = QueryExecutionFactory.create(
				String.format(MANIFEST_ENTRY_QUERY, values), manifestModel);
			rs = exec.execSelect();
			Map<Resource, ManifestEntry> testDataMap = new HashMap<>();
			while (rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				Resource entry = qs.getResource("entry");
				if (entry != null) {
					ManifestEntry me = testDataMap.get(entry);
					if (me == null) {
						me = new ManifestEntry(entry);
						testDataMap.put(entry, me);
					}
					me.addQuerySolution(qs, testDir);
				}
			}
			exec.close();

			for (Map.Entry<Resource, ManifestEntry> e : testDataMap.entrySet()) {
				testDataList.add(e.getValue().getTestData(testDir));
			}
		}
		log.info("Found {} DAWG tests to be run", testDataList.size());
		return testDataList;
	}

	protected AbstractDAWGTestCase(File testDir, String name, File result, File query,
		List<File> data, List<File> graphData) {
		oldWarningFlag = false;
		this.testDir = testDir;
		this.name = name;
		this.result = result;
		this.query = query;
		this.data = data;
		this.graphData = graphData;
	}

	@Override
	public void setUp() {
		super.setUp();
		oldWarningFlag = CheckerLiterals.WarnOnBadLiterals;
		CheckerLiterals.WarnOnBadLiterals = false;
		log.debug("AbstractDAWGTestCase.setUp:  Set CheckerLiterals.WarnOnBadLiterals to false.");
	}

	@Override
	public void tearDown() {
		super.tearDown();
		CheckerLiterals.WarnOnBadLiterals = oldWarningFlag;
	}

	protected String getCurrentTest() {
		return String.format("%1$s/%2$s", testDir.getName(), name);
	}

	@Test
	public void testDawgTest() throws IOException {
		for (String invalid : INVALID_TESTS) {
			if (getCurrentTest().equals(invalid)) {
				log.warn("Skipping DAWG test '{}'", invalid);
				return;
			}
		}
		if (null != data) {
			for (File dataFile : data) {
				loadResource(dataFile.getPath(), getGraph());
			}
		}
		if (graphData != null) {
			for (File graphDataFile : graphData) {
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
				log.debug("Graph URI for test '{}' is '{}'", name, uri);
				loadResource(graphDataFile.getPath(), getNamedGraph(uri));
			}
		}
		try {
			Query q = loadQuery(query.getPath());
			if (q.isSelectType()) {
				ResultSet rs = loadResultSet(result.getPath());
				if (log.isDebugEnabled() && rs.getResultVars().contains("g")) {
					while (rs.hasNext()) {
						QuerySolution qs  = rs.next();
						RDFNode n = qs.get("g");
						String uri = (n == null) ? "null" : n.asResource().getURI();
						log.debug("Value of ?g in test '{}':  '{}'", name, uri);
					}
				}
				runDAWGTest(q, rs);
			} else if (q.isAskType()) {
				SPARQLResult expectedResultSet = ResultSetFactory.result(result.getPath());
				boolean answer = false;
				if (expectedResultSet.isBoolean()) {
					answer = expectedResultSet.getBooleanResult();
				} else {
					Model resultsAsModel = expectedResultSet.getModel();
					StmtIterator sIter = resultsAsModel.listStatements(null, RDF.type,
						ResultSetGraphVocab.ResultSet);
					if (!sIter.hasNext()) {
						log.warn("Could not find ASK result for '{}'", name);
						return;
					}
					Statement s = sIter.nextStatement();
					if (sIter.hasNext()) {
						log.warn("More than one ASK result for '{}'", name);
						return;
					}
					Resource r = s.getSubject();
					Property p = resultsAsModel.createProperty(
						ResultSetGraphVocab.getURI() + "boolean");

					answer = r.getRequiredProperty(p).getBoolean();
				}
				runDAWGTest(q, answer);
			}
		} catch (QueryParseException ex) {
			fail(String.format("'%1$s': query parse exception:  %2$s",
				getCurrentTest(), ex.getMessage()));
		}
	}

	protected abstract void runDAWGTest(Query q, ResultSet resultSet);
	protected abstract void runDAWGTest(Query q, boolean answer);
}
