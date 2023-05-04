package com.bbn.parliament.kb_graph.query.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Taken from ARQ OptimizerTestCase
 *
 * @author rbattle
 */
public class KbOptimizerTest {
	@BeforeAll
	public static void beforeAll() {
		KbOptimize.register();
	}

	@SuppressWarnings("static-method")
	@Test
	public void query_rename_01()
	{
		String queryString =
			"select ?x { ?s ?p ?o . { select ?v { ?x ?y ?v {select ?w { ?a ?y ?w }}} limit 50 } }";
		String opExpectedString = """
			(project (?x)
				(join
					(bgp (triple ?s ?p ?o))
					(slice _ 50
						(project (?v)
							(sequence
								(bgp (triple ?x ?y ?v))
								(project (?w)
									(bgp (triple ?a ?y ?w))))))))
			""";
		check(queryString, opExpectedString);
	}


	@SuppressWarnings("static-method")
	@Test
	public void query_rename_02()
	{
		String queryString =
			"select ?x { ?s ?p ?o . { select ?v { ?x ?y ?v {select * { ?a ?y ?w }}} limit 50 } }";
		String opExpectedString = """
			(project (?x)
				(join
					(bgp (triple ?s ?p ?o))
					(slice _ 50
						(project (?v)
							(sequence
								(bgp (triple ?x ?y ?v))
								(bgp (triple ?a ?y ?w)))))))
			""";
		check(queryString, opExpectedString);
	}

	@SuppressWarnings("static-method")
	@Test
	public void query_rename_03()
	{
		String queryString = "select ?x { ?s ?p ?o . { select * { ?x ?y ?v {select ?w { ?a ?y ?w }}} limit 50 } }";
		String opExpectedString = """
			(project (?x)
				(join
					(bgp (triple ?s ?p ?o))
					(slice _ 50
						(sequence
							(bgp (triple ?x ?y ?v))
							(project (?w)
								(bgp (triple ?a ?y ?w)))))))
			""";
		check(queryString, opExpectedString);
	}

	@SuppressWarnings("static-method")
	@Test
	public void query_rename_04()
	{
		String queryString = "select * { ?s ?p ?o . { select ?v { ?x ?y ?v {select ?w { ?a ?y ?w }}} limit 50 } }";
		String opExpectedString = """
			(join
				(bgp (triple ?s ?p ?o))
				(slice _ 50
					(project (?v)
						(sequence
							(bgp (triple ?x ?y ?v))
							(project (?w)
								(bgp (triple ?a ?y ?w)))))))
			""";
		check(queryString, opExpectedString);
	}

	@SuppressWarnings("static-method")
	@Test
	public void query_rename_05()
	{
		String queryString = "select ?v { ?s ?p ?o . { select ?v { ?x ?y ?v {select ?w { ?a ?y ?w }}} limit 50 } }";
		String opExpectedString = """
			(project (?v)
				(join
					(bgp (triple ?s ?p ?o))
					(slice _ 50
						(project (?v)
							(sequence
								(bgp (triple ?x ?y ?v))
								(project (?w)
									(bgp (triple ?a ?y ?w))))))))
			""";
		check(queryString, opExpectedString);
	}

	@SuppressWarnings("static-method")
	@Test
	public void query_rename_06()
	{
		String queryString = "select ?w { ?s ?p ?o . { select ?w { ?x ?y ?v {select ?w { ?a ?y ?w }}} } } limit 50";
		String opExpectedString = """
			(slice _ 50
				(project (?w)
					(sequence
						(bgp (triple ?s ?p ?o))
						(project (?w)
							(sequence
								(bgp (triple ?x ?y ?v))
								(project (?w)
									(bgp (triple ?a ?y ?w))))))))
			""";
		check(queryString, opExpectedString);
	}

	@SuppressWarnings("static-method")
	@Test
	public void query_rename_07()
	{
		String queryString = "select * { ?s ?p ?o . { select ?w { ?x ?y ?v }}}";
		String opExpectedString = """
			(sequence
				(bgp (triple ?s ?p ?o))
				(project (?w)
					(bgp (triple ?x ?y ?v))))
			""";
		check(queryString, opExpectedString);
	}

	private static void check(String queryString, String opExpectedString)
	{
		Query query = QueryFactory.create("prefix : <http://example.org/>\n" + queryString);
		Op opQuery = Algebra.compile(query);
		check(opQuery, opExpectedString);
	}

	private static void check(Op opToOptimize, String opExpectedString)
	{
		Op opOptimize = Algebra.optimize(opToOptimize);
		Op opExpected = SSE.parseOp(opExpectedString);
		assertEquals(opExpected, opOptimize);
	}
}
