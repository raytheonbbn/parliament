package com.bbn.parliament.jena.query.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bbn.parliament.jena.Kb;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.query.AbstractKbTestCase;
import com.bbn.parliament.jena.query.KbOpExecutor;
import com.bbn.parliament.jena.query.index.mock.MockIndex;
import com.bbn.parliament.jena.query.index.mock.MockIndexFactory;
import com.bbn.parliament.jena.query.index.mock.MockPatternQuerier;
import com.bbn.parliament.jena.query.index.mock.MockPropertyFunction;
import com.bbn.parliament.jena.query.optimize.KbOptimize;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.Op1;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.optimize.Rewrite;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.main.OpExecutor;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.util.Context;

public class IndexExecutionTest extends AbstractKbTestCase {
	private static final int NUM_MOCKED_ITEMS = 5;

	private static MockIndexFactory factory;
	private static Rewrite optimizer;

	private OpExecutor opExecutor;
	private ExecutionContext execCxt;
	private MockIndex index;
	private MockPatternQuerier querier;

	@BeforeClass
	public static void setUpKb() {
		Kb.init();
		factory = new MockIndexFactory();
		IndexFactoryRegistry.getInstance().register(factory);
		optimizer = KbOptimize.factory.create(ARQ.getContext());
	}

	protected QueryIterator createInput() {
		return OpExecutor.createRootQueryIterator(execCxt);
	}

	private static int getNumStatements(Op op){
		Op sub = ((Op1) op).getSubOp();
		return ((OpBGP) sub).getPattern().size();
	}

	@Before
	public void setUpIndex() {
		Context params = ARQ.getContext();
		execCxt = new ExecutionContext(params, defaultGraph, dataset,
			KbOpExecutor.KbOpExecutorFactory);
		opExecutor = KbOpExecutor.KbOpExecutorFactory.create(execCxt);

		index = IndexManager.getInstance().createAndRegister(getGraph(), null, factory);
		querier = new MockPatternQuerier(NUM_MOCKED_ITEMS);
	}

	@After
	public void tearDownIndex() {
		IndexManager.getInstance().unregister(getGraph(), null, index);
		IndexPatternQuerierManager.getInstance().unregister(index);
	}

	@Test
	public void testIndexPropertyFunction() {
		//loadResource("com/bbn/parliament/jena/query/index/mock/mockdata.ttl", getGraph());
		String algebra = ""
			+ "(project (?x ?y)\n"
			+ "  (bgp\n"
			+ "    (triple ?x <" + MockPropertyFunction.URI + "> ?y)\n"
			+ "))";

		Op op = SSE.parseOp(algebra);
		op = optimizer.rewrite(op);

		QueryIterator it = opExecutor.executeOp(op, createInput());
		assertTrue(it.hasNext());
		assertTrue(MockPropertyFunction.isCalled());
	}

	@Test
	public void testIndexPatternQuerier1() {
		String algebra = ""
			+ "(project (?x ?y)\n"
			+ "  (bgp\n"
			//+ "    (triple ?x <http://example.org/data/p> <http://example.org/data/v1>)\n"
			+ "    (triple ?x <http://example.org/mock#object> ?y)\n"
			+ "))";

		Op op = SSE.parseOp(algebra);
		op = optimizer.rewrite(op);
		int patterncount = getNumStatements(op);

		QueryIterator it = opExecutor.executeOp(op, createInput());
		assertFalse(it.hasNext());
		assertFalse(querier.isExamined());
		assertFalse(querier.isEstimated());
		assertFalse(querier.isQueried());

		// register querier now
		IndexPatternQuerierManager.getInstance().register(index, querier);
		it = opExecutor.executeOp(op, createInput());
		assertTrue(querier.isExamined());
		assertTrue(it.hasNext());
		assertTrue(querier.isEstimated() || patterncount == 1);	// Singleton patterns are not estimated
		assertTrue(querier.isQueried());
		int counter = 0;
		while (it.hasNext()) {
			assertFalse(querier.hasBinding());
			it.next();
			++counter;
		}
		assertEquals(NUM_MOCKED_ITEMS, counter);
	}

	@Test
	public void testIndexPatternQuerier2() throws IOException {
		loadResource("data/data-r2/triple-match/data-01.ttl", getGraph());

		String algebra = ""
			+ "(project (?x ?y)\n"
			+ "  (bgp\n"
			+ "    (triple ?x <http://example.org/data/p> <http://example.org/data/v1>)\n"
			+ "    (triple ?x <http://example.org/mock#mocked> ?y)\n"
			+ "))";

		Op op = SSE.parseOp(algebra);
		op = optimizer.rewrite(op);
		int patterncount = getNumStatements(op);

		IndexPatternQuerierManager.getInstance().register(index, querier);
		QueryIterator it = opExecutor.executeOp(op, createInput());
		assertTrue(querier.isExamined());
		assertTrue(it.hasNext());
		assertTrue(querier.isEstimated() || patterncount == 1);	// Singleton patterns are not estimated
		assertTrue(querier.isQueried());
		int counter = 0;
		while (it.hasNext()) {
			Binding b = it.next();
			assertTrue(querier.hasBinding());
			assertTrue(b.get(Var.alloc("x")).isURI());
			assertTrue(b.get(Var.alloc("y")).isBlank());
			++counter;
		}
		assertEquals(NUM_MOCKED_ITEMS, counter);
	}

	@Test
	public void testIndexPatternQuerier3() throws IOException {
		loadResource("data/data-r2/triple-match/data-01.ttl", getGraph());

		String algebra = ""
			+ "(project (?x ?y)\n"
			+ "  (bgp\n"
			+ "    (triple <http://example.org/data/x> <" + MockPropertyFunction.URI + "> ?y)\n"
			+ "    (triple <http://example.org/data/x> <http://example.org/data/p> <http://example.org/data/v1> )\n"
			+ "    (triple ?x <http://example.org/data/p> <http://example.org/data/v1> )\n"
			+ "  )\n"
			+ ")";

		Op op = SSE.parseOp(algebra);
		op = optimizer.rewrite(op);

		QueryIterator it = opExecutor.executeOp(op, createInput());
		assertTrue(it.hasNext());
		assertTrue(MockPropertyFunction.isCalled());
		while (it.hasNext()) {
			Binding b = it.next();
			Node x = b.get(Var.alloc("x"));
			assertTrue(x.isURI());
			assertEquals("http://example.org/data/x", x.getURI());
		}
	}

	@Test
	public void testFilterableIndex() throws IOException {
		loadResource("com/bbn/parliament/jena/query/index/mock/mockdata.ttl", getGraph());

		String algebra = ""
			+ "(project (?x ?y)\n"
			+ "  (filter (<= ?y 5)\n"
			+ "    (bgp\n"
			+ "      (triple ?x <http://example.org/mock#obj> ?y)\n"
			//+ "      (triple <http://example.org/data/x> <http://example.org/data/p> <http://example.org/data/v1> )\n"
			+ "      (triple ?x <http://example.org/data/p> <http://example.org/data/v1> )\n"
			+ "    )\n"
			+ "  )\n"
			+ ")";

		Op op = SSE.parseOp(algebra);
		op = optimizer.rewrite(op);

		//System.out.println(OpAsQuery.asQuery(op));
		IndexPatternQuerierManager.getInstance().register(index, querier);
		QueryIterator it = opExecutor.executeOp(op, createInput());
		assertTrue(it.hasNext());
		assertTrue(index.isRangeIteratorCalled());
		while (it.hasNext()) {
			Binding b = it.next();
			Node x = b.get(Var.alloc("x"));
			assertTrue(x.isURI());
			assertEquals("http://example.org/node", x.getURI());
		}
	}
}
