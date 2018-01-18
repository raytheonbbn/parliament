package com.bbn.parliament.jena.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.util.Context;

public class KbOpExecutorTest extends AbstractKbTestCase {

	private ExecutionContext execCxt;
	private KbOpExecutor opExecutor;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		Context params = ARQ.getContext();
		execCxt = new ExecutionContext(params, defaultGraph, dataset,
			KbOpExecutor.KbOpExecutorFactory);
		opExecutor = new KbOpExecutor(execCxt);
	}

	protected QueryIterator createInput() {
		return QueryIterRoot.create(execCxt);
	}

	private QueryIterator createIterator(Triple... triples) {
		QueryIterator input = QueryIterRoot.create(execCxt);

		OpBGP opBGP = new OpBGP();
		for (Triple triple : triples) {
			opBGP.getPattern().add(triple);
		}
		QueryIterator it = opExecutor.execute(opBGP, input);
		return it;
	}

	@Test
	public void testExecuteOpBGPSimple() throws IOException {
		loadResource("data/data-r2/triple-match/data-02.ttl", getGraph());

		QueryIterator it = createIterator(Triple.create(ResourceFactory
			.createResource("http://example.org/data/x").asNode(), Var
			.alloc("p"), Var.alloc("o")));
		assertTrue(it.hasNext());
		int count = 0;
		while (it.hasNext()) {
			it.next();
			count++;
		}
		assertEquals(1, count);
	}

	@Test
	public void testFilterSequence() throws IOException {
		String algebra;
		Op op;
		QueryIterator it;
		int count = 0;

		loadResource("data/data-r2/triple-match/dawg-data-01.ttl", getNamedGraph());

		// no filter
		algebra = ""
			+ "(distinct\n"
			+ "  (project (?s ?name)\n"
			+ "      (sequence\n"
			+ "        (graph ?g (bgp (triple ?s <http://xmlns.com/foaf/0.1/name> ?name)))\n"
			+ "        (graph ?g (bgp (triple ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person>)))"
			+ "      )))\n";

		op = SSE.parseOp(algebra);

		count = 0;
		it = opExecutor.executeOp(op, createInput());
		while (it.hasNext()) {
			count++;
			it.next();
		}
		assertEquals(3, count);

		// filter name
		algebra = ""
			+ "(distinct\n"
			+ "  (project (?s ?name)\n"
			+ "    (filter (<http://www.w3.org/2005/xpath-functions#contains> (<http://www.w3.org/2005/xpath-functions#lower-case> ?name) (<http://www.w3.org/2005/xpath-functions#lower-case> \"bob\"))\n"
			+ "      (sequence\n"
			+ "        (graph ?g (bgp (triple ?s <http://xmlns.com/foaf/0.1/name> ?name)))\n"
			+ "        (graph ?g (bgp (triple ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person>)))"
			+ "      ))))\n";

		op = SSE.parseOp(algebra);
		count = 0;
		it = opExecutor.executeOp(op, createInput());
		while (it.hasNext()) {
			count++;
			it.next();
		}
		assertEquals(1, count);
	}

	@Test
	public void testFilter() throws IOException {
		loadResource("data/data-r2/sort/data-sort-numbers.ttl", getGraph());

		String algebra = "";
		algebra = ""
			+ "(project (?s ?p ?o)\n"
			+ "  (filter (&& (< ?o 200) (> ?o 5))\n"
			+ "    (bgp (triple ?s ?p ?o))))";

		Op op = SSE.parseOp(algebra);
		int count = 0;
		QueryIterator it = opExecutor.executeOp(op, createInput());
		while (it.hasNext()) {
			count++;
			it.next();
		}
		assertEquals(3, count);
	}

	@Test
	public void testFilterSequenceBGP() throws IOException {
		String algebra;
		Op op;
		QueryIterator it;
		int count = 0;

		loadResource("data/data-r2/triple-match/dawg-data-01.ttl", getGraph());

		// filter name
		algebra = ""
			+ "(distinct\n"
			+ "  (project (?s ?name)\n"
			+ "    (filter (<http://www.w3.org/2005/xpath-functions#contains> (<http://www.w3.org/2005/xpath-functions#lower-case> ?name) (<http://www.w3.org/2005/xpath-functions#lower-case> \"bob\"))\n"
			+ "      (sequence\n"
			+ "        (bgp (triple ?s <http://xmlns.com/foaf/0.1/name> ?name))\n"
			+ "        (bgp (triple ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person>))"
			+ "      ))))\n";

		op = SSE.parseOp(algebra);
		count = 0;
		it = opExecutor.executeOp(op, createInput());
		while (it.hasNext()) {
			count++;
			it.next();
		}
		assertEquals(1, count);
	}

	@Test
	@Ignore
	public void testExecuteOpBGPBound() {
	}

	@Test
	@Ignore
	public void testExecuteOpBGPComplex() {
	}
}
