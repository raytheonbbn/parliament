package com.bbn.parliament.jena.query.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
							(join
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
						(join
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
						(join
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
							(join
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
					(join
						(bgp (triple ?s ?p ?o))
						(project (?w)
							(join
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
			(join
				(bgp (triple ?s ?p ?o))
				(project (?w)
					(bgp (triple ?x ?y ?v))))
			""";
		check(queryString, opExpectedString);
	}

	private static void check(String queryString, String opExpectedString)
	{
		queryString = "prefix : <http://example.org/>\n"+queryString;
		Query query = QueryFactory.create(queryString);
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
