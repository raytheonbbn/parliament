// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.operands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;
import com.bbn.parliament.jena.query.KbOpExecutor;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;

public abstract class BaseOperandTestClass {
	protected TemporalIndex index;
	protected TemporalPropertyFunctionFactory<PersistentTemporalIndex> pfFactory;
	protected TemporalPropertyFunction<PersistentTemporalIndex> pf;

	// Call from @BeforeEach
	protected void beforeEach() {
		index = TestIndexFactory.createPopulatedTestIndex();
		pfFactory = TestIndexFactory.createPropertyFunctionFactory();

		Operand op = getOperand();
		pf = pfFactory.create(op);
		ExecutionContext context = new ExecutionContext(ARQ.getContext(), index.getGraph(),
			DatasetGraphFactory.create(index.getGraph()), KbOpExecutor.KbOpExecutorFactory);

		PropFuncArg argSubject = new PropFuncArg(Node.createVariable("s"));
		PropFuncArg argObject = new PropFuncArg(Node.createVariable("o"));
		Node predicate = Node.createURI(op.getUri());
		pf.build(argSubject, predicate, argObject, context);
	}

	// Call from @AfterEach
	protected void afterEach() {
		index.close();
		index = null;
	}

	public abstract Operand getOperand();

	protected TemporalPropertyFunction<PersistentTemporalIndex> getOperator() {
		return pf;
	}

	protected static void compareExtentIteratorToExpected(Iterator<Record<TemporalExtent>> it,
		Set<String> answerKey) {
		Set<String> results = new TreeSet<>();

		while (it.hasNext()) {
			Record<TemporalExtent> nodeExtent = it.next();
			Node n = nodeExtent.getKey();
			assertNotNull(n, "Resulting variable Node was null");
			results.add(n.toString());
		}
		assertEquals(answerKey.size(), results.size(), "Incorrect number of results");
		for (String s : answerKey) {
			assertTrue(results.contains(s), "Expected result (" + s + ") not found in actual results");
		}
	}
}
