// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.bdb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.ExtentTester;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;

/** @author dkolas */
public class PersistentPropertyFunctionFactory extends TemporalPropertyFunctionFactory<PersistentTemporalIndex> {

	private Map<Operand, TemporalPropertyFunction<PersistentTemporalIndex>> impls;

	public PersistentPropertyFunctionFactory() {
		impls = new HashMap<>();

		impls.put(Operand.INTERVAL_OVERLAPS, new OverlapsOperator(Operand.INTERVAL_OVERLAPS));
		impls.put(Operand.INTERVAL_BEFORE, new BeforeOperator(Operand.INTERVAL_BEFORE));
		impls.put(Operand.INTERVAL_AFTER, new AfterOperator(Operand.INTERVAL_AFTER));
		impls.put(Operand.INTERVAL_STARTS_BEFORE, new StartsBeforeOperator(Operand.INTERVAL_STARTS_BEFORE));
		impls.put(Operand.INTERVAL_FINISHES_AFTER, new FinishesAfterOperator(Operand.INTERVAL_FINISHES_AFTER));
		impls.put(Operand.INTERVAL_EQUALS, new EqualsOperator(Operand.INTERVAL_EQUALS));
		impls.put(Operand.INTERVAL_MEETS, new MeetsOperator(Operand.INTERVAL_MEETS));
		impls.put(Operand.INTERVAL_STARTS, new StartsOperator(Operand.INTERVAL_STARTS));
		impls.put(Operand.INTERVAL_DURING, new DuringOperator(Operand.INTERVAL_DURING));
		impls.put(Operand.INTERVAL_FINISHES, new FinishesOperator(Operand.INTERVAL_FINISHES));
		impls.put(Operand.INTERVAL_MET_BY, new MetByOperator(Operand.INTERVAL_MET_BY));
		impls.put(Operand.INTERVAL_OVERLAPPED_BY, new OverlappedByOperator(Operand.INTERVAL_OVERLAPPED_BY));
		impls.put(Operand.INTERVAL_STARTED_BY, new StartedByOperator(Operand.INTERVAL_STARTED_BY));
		impls.put(Operand.INTERVAL_CONTAINS, new ContainsOperator(Operand.INTERVAL_CONTAINS));
		impls.put(Operand.INTERVAL_FINISHED_BY, new FinishedByOperator(Operand.INTERVAL_FINISHED_BY));
	}

	/** {@inheritDoc} */
	@Override
	public TemporalPropertyFunction<PersistentTemporalIndex> create(String uri) {
		Operand op = Operand.valueOfUri(uri);
		return impls.get(op);
	}

	private static abstract class PersistentTemporalFunction extends TemporalPropertyFunction<PersistentTemporalIndex> {
		public PersistentTemporalFunction(ExtentTester e) {
			super(PersistentTemporalIndex.class, e);
		}
	}

	private static class OverlapsOperator extends PersistentTemporalFunction {
		public OverlapsOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(
			TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}
	}

	private static class OverlappedByOperator extends PersistentTemporalFunction {
		public OverlappedByOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class BeforeOperator extends PersistentTemporalFunction {
		public BeforeOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, start, 0L, start);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(0L, start, 0L, start);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return new PersistentTemporalExtentIterator(getIndex(),
				end, Long.MAX_VALUE, end, Long.MAX_VALUE);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, Long.MAX_VALUE, end, Long.MAX_VALUE);
		}
	}

	private static class AfterOperator extends PersistentTemporalFunction {
		public AfterOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return new PersistentTemporalExtentIterator(getIndex(),
				end, Long.MAX_VALUE, end, Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, Long.MAX_VALUE, end, Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, start, 0L, start);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(0L, start, 0L, start);
		}
	}

	private static class StartsBeforeOperator extends PersistentTemporalFunction {
		public StartsBeforeOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				0L, Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				0L, Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				0L, Long.MAX_VALUE);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				0L, Long.MAX_VALUE);
		}
	}

	private static class FinishesAfterOperator extends PersistentTemporalFunction {
		public FinishesAfterOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, Long.MAX_VALUE,
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, Long.MAX_VALUE,
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, Long.MAX_VALUE,
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, Long.MAX_VALUE,
				0L, boundExtent.getEnd().getInstant());
		}
	}

	private static class EqualsOperator extends PersistentTemporalFunction {
		public EqualsOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class MeetsOperator extends PersistentTemporalFunction {
		public MeetsOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, Long.MAX_VALUE,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, Long.MAX_VALUE,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				0L, Long.MAX_VALUE);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				0L, Long.MAX_VALUE);
		}
	}

	private static class MetByOperator extends PersistentTemporalFunction {
		public MetByOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				0L, Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				0L, Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, Long.MAX_VALUE,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, Long.MAX_VALUE,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant());
		}
	}

	private static class StartsOperator extends PersistentTemporalFunction {
		public StartsOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}
	}

	private static class StartedByOperator extends PersistentTemporalFunction {
		public StartedByOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				0L, boundExtent.getEnd().getInstant());
		}
	}

	private static class DuringOperator extends PersistentTemporalFunction {
		public DuringOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}
	}

	private static class ContainsOperator extends PersistentTemporalFunction {
		public ContainsOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), Long.MAX_VALUE);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				0L, boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				0L, boundExtent.getEnd().getInstant());
		}
	}

	private static class FinishesOperator extends PersistentTemporalFunction {
		public FinishesOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class FinishedByOperator extends PersistentTemporalFunction {
		public FinishedByOperator(Operand operand) {
			super(operand.getExtentTester());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				0L, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
			return new PersistentTemporalExtentIterator(getIndex(),
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), Long.MAX_VALUE,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}
	}
}
