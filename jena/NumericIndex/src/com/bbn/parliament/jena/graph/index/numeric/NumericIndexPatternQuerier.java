/**
 *
 */

package com.bbn.parliament.jena.graph.index.numeric;

import java.io.Serializable;
import java.util.Iterator;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.util.IterLib;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * Querier for {@link NumericIndex}es. The <code>NumericInexPatternQuerier</code> matches
 * triples that contain the indexed predicate and a variable object.
 *
 * @author rbattle
 */
public class NumericIndexPatternQuerier<T extends Number & Serializable & Comparable<T>>
implements IndexPatternQuerier {

	private final String predicate;
	NumericIndex<T> index;

	/**
	 * Construct a new instance.
	 *
	 * @param predicate the predicate to index
	 * @param index the index
	 */
	public NumericIndexPatternQuerier(String predicate, NumericIndex<T> index) {
		this.predicate = predicate;
		this.index = index;
	}

	/** Get the predicate. */
	public String getPredicate() {
		return predicate;
	}

	/** {@inheritDoc} */
	@Override
	public long estimate(BasicPattern pattern) {
		return pattern.size();
	}

	/** {@inheritDoc} */
	@Override
	public QueryIterator query(final BasicPattern pattern, QueryIterator input,
		ExecutionContext context) {

		QueryIterRepeatApply ret = new QueryIterRepeatApply(input, context) {

			@Override
			protected QueryIterator nextStage(final Binding binding) {
				QueryIterator qi = null;
				for (Triple t : pattern) {
					Node s = t.getSubject();
					Node o = t.getObject();

					// update subject and object
					Var v = null;
					if (s.isVariable()) {
						v = Var.alloc(s);
						if (binding.contains(v)) {
							s = binding.get(v);
						}
					}
					if (o.isVariable()) {
						v = Var.alloc(o);
						if (binding.contains(v)) {
							o = binding.get(v);
						}
					}

					if (s.isVariable() && o.isVariable()) {
						// iterate over everything
						Iterator<Record<T>> values = index.iterator();
						qi = new RecordIterator<>(Var.alloc(s), Var.alloc(o),
							values, binding, getExecContext());
					} else if (s.isVariable()
						&& o.isLiteral()
						&& o.getLiteralValue() instanceof Number
						&& index.getRecordFactory().getNumericClass()
						.equals(o.getLiteralValue().getClass())) {
						T value = index.getRecordFactory().getNumericClass()
							.cast(o.getLiteralValue());
						// iterate over all subjects that are indexed against the
						// given value
						Iterator<Record<T>> values = index.query(value);
						qi = new RecordIterator<>(Var.alloc(s), null, values,
							binding, getExecContext());
					} else if (s.isURI() && o.isLiteral()) {
						// lookup the value
						Record<T> rec = index.find(s);
						if (rec.getValue().equals(o.getLiteralValue())) {
							qi = IterLib.result(binding, getExecContext());
						} else {
							qi = IterLib.noResults(getExecContext());
							break;
						}
					} else if (s.isURI() && o.isVariable()) {
						// lookup the value
						Record<T> rec = index.find(s);
						BindingMap b = BindingFactory.create(binding);
						if (null != rec) {
							b.add(Var.alloc(o),
								ResourceFactory.createTypedLiteral(rec.getValue())
								.asNode());
							qi = IterLib.result(b, getExecContext());
						} else {
							qi = IterLib.noResults(getExecContext());
							break;
						}
					} else {
						// invalid triple
						qi = IterLib.noResults(getExecContext());
						break;
					}
				}
				return qi;
			}
		};
		return ret;
	}

	/** {@inheritDoc} */
	@Override
	public BasicPattern examine(BasicPattern pattern) {
		BasicPattern p = new BasicPattern();
		for (Triple t : pattern) {
			if (t.getSubject().isVariable()
				&& t.getPredicate().getURI().equals(predicate)
				&& t.getObject().isVariable()) {
				p.add(t);
			}
		}
		return p;
	}

	/** Iterator for numeric records. */
	private static class RecordIterator<T extends Number & Serializable & Comparable<T>>
	extends QueryIter {

		private Var subject;
		private Var object;
		private Iterator<Record<T>> iterator;
		private Binding binding;

		public RecordIterator(Var subject, Var object,
			Iterator<Record<T>> iterator, Binding binding,
			ExecutionContext execCxt) {
			super(execCxt);
			this.subject = subject;
			this.object = object;
			this.iterator = iterator;
			this.binding = binding;
		}

		/** {@inheritDoc} */
		@Override
		protected boolean hasNextBinding() {
			return iterator.hasNext();
		}

		/** {@inheritDoc} */
		@Override
		protected Binding moveToNextBinding() {
			Record<T> rec = iterator.next();
			BindingMap b = null;
			if (null != binding) {
				b = BindingFactory.create(binding);
			} else {
				b = BindingFactory.create();
			}
			if (null != subject) {
				b.add(subject, rec.getKey());
			}
			if (null != object) {
				b.add(object, ResourceFactory.createTypedLiteral(rec.getValue())
					.asNode());
			}
			return b;
		}

		/** {@inheritDoc} */
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
