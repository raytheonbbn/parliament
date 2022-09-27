package com.bbn.parliament.jena.graph.index;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphListener;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ArrayIterator;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * A wrapper for an {@link Index}. The <code>IndexListener</code> wraps an
 * <code>Index</code> with a <code>GraphListener</code> and passes all add and delete
 * events to the <code>Index</code>.
 *
 * @param <T> The type of object to index
 * @author rbattle
 */
class IndexListener<T> implements GraphListener {
	private static final Logger LOG = LoggerFactory.getLogger(IndexListener.class);
	private Index<T> index;

	/**
	 * Construct a new instance.
	 *
	 * @param index the index to wrap.
	 */
	public IndexListener(Index<T> index) {
		this.index = index;
	}

	/** Returns the wrapped the index. */
	public Index<T> getIndex() {
		return index;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((index == null) ? 0 : index.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexListener<?> other = (IndexListener<?>) obj;
		if (index == null) {
			if (other.index != null)
				return false;
		} else if (!index.equals(other.index))
			return false;
		return true;
	}

	@Override
	public void notifyAddGraph(Graph g, Graph added) {
		for (Triple t : index.getRecordFactory().getTripleMatchers()) {
			ExtendedIterator<Triple> it = added.find(t);
			notifyAddIterator(g, it);
		}
	}

	@Override
	public void notifyDeleteGraph(Graph g, Graph removed) {
		for (Triple t : index.getRecordFactory().getTripleMatchers()) {
			ExtendedIterator<Triple> it = removed.find(t);
			notifyDeleteIterator(g, it);
		}
	}

	@Override
	public void notifyAddTriple(Graph g, Triple t) {
		Record<T> r = index.getRecordFactory().createRecord(t);
		if (null == r) {
			return;
		}
		boolean added = false;
		try {
			added = index.add(r);
		} catch (IndexException ex) {
			LOG.error("Error while adding %s to index".formatted(r), ex);
		}
		if (!added) {
			// 99% of the time this means we tried to add a duplicate to the index.
			LOG.debug("Did not add {} to index {}", r, index);
		}
	}

	@Override
	public void notifyAddArray(Graph g, Triple[] triples) {
		notifyAddIterator(g, new ArrayIterator<>(triples));
	}

	@Override
	public void notifyAddList(Graph g, List<Triple> triples) {
		notifyAddIterator(g, triples.iterator());
	}

	@Override
	public void notifyAddIterator(Graph g, Iterator<Triple> it) {
		RecordIterator records = new RecordIterator(it);
		index.add(records);
	}

	@Override
	public void notifyDeleteTriple(Graph g, Triple t) {
		Record<T> r = index.getRecordFactory().createRecord(t);
		if (null == r) {
			return;
		}
		try {
			index.remove(r);
		} catch (IndexException e) {
			LOG.error("Error while removing {} from index", r);
		}
	}

	@Override
	public void notifyDeleteList(Graph g, List<Triple> triples) {
		notifyDeleteIterator(g, triples.iterator());
	}

	@Override
	public void notifyDeleteArray(Graph g, Triple[] triples) {
		notifyDeleteIterator(g, new ArrayIterator<>(triples));
	}

	@Override
	public void notifyDeleteIterator(Graph g, Iterator<Triple> it) {
		RecordIterator records = new RecordIterator(it);
		index.remove(records);
	}

	@Override
	public void notifyEvent(Graph source, Object value) {
		// notify of start/finish reading events
	}

	private class RecordIterator implements ClosableIterator<Record<T>> {
		private Iterator<Triple> triples;
		private Record<T> next;

		public RecordIterator(Iterator<Triple> triples) {
			this.triples = triples;
		}

		@Override
		public boolean hasNext() {
			while (null == next && triples.hasNext()) {
				Triple triple = triples.next();
				// next will be null if record factory cannot create a record
				next = index.getRecordFactory().createRecord(triple);
			}
			return (null != next);
		}

		@Override
		public Record<T> next() {
			Record<T> record = next;
			next = null;
			return record;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			NiceIterator.close(triples);
		}
	}
}
