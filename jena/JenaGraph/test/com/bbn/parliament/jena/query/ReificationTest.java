
package com.bbn.parliament.jena.query;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.TestingDataset;
import com.bbn.parliament.jena.graph.KbGraph;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.ResultSetStream;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare;

/** @author dkolas */
@RunWith(JUnitPlatform.class)
public class ReificationTest {
	private static final String PREFIXES = "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\n" +
			"PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> \r\n" +
			"PREFIX  owl:  <http://www.w3.org/2002/07/owl#> \r\n" +
			"PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#> \r\n" +
			"PREFIX  dc:   <http://purl.org/dc/elements/1.1/> \r\n" +
			"PREFIX  : <http://example.org/foo#> \r\n";

	private static final String INPUT_DATA = "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" +
			"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" +
			"@prefix owl:  <http://www.w3.org/2002/07/owl#> .\r\n" +
			"@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .\r\n" +
			"@prefix dc:   <http://purl.org/dc/elements/1.1/> .\r\n" +
			"@prefix : <http://example.org/foo#> .\r\n" +
			"\r\n" +
			":A :B :C .\r\n" +
			":D :E :F .\r\n" +
			":G :H :I .\r\n" +
			"\r\n" +

			":J a rdf:Statement;\r\n" +
			"	rdf:subject :A;\r\n" +
			"	rdf:predicate :B;\r\n" +
			"	rdf:object :C;\r\n" +
			"	dc:creator :X .\n" +

			":J2 a rdf:Statement;\r\n" +
			"	rdf:subject :A;\r\n" +
			"	rdf:predicate :B;\r\n" +
			"	rdf:object :C;\r\n" +
			"	dc:creator :Y .\n" +

			":J3 a rdf:Statement;\r\n" +
			"	rdf:subject :A;\r\n" +
			"	rdf:predicate :B;\r\n" +
			"	rdf:object :C;\r\n" +
			"	dc:creator :X .\n" +

			":J4 a rdf:Statement;\r\n" +
			"	rdf:subject :D;\r\n" +
			"	rdf:predicate :B;\r\n" +
			"	rdf:object :C ;\r\n" +
			"	dc:creator :X .\n" +

			":J :isBetterThan :J2 .\n" +

			":A1 dc:creator :C1 .\n" +
			":A2 dc:creator :C2 .\n" +
			":A3 dc:creator :C3 .\n" +
			":A4 dc:creator :C4 .\n" +

			":Jpartial rdf:subject :A .\n" +
			"";

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
		String query = PREFIXES +
				"SELECT ?stmt ?creator \n" +
				"WHERE {\n" +
				"	?stmt a rdf:Statement;\n" +
				"		rdf:subject :A;\n" +
				"		rdf:predicate :B;\n" +
				"		rdf:object :C;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery2(){
		String query = PREFIXES +
				"SELECT ?stmt ?p ?creator \n" +
				"WHERE {\n" +
				"	?stmt a rdf:Statement;\n" +
				"		rdf:subject :A;\n" +
				"		rdf:predicate ?p;\n" +
				"		rdf:object :C;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery3(){
		String query = PREFIXES +
				"SELECT ?stmt ?p ?p2 ?creator \n" +
				"WHERE {\n" +
				"	?stmt a rdf:Statement;\n" +
				"		rdf:subject :A;\n" +
				"		rdf:predicate ?p;\n" +
				"		rdf:predicate ?p2;\n" +
				"		rdf:object :C;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery4(){
		String query = PREFIXES +
				"SELECT ?stmt ?s \n" +
				"WHERE {\n" +
				"	?stmt a rdf:Statement;\n" +
				"		rdf:subject ?s;\n" +
				"		rdf:predicate :B;\n" +
				"		rdf:object :C;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery5(){
		String query = PREFIXES +
				"SELECT ?stmt ?s ?p ?o \n" +
				"WHERE {\n" +
				"	?stmt a rdf:Statement;\n" +
				"		rdf:subject ?s;\n" +
				"		rdf:predicate ?p;\n" +
				"		rdf:object ?o;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery6(){
		String query = PREFIXES +
				"SELECT ?stmt ?s \n" +
				"WHERE {\n" +
				"	?stmt a rdf:Statement;\n" +
				"		rdf:subject ?s;\n" +
				"		rdf:predicate :B;\n" +
				"		rdf:object :C;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery7(){
		String query = PREFIXES +
				"SELECT ?s ?p ?o ?creator\n" +
				"WHERE {\n" +
				"	:J2 a rdf:Statement;\n" +
				"		rdf:subject ?s;\n" +
				"		rdf:predicate ?p;\n" +
				"		rdf:object ?o;\n" +
				"		dc:creator ?creator .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery8(){
		String query = PREFIXES +
				"SELECT ?s1 ?s2 ?o\n" +
				"WHERE {\n" +
				"	?s1 a rdf:Statement;\n" +
				"		rdf:predicate :B;\n" +
				"		:isBetterThan ?s2 .\n" +
				"	?s2 a rdf:Statement;\n" +
				"		rdf:subject ?s;\n" +
				"		rdf:predicate :B;\n" +
				"		rdf:object ?o .\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery9(){
		String query = PREFIXES +
				"SELECT ?s1 ?o\n" +
				"WHERE {\n" +
				"	?s1 a rdf:Statement;\n" +
				"		rdf:predicate :B;\n" +
				"}";
		testAQuery(query);
	}

	@Test
	public void testReificationQuery10(){
		String query = PREFIXES +
				"SELECT ?s1 ?c\n" +
				"WHERE {\n" +
				"	?s1 rdf:predicate ?b;\n" +
				"		dc:creator ?c .\n" +
				"}";
		testAQuery(query);
	}


	// This test will fail until partial reifications on insert are handled somehow.
	//@Test
	public void testPartialReificationQuery(){
		String query = PREFIXES +
				"SELECT ?stmt ?s \n" +
				"WHERE {\n" +
				"	?stmt rdf:subject ?s.\n" +
				"}";
		testAQuery(query);
	}

	private void testAQuery(String query){
		defaultGraphModel.read(new StringReader(INPUT_DATA), "", "TURTLE");
		jenaDefaultModel.read(new StringReader(INPUT_DATA), "", "TURTLE");

		QueryExecution execution = QueryExecutionFactory.create(query, dataset.getGraphStore().toDataset());
		ResultSetRewindable resultSet = ResultSetFactory.copyResults(execution.execSelect());
		execution.close();

		QueryExecution execution2 = QueryExecutionFactory.create(query, jenaDefaultModel);
		ResultSetRewindable jenaResultSet = ResultSetFactory.copyResults(execution2.execSelect());
		execution2.close();

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
