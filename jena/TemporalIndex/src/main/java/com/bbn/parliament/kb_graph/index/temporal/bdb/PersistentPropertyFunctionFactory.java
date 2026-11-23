// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.temporal.bdb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.temporal.ExtentTester;
import com.bbn.parliament.kb_graph.index.temporal.Operand;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunction;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInterval;

/** @author dkolas */
public class PersistentPropertyFunctionFactory extends TemporalPropertyFunctionFactory<PersistentTemporalIndex> {

	private Map<Operand, TemporalPropertyFunction<PersistentTemporalIndex>> impls;

	public PersistentPropertyFunctionFactory() {
		impls = new HashMap<>();

		impls.put(Operand.INTERVAL_OVERLAPS, new OverlapsOperator(Operand.INTERVAL_OVERLAPS));
		impls.put(Operand.INTERVAL_BEFORE, new IntervalBeforeOperator(Operand.INTERVAL_BEFORE));
		impls.put(Operand.INTERVAL_AFTER, new IntervalAfterOperator(Operand.INTERVAL_AFTER));
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
		impls.put(Operand.AFTER, new AfterOperator(Operand.AFTER));
		impls.put(Operand.BEFORE, new BeforeOperator(Operand.BEFORE));
		impls.put(Operand.HAS_BEGINNING, new HasBeginningOperator(Operand.HAS_BEGINNING));
		impls.put(Operand.HAS_END, new HasEndOperator(Operand.HAS_END));
		impls.put(Operand.INSIDE, new InsideOperator(Operand.INSIDE));
		impls.put(Operand.INSTANT_EQUALS, new InstEqualsOperator(Operand.INSTANT_EQUALS));
	}

	/** {@inheritDoc} */
	@Override
	public TemporalPropertyFunction<PersistentTemporalIndex> create(String uri) {
		Operand op = Operand.valueOfUri(uri);
		return impls.get(op);
	}

	/**
	 * Abstract superclass for all static operator classes. The domain and range fields are used to represent
	 * the domain/range type constraints implemented in each predicate function subclass. Whenever a binding
	 * operation must be performed, this class first checks that the bound extent is a valid domain/range,
	 * and then filters the binding solutions depending on whether the variable is of the function's domain
	 * or range.
	 *
	 * @author mhale
	 */
	private static abstract class PersistentTemporalFunction extends TemporalPropertyFunction<PersistentTemporalIndex> {

		protected static final long DEFINED_MIN = Long.MIN_VALUE + 1;
		protected static final long DEFINED_MAX = Long.MAX_VALUE - 1;

		protected final Class<? extends TemporalExtent> domain;
		protected final Class<? extends TemporalExtent> range;


		public PersistentTemporalFunction(ExtentTester e, Class<? extends TemporalExtent> domain,
			Class<? extends TemporalExtent> range) {
			super(PersistentTemporalIndex.class, e);
			this.domain = domain;
			this.range = range;
		}

		protected abstract long[] getFirstVarConstraints(TemporalExtent boundExtent);
		protected abstract long[] getSecondVarConstraints(TemporalExtent boundExtent);

		@Override
		public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent)	{
			if (!range.isInstance(boundExtent))	{
				return Collections.emptyIterator();
			}
			long[] constraints = getFirstVarConstraints(boundExtent);
			if (constraints.length != 4)	{
				throw new IllegalArgumentException(
					"Exactly 4 longs are expected -- received %1$s."
					.formatted(constraints.length));
			}
			return new PersistentTemporalExtentIterator(getIndex(),
				constraints[0], constraints[1], constraints[2], constraints[3], domain);
		}

		@Override
		public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent)	{
			if (!domain.isInstance(boundExtent))	{return Collections.emptyIterator();}
			long[] constraints = getSecondVarConstraints(boundExtent);
			if (constraints.length != 4)	{
				throw new IllegalArgumentException(
					"Exactly 4 longs are expected -- received %1$s."
					.formatted(constraints.length));
			}
			return new PersistentTemporalExtentIterator(getIndex(),
				constraints[0], constraints[1], constraints[2], constraints[3], range);
		}
	}

	private static final class OverlapsOperator extends PersistentTemporalFunction {

		public OverlapsOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			long end = boundExtent.getEnd().getInstant();
			return new long[]{DEFINED_MIN, start, start, end};
			//			return new long[]{
			//					DEFINED_MIN, boundExtent.getStart().getInstant(),
			//					boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}
	}

	private static class OverlappedByOperator extends PersistentTemporalFunction {
		public OverlappedByOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getStart().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class IntervalBeforeOperator extends PersistentTemporalFunction {
		public IntervalBeforeOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new long[]{
				DEFINED_MIN, start, DEFINED_MIN, start};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(DEFINED_MIN, start, DEFINED_MIN, start);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {

			long end = boundExtent.getEnd().getInstant();
			return new long[]{
				end, DEFINED_MAX, end, DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, DEFINED_MAX, end, DEFINED_MAX);
		}
	}

	private static class IntervalAfterOperator extends PersistentTemporalFunction {
		public IntervalAfterOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return new long[]{
				end, DEFINED_MAX, end, DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, DEFINED_MAX, end, DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new long[]{
				DEFINED_MIN, start, DEFINED_MIN, start};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(DEFINED_MIN, start, DEFINED_MIN, start);
		}
	}

	private static class StartsBeforeOperator extends PersistentTemporalFunction {
		public StartsBeforeOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				DEFINED_MIN, DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				DEFINED_MIN, DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, DEFINED_MAX);
		}
	}

	private static class FinishesAfterOperator extends PersistentTemporalFunction {
		public FinishesAfterOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, DEFINED_MAX,
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, DEFINED_MAX,
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant());
		}
	}

	private static class EqualsOperator extends PersistentTemporalFunction {
		public EqualsOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
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
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, DEFINED_MAX,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, DEFINED_MAX,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				DEFINED_MIN, DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				DEFINED_MIN, DEFINED_MAX);
		}
	}

	private static class MetByOperator extends PersistentTemporalFunction {
		public MetByOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				DEFINED_MIN, DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant(),
				DEFINED_MIN, DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, DEFINED_MAX,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, DEFINED_MAX,
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant());
		}
	}

	private static class StartsOperator extends PersistentTemporalFunction {
		public StartsOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				DEFINED_MIN, boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				DEFINED_MIN, boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}
	}

	private static class StartedByOperator extends PersistentTemporalFunction {
		public StartedByOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				DEFINED_MIN, boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				DEFINED_MIN, boundExtent.getEnd().getInstant());
		}
	}

	private static class DuringOperator extends PersistentTemporalFunction {
		public DuringOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}
	}

	private static class ContainsOperator extends PersistentTemporalFunction {
		public ContainsOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant());
		}
	}

	private static class FinishesOperator extends PersistentTemporalFunction {
		public FinishesOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class FinishedByOperator extends PersistentTemporalFunction {
		public FinishedByOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInterval.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(TemporalExtent boundExtent) {
			return new long[]{
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class BeforeOperator extends PersistentTemporalFunction {
		public BeforeOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalExtent.class,	/*domain*/
				TemporalExtent.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new long[]{
				DEFINED_MIN, start, DEFINED_MIN, start};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(DEFINED_MIN, start, DEFINED_MIN, start);
		}

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return new long[]{
				end, DEFINED_MAX,
				end, DEFINED_MAX};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, DEFINED_MAX, end, DEFINED_MAX);
		}
	}

	private static class AfterOperator extends PersistentTemporalFunction {

		public AfterOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalExtent.class,	/*domain*/
				TemporalExtent.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return new long[]{
				end, DEFINED_MAX, end, DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, DEFINED_MAX, end, DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new long[]{
				DEFINED_MIN, start, DEFINED_MIN, start};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(DEFINED_MIN, start, DEFINED_MIN, start);
		}
	}

	private static class HasBeginningOperator extends PersistentTemporalFunction {
		public HasBeginningOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalExtent.class,	/*domain*/
				TemporalInstant.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {

			long start = boundExtent.getStart().getInstant();
			return new long[]{
				start, start, start, DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(start, start, start, DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return new long[]{
				start, start,
				start, start};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			return getIndex().estimate(start, start, start, start);
		}
	}

	private static class HasEndOperator extends PersistentTemporalFunction {
		public HasEndOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalExtent.class,	/*domain*/
				TemporalInstant.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {

			long end = boundExtent.getEnd().getInstant();
			return new long[]{
				DEFINED_MIN, end, end, end};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(DEFINED_MIN, end, end, end);
		}

		/** Extents that end at {@link Long#MAX_VALUE} are implied to not have an upper limit
		 *  and therefore never return any ending bindings. */

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return new long[]{
				end, end,
				end, end};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			long end = boundExtent.getEnd().getInstant();
			return getIndex().estimate(end, end, end, end);
		}
	}

	private static class InstEqualsOperator extends PersistentTemporalFunction {
		public InstEqualsOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInstant.class,	/*domain*/
				TemporalInstant.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {
			long start = boundExtent.getStart().getInstant();
			long end = boundExtent.getEnd().getInstant();
			return new long[]{start, start, end, end};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {

			return new long[]{
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), boundExtent.getEnd().getInstant());
		}
	}

	private static class InsideOperator extends PersistentTemporalFunction {
		public InsideOperator(Operand operand) {
			super(operand.getExtentTester(),
				TemporalInterval.class,	/*domain*/
				TemporalInstant.class); /*range*/
		}

		@Override
		public long[] getFirstVarConstraints(TemporalExtent boundExtent) {

			return new long[]{
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX};
		}

		@Override
		public long estimateFirstVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				DEFINED_MIN, boundExtent.getStart().getInstant(),
				boundExtent.getEnd().getInstant(), DEFINED_MAX);
		}

		@Override
		public long[] getSecondVarConstraints(
			TemporalExtent boundExtent) {

			return new long[]{
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant()};
		}

		@Override
		public long estimateSecondVar(TemporalExtent boundExtent) {
			return getIndex().estimate(
				boundExtent.getStart().getInstant(), DEFINED_MAX,
				DEFINED_MIN, boundExtent.getEnd().getInstant());
		}
	}
}
