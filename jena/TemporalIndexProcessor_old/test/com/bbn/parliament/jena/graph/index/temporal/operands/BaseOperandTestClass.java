// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.TemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.temporal.bdb.PersistentTemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.index.TestIndexFactory;
import com.bbn.parliament.jena.query.KbOpExecutor;
import com.bbn.parliament.jena.util.FileUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;

import junit.framework.TestCase;

public abstract class BaseOperandTestClass extends TestCase {
	protected TemporalIndex index;
	protected TemporalPropertyFunctionFactory<PersistentTemporalIndex> pfFactory;
	protected TemporalPropertyFunction<PersistentTemporalIndex> pf;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		index = TestIndexFactory.createPopulatedTestIndex();
		pfFactory = TestIndexFactory.createPropertyFunctionFactory();
		setUpOperator();
	}

	@Override
	protected void tearDown() throws Exception {
		index.close();
		index.delete();
		index = null;
		FileUtil.delete(new File("indexes"));
		super.tearDown();
	}

	protected TemporalPropertyFunction<PersistentTemporalIndex> getOperator() {
		return pf;
	}

	public abstract Operand getOperand();

	private void setUpOperator() {
		Operand op = getOperand();
		pf = pfFactory.create(op);
		ExecutionContext context = new ExecutionContext(ARQ.getContext(), index.getGraph(),
			DatasetGraphFactory.create(index.getGraph()), KbOpExecutor.KbOpExecutorFactory);

		PropFuncArg argSubject = new PropFuncArg(Node.createVariable("s"));
		PropFuncArg argObject = new PropFuncArg(Node.createVariable("o"));
		Node predicate = Node.createURI(op.getUri());
		pf.build(argSubject, predicate, argObject, context);
	}

	protected void compareExtentIteratorToExpected(Iterator<Record<TemporalExtent>> it,
		Set<String> answerKey) {
		Set<String> results = new TreeSet<>();

		while (it.hasNext()) {
			Record<TemporalExtent> nodeExtent = it.next();
			Node n = nodeExtent.getKey();
			assertNotNull("Resulting variable Node was null", n);
			results.add(n.toString());
		}
		assertEquals("Incorrect number of results", answerKey.size(), results.size());
		for (String s : answerKey) {
			assertTrue("Expected result (" + s + ") not found in actual results", results.contains(s));
		}
	}
}
