package com.bbn.parliament.jena.query.index;

import java.util.Iterator;

import com.bbn.parliament.jena.graph.index.RangeIndex;
import com.bbn.parliament.jena.graph.index.Record;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.expr.E_GreaterThan;
import com.hp.hpl.jena.sparql.expr.E_GreaterThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_LessThan;
import com.hp.hpl.jena.sparql.expr.E_LessThanOrEqual;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.util.IterLib;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * An iterator for querying over a {@link RangeIndex}. The
 * <code>RangeIndexQueryIterator</code> is used to query an index for a given range. For
 * instance, the SPARQL FILTER expression:
 * <blockquote><code>FILTER (?x &lt; 5)</code></blockquote>
 * could be used to search an index for all items less than 5. The index is queried for
 * each binding in the input iterator.
 * <br><br>
 * The iterator handles the following expressions:
 * <ul>
 * <li><code>com.hp.hpl.jena.sparql.expr.E_GreaterThan</code></li>
 * <li><code>com.hp.hpl.jena.sparql.expr.E_GreaterThanOrEqual</code></li>
 * <li><code>com.hp.hpl.jena.sparql.expr.E_LessThan</code></li>
 * <li><code>com.hp.hpl.jena.sparql.expr.E_LessThanOrEqual</code></li>
 * </ul>
 *
 * @author rbattle
 * @param <T> The type of object that is indexed.
 * @see com.bbn.parliament.jena.query.SolverUtil#solve(com.hp.hpl.jena.sparql.expr.ExprList,
 *      com.hp.hpl.jena.sparql.core.BasicPattern, QueryIterator, ExecutionContext)
 */
public class RangeIndexQueryIterator<T extends Comparable<T>> extends QueryIterRepeatApply {
	ExprFunction2 expr;
	private RangeIndex<T> index;
	boolean varFirst;
	Var resourceVar;

	/**
	 * Creates a new instance.
	 *
	 * @param expr the expression to evaluate.
	 * @param resourceVar a SPARQL variable containing the indexed resource. This variable
	 *        must be in the expression.
	 * @param index the index to query.
	 * @param input the input bindings.
	 * @param context the execution context.
	 */
	public RangeIndexQueryIterator(ExprFunction2 expr, Var resourceVar,
		RangeIndex<T> index, QueryIterator input,
		ExecutionContext context) {
		super(input, context);
		this.expr = expr;
		this.index = index;
		this.varFirst = expr.getArg1().isVariable();
		this.resourceVar = resourceVar;
	}

	/** {@inheritDoc} */
	@Override
	protected QueryIterator nextStage(Binding binding) {
		T start = null;
		T end = null;

		Expr constantArg = (varFirst) ? expr.getArg2() : expr.getArg1();
		Var var = (varFirst) ? expr.getArg1().asVar() : expr.getArg2().asVar();

		// already have a binding for the filter variable so need to just check it
		// against the filter
		if (binding.contains(var)) {
			if (expr.isSatisfied(binding, getExecContext())) {
				return IterLib.result(binding, getExecContext());
			}
			return IterLib.noResults(getExecContext());

		}

		@SuppressWarnings("unchecked")
		T value = (T) constantArg.getConstant().getNode().getLiteralValue();
		if (expr instanceof E_LessThan || expr instanceof E_LessThanOrEqual) {
			if (varFirst) {
				end = value;
			} else {
				start = value;
			}
		} else if (expr instanceof E_GreaterThan || expr instanceof E_GreaterThanOrEqual) {
			if (varFirst) {
				start = value;
			} else {
				end = value;
			}
		}

		// check to see if resource is already bound. If it is, we don't need to
		// do a range query
		if (binding.contains(resourceVar)) {
			Node resource = binding.get(resourceVar);
			Record<T> record = index.find(resource);
			if (null == record) {
				return IterLib.noResults(getExecContext());
			}

			boolean valid = true;
			T v = record.getValue();
			if (null != start) {
				valid = (start.compareTo(v) <= 0);
			}
			if (null != end) {
				valid = (v.compareTo(end) <= 0);
			}

			if (valid) {
				BindingMap b = BindingFactory.create(binding);
				b.add(var, ResourceFactory.createTypedLiteral(record.getValue())
					.asNode());
				return IterLib.result(b, getExecContext());
			}
			return IterLib.noResults(getExecContext());
		}
		Iterator<Record<T>> records = index.iterator(start, end);
		return new RecordIterator(records, binding, getExecContext());
	}

	private class RecordIterator extends QueryIter {
		private Iterator<Record<T>> iterator;
		private Binding binding;

		public RecordIterator(Iterator<Record<T>> records, Binding binding,
			ExecutionContext execCxt) {
			super(execCxt);
			this.iterator = records;
			this.binding = binding;
		}

		@Override
		protected boolean hasNextBinding() {
			return iterator.hasNext();
		}

		@Override
		protected Binding moveToNextBinding() {
			Record<T> record = iterator.next();
			BindingMap eb = BindingFactory.create(binding);
			Var var = varFirst ? expr.getArg1().asVar() : expr.getArg2().asVar();
			eb.add(resourceVar, record.getKey());
			eb.add(var, ResourceFactory.createTypedLiteral(record.getValue())
				.asNode());
			return eb;
		}

		@Override
		protected void closeIterator() {
			NiceIterator.close(iterator);
		}

		@Override
		protected void requestCancel() {
			// TODO can we cancel?
		}
	}
}
