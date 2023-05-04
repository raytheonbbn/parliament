
package com.bbn.parliament.kb_graph.query;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.resultset.ResultSetCompare;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.TestingDataset;

/** @author dkolas */
public class ReificationTest {
	private static final String PREFIXES = """
		prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
		prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		prefix owl:  <http://www.w3.org/2002/07/owl#>
		prefix xsd:  <http://www.w3.org/2001/XMLSchema#>
		prefix dc:   <http://purl.org/dc/elements/1.1/>
		prefix :     <http://example.org/foo#>
		""";

	private static final String INPUT_DATA = """
		@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
		@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
		@prefix owl:  <http://www.w3.org/2002/07/owl#> .
		@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .
		@prefix dc:   <http://purl.org/dc/elements/1.1/> .
		@prefix : <http://example.org/foo#> .

		:A :B :C .
		:D :E :F .
		:G :H :I .

		:J a rdf:Statement;
			rdf:subject :A;
			rdf:predicate :B;
			rdf:object :C;
			dc:creator :X .

		:J2 a rdf:Statement;
			rdf:subject :A;
			rdf:predicate :B;
			rdf:object :C;
			dc:creator :Y .

		:J3 a rdf:Statement;
			rdf:subject :A;
			rdf:predicate :B;
			rdf:object :C;
			dc:creator :X .

		:J4 a rdf:Statement;
			rdf:subject :D;
			rdf:predicate :B;
			rdf:object :C ;
			dc:creator :X .

		:J :isBetterThan :J2 .

		:A1 dc:creator :C1 .
		:A2 dc:creator :C2 .
		:A3 dc:creator :C3 .
		:A4 dc:creator :C4 .

		:Jpartial rdf:subject :A .
		""";

	private static TestingDataset dataset;

	@BeforeAll
	public static void beforeAll() {
		dataset = new TestingDataset();
	}

	@AfterAll
	public static void afterAll() {
		dataset.clear();
	}

	private Model jenaDefaultModel;
	protected Model defaultGraphModel;

	@BeforeEach
	public void beforeEach(){
		@SuppressWarnings("resource")
		KbGraph defaultGraph = dataset.getDefaultGraph();
		defaultGraphModel = ModelFactory.createModelForGraph(defaultGraph);
		jenaDefaultModel = ModelFactory.createDefaultModel();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		dataset.reset();
	}

	@Test
	public void testReificationQuery1(){
		String query = PREFIXES + """
				select ?stmt ?creator where {
					?stmt a rdf:Statement;
						rdf:subject :A;
						rdf:predicate :B;
						rdf:object :C;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery2(){
		String query = PREFIXES + """
				select ?stmt ?p ?creator where {
					?stmt a rdf:Statement;
						rdf:subject :A;
						rdf:predicate ?p;
						rdf:object :C;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery3(){
		String query = PREFIXES + """
				select ?stmt ?p ?p2 ?creator where {
					?stmt a rdf:Statement;
						rdf:subject :A;
						rdf:predicate ?p;
						rdf:predicate ?p2;
						rdf:object :C;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery4(){
		String query = PREFIXES + """
				select ?stmt ?s where {
					?stmt a rdf:Statement;
						rdf:subject ?s;
						rdf:predicate :B;
						rdf:object :C;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery5(){
		String query = PREFIXES + """
				select ?stmt ?s ?p ?o where {
					?stmt a rdf:Statement;
						rdf:subject ?s;
						rdf:predicate ?p;
						rdf:object ?o;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery6(){
		String query = PREFIXES + """
				select ?stmt ?s where {
					?stmt a rdf:Statement;
						rdf:subject ?s;
						rdf:predicate :B;
						rdf:object :C;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery7(){
		String query = PREFIXES + """
				select ?s ?p ?o ?creator where {
					:J2 a rdf:Statement;
						rdf:subject ?s;
						rdf:predicate ?p;
						rdf:object ?o;
						dc:creator ?creator .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery8(){
		String query = PREFIXES + """
				select ?s1 ?s2 ?o where {
					?s1 a rdf:Statement;
						rdf:predicate :B;
						:isBetterThan ?s2 .
					?s2 a rdf:Statement;
						rdf:subject ?s;
						rdf:predicate :B;
						rdf:object ?o .
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery9(){
		String query = PREFIXES + """
				select ?s1 ?o where {
					?s1 a rdf:Statement;
						rdf:predicate :B;
				}
				""";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery10(){
		String query = PREFIXES + """
				select ?s1 ?c where {
					?s1 rdf:predicate ?b;
						dc:creator ?c .
				}
				""";
		testAQuery(query);
	}


	// This test will fail until partial reifications on insert are handled somehow.
	//@Test
	public void testPartialReificationQuery(){
		String query = PREFIXES + """
				select ?stmt ?s where {
					?stmt rdf:subject ?s.
				}
				""";
		testAQuery(query);
	}

	private void testAQuery(String query){
		defaultGraphModel.read(new StringReader(INPUT_DATA), "", "TURTLE");
		jenaDefaultModel.read(new StringReader(INPUT_DATA), "", "TURTLE");

		ResultSetRewindable resultSet;
		try (QueryExecution execution = QueryExecutionFactory.create(query, dataset.getGraphStore().toDataset())) {
			resultSet = ResultSetFactory.copyResults(execution.execSelect());
		}

		ResultSetRewindable jenaResultSet;
		try (QueryExecution execution2 = QueryExecutionFactory.create(query, jenaDefaultModel)) {
			jenaResultSet = ResultSetFactory.copyResults(execution2.execSelect());
		}

		assertTrue(equals(jenaResultSet, resultSet, QueryFactory.create(query)), "Result sets did not match.");
	}

	protected static ResultSetRewindable makeUnique(ResultSetRewindable results) {
		// VERY crude. Utilises the fact that bindings have value equality.
		List<Binding> x = new ArrayList<>();
		Set<Binding> seen = new HashSet<>();

		for (; results.hasNext();) {
			Binding b = results.nextBinding();
			if (seen.contains(b))
				continue;
			seen.add(b);
			x.add(b);
		}
		QueryIterator qIter = new QueryIterPlainWrapper(x.iterator());
		ResultSet rs = new ResultSetStream(results.getResultVars(),
				ModelFactory.createDefaultModel(), qIter);
		return ResultSetFactory.makeRewindable(rs);
	}

	protected static boolean equals(ResultSetRewindable expected,
			ResultSetRewindable actual, Query query) {
		ResultSetRewindable exp = expected;
		ResultSetRewindable act = actual;
		if (query.isReduced()) {
			exp = makeUnique(exp);
			act = makeUnique(act);
		}
		if (query.isOrdered()) {
			return ResultSetCompare.equalsByValueAndOrder(exp, act);
		}
		return ResultSetCompare.equalsByValue(exp, act);
	}
}
