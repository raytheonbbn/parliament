package com.bbn.parliament.jena.query.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.sse.SSE;

/**
 * Taken from ARQ OptimizerTestCase
 *
 * @author rbattle
 */
public class KbOptimizerTestCase {

	@BeforeClass
	public static void register() {
		KbOptimize.register();
	}

	@SuppressWarnings("static-method")
	@Test public void query_rename_01()
	{
		String queryString =
			"SELECT ?x { ?s ?p ?o . { SELECT ?v { ?x ?y ?v {SELECT ?w { ?a ?y ?w }}} LIMIT 50 } }" ;
		String opExpectedString =
			"(project (?x)\n" +
				"  (join\n" +
				"    (bgp (triple ?s ?p ?o))\n" +
				"    (slice _ 50\n" +
				"      (project (?v)\n" +
				"        (join\n" +
				"          (bgp (triple ?x ?y ?v))\n" +
				"          (project (?w)\n" +
				"            (bgp (triple ?a ?y ?w))))))))";
		check(queryString, opExpectedString) ;
	}


	@SuppressWarnings("static-method")
	@Test public void query_rename_02()
	{
		String queryString =
			"SELECT ?x { ?s ?p ?o . { SELECT ?v { ?x ?y ?v {SELECT * { ?a ?y ?w }}} LIMIT 50 } }"  ;
		String opExpectedString =
			"(project (?x)\n" +
				"  (join\n" +
				"    (bgp (triple ?s ?p ?o))\n" +
				"    (slice _ 50\n" +
				"      (project (?v)\n" +
				"        (sequence\n" +
				"          (bgp (triple ?x ?y ?v))\n" +
				"          (bgp (triple ?a ?y ?w)))))))" ;
		check(queryString, opExpectedString) ;
	}

	@SuppressWarnings("static-method")
	@Test public void query_rename_03()
	{
		String queryString = "SELECT ?x { ?s ?p ?o . { SELECT * { ?x ?y ?v {SELECT ?w { ?a ?y ?w }}} LIMIT 50 } }" ;
		String opExpectedString =
			"(project (?x)\n" +
				"  (join\n" +
				"    (bgp (triple ?s ?p ?o))\n" +
				"    (slice _ 50\n" +
				"      (join\n" +
				"        (bgp (triple ?x ?y ?v))\n" +
				"        (project (?w)\n" +
				"          (bgp (triple ?a ?y ?w)))))))" ;
		check(queryString, opExpectedString) ;
	}

	@SuppressWarnings("static-method")
	@Test public void query_rename_04()
	{
		String queryString = "SELECT * { ?s ?p ?o . { SELECT ?v { ?x ?y ?v {SELECT ?w { ?a ?y ?w }}} LIMIT 50 } }" ;
		String opExpectedString =
			"(join\n" +
				"  (bgp (triple ?s ?p ?o))\n" +
				"  (slice _ 50\n" +
				"    (project (?v)\n" +
				"      (join\n" +
				"        (bgp (triple ?x ?y ?v))\n" +
				"        (project (?w)\n" +
				"          (bgp (triple ?a ?y ?w)))))))" ;
		check(queryString, opExpectedString) ;
	}

	@SuppressWarnings("static-method")
	@Test public void query_rename_05()
	{
		String queryString = "SELECT ?v { ?s ?p ?o . { SELECT ?v { ?x ?y ?v {SELECT ?w { ?a ?y ?w }}} LIMIT 50 } }"    ;
		String opExpectedString =
			"(project (?v)\n" +
				"  (join\n" +
				"    (bgp (triple ?s ?p ?o))\n" +
				"    (slice _ 50\n" +
				"      (project (?v)\n" +
				"        (join\n" +
				"          (bgp (triple ?x ?y ?v))\n" +
				"          (project (?w)\n" +
				"            (bgp (triple ?a ?y ?w))))))))" ;
		check(queryString, opExpectedString) ;
	}

	@SuppressWarnings("static-method")
	@Test public void query_rename_06()
	{
		String queryString = "SELECT ?w { ?s ?p ?o . { SELECT ?w { ?x ?y ?v {SELECT ?w { ?a ?y ?w }}} } } LIMIT 50" ;
		String opExpectedString =
			"(slice _ 50\n" +
				"  (project (?w)\n" +
				"    (join\n" +
				"      (bgp (triple ?s ?p ?o))\n" +
				"      (project (?w)\n" +
				"        (join\n" +
				"          (bgp (triple ?x ?y ?v))\n" +
				"          (project (?w)\n" +
				"            (bgp (triple ?a ?y ?w))))))))\n" +
				"" ;
		check(queryString, opExpectedString) ;
	}

	@SuppressWarnings("static-method")
	@Test public void query_rename_07()
	{
		String queryString = "SELECT * { ?s ?p ?o . { SELECT ?w { ?x ?y ?v }}}"  ;
		String opExpectedString =
			"(join\n" +
				"  (bgp (triple ?s ?p ?o))\n" +
				"  (project (?w)\n" +
				"    (bgp (triple ?x ?y ?v))))" ;
		check(queryString, opExpectedString) ;
	}

	private static void check(String queryString, String opExpectedString)
	{
		queryString = "PREFIX : <http://example/>\n"+queryString ;
		Query query = QueryFactory.create(queryString) ;
		Op opQuery = Algebra.compile(query) ;
		check(opQuery, opExpectedString) ;
	}

	private static void check(Op opToOptimize, String opExpectedString)
	{
		Op opOptimize = Algebra.optimize(opToOptimize) ;
		Op opExpected = SSE.parseOp(opExpectedString) ;
		assertEquals(opExpected, opOptimize) ;
	}
}
