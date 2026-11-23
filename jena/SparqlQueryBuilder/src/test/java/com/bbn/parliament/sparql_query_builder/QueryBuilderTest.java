// Copyright (c) 2019, 2020 RTX BBN Technologies Corp.

package com.bbn.parliament.sparql_query_builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Since verification of the proper query results is so hard (we'd have to
 * compare strings with the expected query, and make sure we get every character
 * right including newlines etc), these tests just ensure that the operations
 * don't produce exceptions.
 * <p>
 * If you think of a good way to do this, feel free to update the tests.
 *
 * @author mallen
 */
public class QueryBuilderTest {
	private static final String EXAMPLE_IRI = "http://www.bbn.com/ontologies/rush/problem-strategy#name";
	private static final Logger LOG = LoggerFactory.getLogger(QueryBuilderTest.class);
	private static final String QUERY_FILE = "TestSelectQuery.sparql";
	private static final String VALUE_VAR_IN_TEST_QUERY = "value";
	private static final String VAR1_VAR_IN_TEST_QUERY_WITH_VALUES = "var1";

	@BeforeAll
	public static void beforeAll() throws Exception {
		QueryBuilder.addToDefaultPrefixMapping(PrefixMapping.Standard);
	}

	@AfterAll
	public static void afterAll() throws Exception {
	}

	@BeforeEach
	public void beforeEach() throws Exception {
	}

	@AfterEach
	public void afterEach() throws Exception {
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testLoadQueryAsClassRsrc() throws Exception {
		//TODO: compare strings
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		assertNotNull(q.toString());
		assertFalse(q.toString().isEmpty());
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testLoadQueryAsGlobalRsrc() throws Exception {
		//TODO: compare strings
		String pkgName = QueryBuilderTest.class.getPackage().getName();
		String rsrcPath = pkgName.replaceAll("\\.", "/") + '/' + QUERY_FILE;
		QueryBuilder q = QueryBuilder.fromRsrc(rsrcPath);
		assertNotNull(q.toString());
		assertFalse(q.toString().isEmpty());
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetRDFNodeArg() {
		RDFNode node = ResourceFactory.createResource(EXAMPLE_IRI);
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setArg("prop", node);

		LOG.trace("Set RDFNode argument:\n{}", q);

		assertTrue(q.toString().contains("ps:name"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetNodeArg() {
		Node node = ResourceFactory.createResource(EXAMPLE_IRI).asNode();
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setArg("prop", node);

		LOG.trace("Set Node argument:\n{}", q);

		assertTrue(q.toString().contains("ps:name"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetIriArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setIriArg("prop", EXAMPLE_IRI);

		LOG.trace("Set IRI argument:\n{}", q);

		assertTrue(q.toString().contains("ps:name"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetStringArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setArg(VALUE_VAR_IN_TEST_QUERY, "example text");

		LOG.trace("Set string argument:\n{}", q);

		assertTrue(q.toString().contains("\"example text\""));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetLangStringArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setLangArg(VALUE_VAR_IN_TEST_QUERY, "example text", "en-US");

		LOG.trace("Set language string argument:\n{}", q);

		assertTrue(q.toString().contains("\"example text\"@en-US"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetBooleanArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setArg(VALUE_VAR_IN_TEST_QUERY, true);

		if (LOG.isTraceEnabled()) {
			LOG.trace("Set Boolean argument:\n{}", q);
			try (Writer wtr = new FileWriter("boolean-query.txt")) {
				wtr.write(q.toString());
			} catch (IOException ex) {
				ex.printStackTrace();
				fail();
			}
		}

		Pattern regex = Pattern.compile(".*\\?prop +true$.*",
			Pattern.MULTILINE | Pattern.DOTALL);
		assertTrue(regex.matcher(q.toString()).matches());
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetIntegerArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setArg(VALUE_VAR_IN_TEST_QUERY, 42);

		LOG.trace("Set integer argument:\n{}", q);

		Pattern regex = Pattern.compile(".*\\?prop +42$.*",
			Pattern.MULTILINE | Pattern.DOTALL);
		assertTrue(regex.matcher(q.toString()).matches());
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetDateTimeArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		Calendar cal = new Calendar.Builder()
			.setCalendarType("iso8601")
			.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
			.setDate(2019, 3, 1)
			.setTimeOfDay(9, 17, 33, 973)
			.build();
		q.setArg(VALUE_VAR_IN_TEST_QUERY, cal);

		LOG.trace("Set date-time argument:\n{}", q);

		assertTrue(q.toString().contains("\"2019-04-01T09:17:33.973Z\"^^xsd:dateTime"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetTypedArg() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.setTypedArg(VALUE_VAR_IN_TEST_QUERY, "42", "xsd:nonNegativeInteger");

		LOG.trace("Set typed argument:\n{}", q);

		assertTrue(q.toString().contains("\"42\"^^xsd:nonNegativeInteger"));
	}

	private static final String SET_VALUES_EXPECTED_RESULT = ""
		+ "\"ValueForMarkerAValue1Var1\",\n"
		+ "\"ValueForMarkerCValue1Var1\",\"ValueForMarkerCValue1Var2\"\n"
		+ "\"ValueForMarkerCValue2Var1\",\"ValueForMarkerCValue2Var2\"\n"
		+ "\"ValueForMarkerDValue1Var1\",\"42\"^^xsd:int\n"
		+ "\"ValueForMarkerDValue2Var1\",ps:xyzzy\n"
		+ "\"ValueForMarkerDValue3Var1\",\"ValueForMarkerDValue3Var2\"\n"
		+ "ps:bar,\n"
		+ "ps:baz,\n"
		+ "ps:foo,";

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSetValues() throws Exception {
		PrefixMapping pm = PrefixMapping.Factory.create()
			.withDefaultMappings(PrefixMapping.Standard)
			.setNsPrefix("abox", "http://www.bbn.com/abox/rush/decomposer#")
			.setNsPrefix("ps", "http://www.bbn.com/ontologies/rush/problem-strategy#");
		Query q = QueryBuilder.fromRsrc("TestSelectQueryWithValues.sparql", QueryBuilderTest.class, pm)
			.setValues(VAR1_VAR_IN_TEST_QUERY_WITH_VALUES, "markerA", Arrays.asList(
				ResourceFactory.createPlainLiteral("ValueForMarkerAValue1Var1")))
			.setValues(VAR1_VAR_IN_TEST_QUERY_WITH_VALUES, pm.expandPrefix("ps:markerB"),
				ResourceFactory.createResource(pm.expandPrefix("ps:foo")),
				ResourceFactory.createResource(pm.expandPrefix("ps:bar")),
				ResourceFactory.createResource(pm.expandPrefix("ps:baz")))
			.setValues(Arrays.asList(VAR1_VAR_IN_TEST_QUERY_WITH_VALUES, "var2"), "markerC",
				Arrays.asList(
					ResourceFactory.createPlainLiteral("ValueForMarkerCValue1Var1"),
					ResourceFactory.createPlainLiteral("ValueForMarkerCValue1Var2")),
				Arrays.asList(
					ResourceFactory.createPlainLiteral("ValueForMarkerCValue2Var1"),
					ResourceFactory.createPlainLiteral("ValueForMarkerCValue2Var2"))
				)
			.setValues(Arrays.asList(VAR1_VAR_IN_TEST_QUERY_WITH_VALUES, "var2"), "markerD",
				Arrays.asList(
					ResourceFactory.createPlainLiteral("ValueForMarkerDValue1Var1"),
					ResourceFactory.createTypedLiteral(42)),
				Arrays.asList(
					ResourceFactory.createPlainLiteral("ValueForMarkerDValue2Var1"),
					ResourceFactory.createResource(pm.expandPrefix("ps:xyzzy"))),
				Arrays.asList(
					ResourceFactory.createPlainLiteral("ValueForMarkerDValue3Var1"),
					ResourceFactory.createPlainLiteral("ValueForMarkerDValue3Var2"))
				)
			.asQuery();

		LOG.trace("SELECT with set VALUES:\n{}", q);

		Model model = ModelFactory.createDefaultModel();	// NOPMD - DataflowAnomalyAnalysis (Deprecated)
		try (QueryExecution qe = QueryExecutionFactory.create(q, model)) {
			ResultSet resultSet = qe.execSelect();
			SortedSet<String> results = new TreeSet<>();
			while (resultSet.hasNext()) {
				QuerySolution qs = resultSet.next();
				RDFNode node1 = qs.get(VAR1_VAR_IN_TEST_QUERY_WITH_VALUES);
				RDFNode node2 = qs.get("var2");
				results.add(asString(node1, pm) + "," + asString(node2, pm));
			}
			String resultStr = results.stream().collect(Collectors.joining("\n"));

			LOG.trace("Result from set VALUES:\n{}", resultStr);

			assertEquals(SET_VALUES_EXPECTED_RESULT, resultStr);
		}
	}

	private static String asString(RDFNode node, PrefixMapping pm) {
		if (node == null) {
			return "";
		} else if (node.isURIResource()) {
			return pm.shortForm(node.asResource().getURI());
		} else if (node.isLiteral()) {
			if (node.asLiteral().getDatatypeURI() == null
					|| node.asLiteral().getDatatypeURI().equals(XSD.xstring.toString())) {
				return String.format("\"%1$s\"", node.asLiteral().getLexicalForm());
			} else if (node.asLiteral().getDatatypeURI().equals(pm.expandPrefix("rdf:langString"))) {
				return String.format("\"%1$s\"@%2$s", node.asLiteral().getLexicalForm(), node.asLiteral().getLanguage());
			} else {
				return String.format("\"%1$s\"^^%2$s", node.asLiteral().getLexicalForm(), pm.shortForm(node.asLiteral().getDatatypeURI()));
			}
		} else {
			return "unrecognized_node_type";
		}
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testScopeSelectToGraph() {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.scopeToGraph("abox:TestGraph");

		LOG.trace("SELECT scoped to graph:\n{}", q);

		assertTrue(q.toString().contains("GRAPH abox:TestGraph"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testFilterRegex() throws Exception {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.addFilter(ExprFactory.regEx(VALUE_VAR_IN_TEST_QUERY, "(reg|ex|filt)", RegExFlag.CASE_INSENSITIVE));

		LOG.trace("SELECT with filter REGEX:\n{}", q);

		assertTrue(q.toString().contains("FILTER regex(?value, \"(reg|ex|filt)\", \"i\")"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testFilterGreaterThan() throws Exception {
		QueryBuilder q = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		q.addFilter(ExprFactory.greaterThanOrEqual(VALUE_VAR_IN_TEST_QUERY, 10));

		LOG.trace("SELECT with filter REGEX:\n{}", q);

		assertTrue(q.toString().contains("FILTER ( ?value >= 10 )"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testUnionQuery() throws Exception {
		QueryBuilder q1 = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);
		QueryBuilder q2 = QueryBuilder.fromRsrc(QUERY_FILE, QueryBuilderTest.class);

		q1.union(q2.asQuery());

		LOG.trace("SELECT with UNION:\n{}", q1);

		// TODO Use REGEX to verify correct { ... } UNION { ... } structure
		assertTrue(q1.toString().contains("UNION"));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void addSubQuery() throws Exception {
		QueryBuilder q1 = QueryBuilder.fromString("SELECT * WHERE { ?uri a ?type }");
		QueryBuilder q2 = QueryBuilder.fromString("SELECT ?uri2 ?type2 WHERE { ?uri2 a ?type2 }");
		q1.appendSubQuery(q2);

		assertTrue(q1.toString().contains("?uri2"));
		assertTrue(q1.toString().contains("?type2"));
	}
}
