// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.TemporalExtentIterator.InclusionDecider;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;
import com.bbn.parliament.jena.query.index.QueryCache;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.pfunction.EstimableIndexPropertyFunction;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterCommonParent;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.util.IterLib;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/** @author dkolas */
public abstract class TemporalPropertyFunction<I extends TemporalIndex>
	extends EstimableIndexPropertyFunction<TemporalExtent> {

	private static final boolean EVALUATE_DOUBLY_UNBOUND_PROP_FUNCTIONS = true;

	private static Logger log = LoggerFactory.getLogger(TemporalPropertyFunction.class);

	private ExtentTester extentTester;

	public TemporalPropertyFunction(Class<I> indexClass,
		ExtentTester extentTester) {
		super(indexClass, TemporalOperandFactory.class);
		this.extentTester = extentTester;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected I getIndex() {
		return (I)index;
	}

	/** {@inheritDoc} */
	@Override
	public void build(PropFuncArg argSubject, Node predicate,
		PropFuncArg argObject, ExecutionContext context) {
		if (argSubject.isList() || argObject.isList()) {
			throw new RuntimeException("Predicate {0} does not support lists."
				.formatted(predicate.getURI()));
		}
		super.build(argSubject, predicate, argObject, context);
	}

	/** {@inheritDoc} */
	@Override
	public long estimate(List<Node> subjects, List<Node> objects,
		Map<Node, Operand<TemporalExtent>> operands) {

		Node subject = subjects.get(0);
		Node object = objects.get(0);

		TemporalExtent extent1 = operands.get(subject).getRepresentation();
		TemporalExtent extent2 = operands.get(object).getRepresentation();

		try {
			if (null != extent1 && null != extent2) {
				return 1;	// No need to check. This predicate will return at most one result
			} else if (null != extent1) {
				return estimateSecondVar(extent1);
			} else if (null != extent2) {
				return estimateFirstVar(extent2);
			} else {
				long indexSize = index.size();
				return indexSize * indexSize;	// both extents are null
			}
		} catch (IndexException ex) {
			log.warn("Exception while retrieving the size of the temporal index", ex);
			return 0;
		}
	}

	/** {@inheritDoc} */
	@Override
	public QueryIterator execBinding(Binding binding, List<Node> subjects,
		List<Node> objects, Map<Node, Operand<TemporalExtent>> operands,
		ExecutionContext context) {

		if (log.isTraceEnabled()) {
			log.trace("execBinding:  {} subjects:", subjects.size());
			for (Node n : subjects) {
				log.trace("   execBinding:  subject = '{}'", n);
			}
			log.trace("execBinding:  {} objects:", objects.size());
			for (Node n : objects) {
				log.trace("   execBinding:  object = '{}'", n);
			}
			log.trace("execBinding:  {} operand mappings:", operands.size());
			for (Map.Entry<Node, Operand<TemporalExtent>> e : operands.entrySet()) {
				log.trace("   execBinding:  operand mapping '{}' --> '{}'", e.getKey(), e.getValue());
			}

			Iterator<Record<TemporalExtent>> it = index.iterator();
			if (it.hasNext()) {
				log.trace("Temporal Index Contents:");
				while (it.hasNext()) {
					Record<TemporalExtent> rec = it.next();
					log.trace("   Record:  {}", rec);
				}
			}
		}

		Node node1 = subjects.get(0);
		Node node2 = objects.get(0);

		TemporalExtent extent1 = getExtentForNode(operands, node1);
		TemporalExtent extent2 = getExtentForNode(operands, node2);

		if (extent1 == null || extent2 == null) {
			QueryIterator result = performOperation(extent1, extent2, node1, node2,
				index.getQueryCache(), context);
			return new QueryIterCommonParent(result, binding, context);
		} else if (testExtents(extent1, extent2)) {
			BindingMap newResultBinding = BindingFactory.create(binding);
			addToBinding(newResultBinding, node1, extent1);
			addToBinding(newResultBinding, node2, extent2);
			return IterLib.result(newResultBinding, context);
		} else {
			return IterLib.noResults(context);
		}
	}

	private static TemporalExtent getExtentForNode(Map<Node, Operand<TemporalExtent>> operands, Node node) {
		Operand<TemporalExtent> operand = operands.get(node);
		return (operand == null) ? null : operand.getRepresentation();
	}

	private void addToBinding(BindingMap binding, Node node, TemporalExtent extent) {
		if (node.isVariable()) {
			Var var = Var.alloc(node);
			if (!binding.contains(var)) {
				binding.add(var, node);
			}
			index.getQueryCache().put(node, extent);
		}
	}

	public boolean testExtents(TemporalExtent extent1, TemporalExtent extent2) {
		return extentTester.testExtents(extent1, extent2);
	}

	public QueryIterator performOperation(TemporalExtent extent1,
		TemporalExtent extent2, Node rootNode1, Node rootNode2,
		QueryCache<TemporalExtent> queryCache, ExecutionContext context) {
		log.trace("Performing {} on variables {} and {}",
			new Object[] { getClass().getSimpleName(), rootNode1, rootNode2 });
		if (extent1 != null) {
			log.trace("extent1 is non-null");
			return new MapIterator(rootNode2, bindSecondVar(extent1), queryCache, context);
		} else if (extent2 != null) {
			log.trace("extent2 is non-null");
			return new MapIterator(rootNode1, bindFirstVar(extent2), queryCache, context);
		} else {
			log.trace("Both extents are null");
			if (EVALUATE_DOUBLY_UNBOUND_PROP_FUNCTIONS) {
				return new DoubleMapIterator<>(rootNode1, rootNode2, this, queryCache, context);
			} else {
				throw new RuntimeException("Queries with temporal binary predicates that are unbound in both variables are not yet supported.");
			}
		}
	}

	public abstract Iterator<Record<TemporalExtent>> bindFirstVar(
		TemporalExtent boundExtent);

	public abstract Iterator<Record<TemporalExtent>> bindSecondVar(
		TemporalExtent boundExtent);

	public abstract long estimateSecondVar(TemporalExtent extentToCheck);

	public abstract long estimateFirstVar(TemporalExtent extentToCheck);

	private static class MapIterator extends QueryIter {
		private final Var var;
		private final Iterator<Record<TemporalExtent>> internal;
		private final QueryCache<TemporalExtent> queryCache;

		public MapIterator(Node varNode, Iterator<Record<TemporalExtent>> internal,
			QueryCache<TemporalExtent> queryCache, ExecutionContext context) {
			super(context);
			this.var = Var.alloc(varNode);
			this.internal = internal;
			this.queryCache = queryCache;
		}

		@Override
		protected boolean hasNextBinding() {
			return internal.hasNext();
		}

		@Override
		protected Binding moveToNextBinding() {
			BindingMap binding = BindingFactory.create();
			Record<TemporalExtent> r = internal.next();
			binding.add(var, r.getKey());
			queryCache.put(r.getKey(), r.getValue());
			return binding;
		}

		@Override
		protected void closeIterator() {
			NiceIterator.close(internal);
		}

		@Override
		protected void requestCancel() {
			// TODO can we cancel?
		}
	}

	private static class DoubleMapIterator<I extends TemporalIndex> extends QueryIter {
		private final Var var1;
		private final Var var2;
		private final TemporalPropertyFunction<I> propFxn;
		private final Iterator<Record<TemporalExtent>> ext1Iter;
		private Record<TemporalExtent> ext1Record;
		private Iterator<Record<TemporalExtent>> ext2Iter;
		private final QueryCache<TemporalExtent> queryCache;

		public DoubleMapIterator(Node varNode1, Node varNode2,
			TemporalPropertyFunction<I> propFxn, QueryCache<TemporalExtent> queryCache,
			ExecutionContext context) {
			super(context);
			var1 = Var.alloc(varNode1);
			var2 = Var.alloc(varNode2);
			log.trace("Creating DoubleMapIterator with var1 = {}, var2 = {}", var1, var2);
			this.propFxn = propFxn;
			ext1Iter = propFxn.getIndex().iterator();
			ext1Record = null;
			ext2Iter = null;
			if (ext1Iter.hasNext()) {
				ext1Record = ext1Iter.next();
				ext2Iter = propFxn.bindSecondVar(ext1Record.getValue());
				log.trace("ext1Iter is not empty");
			}
			this.queryCache = queryCache;
		}

		@Override
		protected boolean hasNextBinding() {
			if (ext2Iter == null) {
				return false;	// Indicates that ext1Iter was empty during construction above
			} else {
				while (!ext2Iter.hasNext()) {
					if (ext1Iter.hasNext()) {
						ext1Record = ext1Iter.next();
						NiceIterator.close(ext2Iter);
						ext2Iter = propFxn.bindSecondVar(ext1Record.getValue());
						log.trace("moving to next ext1Iter record:  '{}', '{}'",
							ext1Record.getKey(), ext1Record.getValue());
					} else {
						log.trace("ext1Iter has no more bindings");
						return false;
					}
				}
				return ext2Iter.hasNext();
			}
		}

		@Override
		protected Binding moveToNextBinding() {
			BindingMap binding = BindingFactory.create();
			Record<TemporalExtent> ext2Record = ext2Iter.next();
			binding.add(var1, ext1Record.getKey());
			binding.add(var2, ext2Record.getKey());
			queryCache.put(ext1Record.getKey(), ext1Record.getValue());
			queryCache.put(ext2Record.getKey(), ext2Record.getValue());
			log.trace("returned binding for var1:  '{}', '{}', '{}'",
				new Object[] { var1, ext1Record.getKey(), ext1Record.getValue() });
			log.trace("returned binding for var2:  '{}', '{}', '{}'",
				new Object[] { var2, ext2Record.getKey(), ext2Record.getValue() });
			return binding;
		}

		@Override
		protected void closeIterator() {
			NiceIterator.close(ext1Iter);
			NiceIterator.close(ext2Iter);
		}

		@Override
		protected void requestCancel() {
			// TODO can we cancel?
		}
	}

	// **************************
	// *** INCLUSION DECIDERS ***
	// **************************

	protected static class StartsBeforeInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public StartsBeforeInclusionDecider(TemporalExtent extent) {
			_boundExtent = extent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null) {
				if (instant.isStart() && instant.lessThan(_boundExtent.getStart())) {
					return interval;
				}
			} else if (instant.lessThan(_boundExtent.getStart())) {
				return instant;
			}
			return null;
		}
	}

	protected static class AlwaysIncludeStartsInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public AlwaysIncludeStartsInclusionDecider(TemporalExtent extent) {
			_boundExtent = extent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null) {
				if (instant.isStart()) {
					return interval;
				}
			} else {
				return instant;
			}
			return null;
		}
	}

	protected static class AlwaysIncludeEndsInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public AlwaysIncludeEndsInclusionDecider(TemporalExtent extent) {
			_boundExtent = extent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null) {
				if (instant.isStart()) {
					return interval;
				}
			} else {
				return instant;
			}
			return null;
		}
	}

	protected static class EndsBeforeInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public EndsBeforeInclusionDecider(TemporalExtent extent) {
			_boundExtent = extent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null) {
				if (instant.isEnd() && instant.lessThan(_boundExtent.getStart())) {
					return interval;
				}
			} else if (instant.lessThan(_boundExtent.getStart())) {
				return instant;
			}
			return null;
		}
	}

	protected static class EndsAfterInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public EndsAfterInclusionDecider(TemporalExtent extent) {
			_boundExtent = extent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null) {
				if (instant.isEnd() && instant.greaterThan(_boundExtent.getEnd())) {
					return interval;
				}
			} else if (instant.greaterThan(_boundExtent.getEnd())) {
				return instant;
			}
			return null;
		}
	}

	protected static class StartsAfterInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public StartsAfterInclusionDecider(TemporalExtent extent) {
			_boundExtent = extent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null) {
				if (instant.isStart() && instant.greaterThan(_boundExtent.getEnd())) {
					return interval;
				}
			} else if (instant.greaterThan(_boundExtent.getEnd())) {
				return instant;
			}
			return null;
		}
	}

	protected static class FinishesAfterEndInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public FinishesAfterEndInclusionDecider(TemporalExtent boundExtent) {
			_boundExtent = boundExtent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			// I want to make sure that this instant is the beginning of an
			// interval and that it ends after _boundExtent ends
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null && instant.isStart()) {
				return (interval.getEnd().greaterThan(_boundExtent.getEnd())) ? interval : null;
			}
			return null;
		}
	}

	protected static class FinishesBeforeEndInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public FinishesBeforeEndInclusionDecider(TemporalExtent boundExtent) {
			_boundExtent = boundExtent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			// I want to make sure that this instant is the beginning of an interval
			// and that it ends before _boundExtent ends
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null && instant.isStart()) {
				return (interval.getEnd().lessThan(_boundExtent.getEnd())) ? interval : null;
			}
			return null;
		}
	}

	protected static class FinishEqualsStartInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public FinishEqualsStartInclusionDecider(TemporalExtent boundExtent) {
			_boundExtent = boundExtent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			// I want to make sure that this instant is the end of an interval
			// and that it ends exactly when _boundExtent begins
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null && instant.isEnd()) {
				return (interval.getEnd().sameAs(_boundExtent.getStart())) ? interval : null;
			}
			return null;
		}
	}

	protected static class StartEqualsEndInclusionDecider implements InclusionDecider {
		TemporalExtent _boundExtent;

		public StartEqualsEndInclusionDecider(TemporalExtent boundExtent) {
			_boundExtent = boundExtent;
		}

		@Override
		public TemporalExtent test(TemporalInstant instant) {
			// I want to make sure that this instant is the start of an interval
			// and that it starts exactly when _boundExtent ends
			TemporalInterval interval = instant.getParentInterval();
			if (interval != null && instant.isStart()) {
				return (interval.getStart().sameAs(_boundExtent.getEnd())) ? interval : null;
			}
			return null;
		}
	}
}
