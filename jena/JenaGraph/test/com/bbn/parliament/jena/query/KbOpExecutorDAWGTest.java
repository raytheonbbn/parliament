package com.bbn.parliament.jena.query;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable;
import com.hp.hpl.jena.sparql.util.Context;

@RunWith(Parameterized.class)
public class KbOpExecutorDAWGTest extends AbstractDAWGTestCase {

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

	@Override
	@After
	public void tearDown() {
		super.tearDown();
	}

	@Parameters
	public static Collection<Object[]> generateData() {
		return AbstractDAWGTestCase.generateData();
	}

	public KbOpExecutorDAWGTest(File testDir, String name, File result, File query,
		List<File> data, List<File> graphData) {
		super(testDir, name, result, query, data, graphData);
	}

	@Override
	protected void runDAWGTest(Query query, ResultSet resultSet) {
		ResultSetRewindable expected = ResultSetFactory.makeRewindable(resultSet);
		Op op = Algebra.compile(query);
		QueryIterator input = QueryIterRoot.create(execCxt);
		QueryIterator it = opExecutor.executeOp(op, input);
		ResultSetRewindable actual = ResultSetFactory
			.makeRewindable(ResultSetFactory.create(it, query.getResultVars()));
		assertTrue(String.format("'%1$s': result sets are not equal", getCurrentTest()),
			QueryTestUtil.equals(expected, actual, query));
	}

	@Override
	protected void runDAWGTest(Query query, boolean answer) {
		Op op = Algebra.compile(query);
		QueryIterator input = QueryIterRoot.create(execCxt);
		QueryIterator it = opExecutor.executeOp(op, input);
		assertTrue(String.format("'%1$s': result sets are not equal", getCurrentTest()),
			answer == it.hasNext());
	}
}
