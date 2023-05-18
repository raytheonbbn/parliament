// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal.memory;

import java.util.HashMap;
import java.util.Map;

import com.bbn.parliament.kb_graph.index.temporal.ExtentTester;
import com.bbn.parliament.kb_graph.index.temporal.Operand;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunction;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalAfter;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalBefore;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalContains;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalDuring;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalEquals;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalFinishedBy;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalFinishes;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalFinishesAfter;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalMeets;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalMetBy;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalOverlappedBy;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalOverlaps;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalStartedBy;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalStarts;
import com.bbn.parliament.kb_graph.index.temporal.operands.IntervalStartsBefore;

/** @author dkolas */
public class MemoryPropertyFunctionFactory extends TemporalPropertyFunctionFactory<MemoryTemporalIndex> {

	private Map<Operand, TemporalPropertyFunction<MemoryTemporalIndex>> implementations;

	public MemoryPropertyFunctionFactory(){
		implementations = new HashMap<>();

		implementations.put(Operand.INTERVAL_OVERLAPS, new IntervalOverlaps(ExtentTester.INTERVAL_OVERLAPS));
		implementations.put(Operand.INTERVAL_BEFORE, new IntervalBefore(ExtentTester.INTERVAL_BEFORE));
		implementations.put(Operand.INTERVAL_AFTER, new IntervalAfter(ExtentTester.INTERVAL_AFTER));
		implementations.put(Operand.INTERVAL_STARTS_BEFORE, new IntervalStartsBefore(ExtentTester.INTERVAL_STARTS_BEFORE));
		implementations.put(Operand.INTERVAL_FINISHES_AFTER, new IntervalFinishesAfter(ExtentTester.INTERVAL_FINISHES_AFTER));
		implementations.put(Operand.INTERVAL_EQUALS, new IntervalEquals(ExtentTester.EQUALS));
		implementations.put(Operand.INTERVAL_MEETS, new IntervalMeets(ExtentTester.INTERVAL_MEETS));
		implementations.put(Operand.INTERVAL_STARTS, new IntervalStarts(ExtentTester.INTERVAL_STARTS));
		implementations.put(Operand.INTERVAL_DURING, new IntervalDuring(ExtentTester.INTERVAL_DURING));
		implementations.put(Operand.INTERVAL_FINISHES, new IntervalFinishes(ExtentTester.INTERVAL_FINISHES));
		implementations.put(Operand.INTERVAL_MET_BY, new IntervalMetBy(ExtentTester.INTERVAL_MET_BY));
		implementations.put(Operand.INTERVAL_OVERLAPPED_BY, new IntervalOverlappedBy(ExtentTester.INTERVAL_OVERLAPPED_BY));
		implementations.put(Operand.INTERVAL_STARTED_BY, new IntervalStartedBy(ExtentTester.INTERVAL_STARTED_BY));
		implementations.put(Operand.INTERVAL_CONTAINS, new IntervalContains(ExtentTester.INTERVAL_CONTAINS));
		implementations.put(Operand.INTERVAL_FINISHED_BY, new IntervalFinishedBy(ExtentTester.INTERVAL_FINISHED_BY));
	}

	/**{@inheritDoc} */
	@Override
	public TemporalPropertyFunction<MemoryTemporalIndex> create(String uri) {
		Operand op = Operand.valueOfUri(uri);
		return implementations.get(op);
	}
}
