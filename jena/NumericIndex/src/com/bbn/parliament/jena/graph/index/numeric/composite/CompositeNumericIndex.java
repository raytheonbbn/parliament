package com.bbn.parliament.jena.graph.index.numeric.composite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.CompositeIndex;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexBase;
import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.Record.TripleRecord;
import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.bbn.parliament.jena.graph.index.numeric.NumericIndex;
import com.bbn.parliament.jena.graph.index.numeric.NumericIndexFactory;
import com.bbn.parliament.jena.query.index.IndexPatternQuerierManager;
import com.bbn.parliament.jena.util.FileUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * A composite index that dynamically generates a {@link NumericIndex} for
 * predicates with numeric objects. The composite index is not queryable, but
 * it's sub indexes are. The sub indexes are registered with the
 * {@link IndexManager} and act to the query engine as if they were configured
 * manually.
 *
 * @author rbattle
 */
public class CompositeNumericIndex extends IndexBase<Number> implements CompositeIndex<Number> {

	private Map<String, NumericIndex<? extends Number>> subIndexes;
	private String indexDir;
	private CompositeNumericRecordFactory recordFactory;
	private Graph graph;
	private Node graphName;

	/**
	 * Create a new instance for the given graph and index directory.
	 *
	 * @param graph a graph
	 * @param graphName the name of the graph
	 * @param indexDir the directory to store the indexes.
	 */
	public CompositeNumericIndex(Graph graph, Node graphName, String indexDir) {
		this.graph = graph;
		this.graphName = graphName;
		this.indexDir = indexDir + File.separator + "numeric";
		this.subIndexes = new HashMap<String, NumericIndex<?>>();
		this.recordFactory = new CompositeNumericRecordFactory(this);
	}

	/** {@inheritDoc} */
	@Override
	public Collection<Index<Number>> getSubIndexes() {
		Collection<Index<Number>> indexes = new ArrayList<>();
		for (Index<? extends Number> index : subIndexes.values()) {
			@SuppressWarnings("unchecked")
			Index<Number> i = (Index<Number>) index;
			indexes.add(i);
		}
		return indexes;
	}

	/** {@inheritDoc} */
	@Override
	public void doClose() throws IndexException {
		for (Index<Number> index : getSubIndexes()) {
			index.close();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void doOpen() throws IndexException {
		for (Index<Number> index : getSubIndexes()) {
			index.open();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void doDelete() throws IndexException {
		for (Index<Number> index : getSubIndexes()) {
			index.delete();
		}
		FileUtil.delete(new File(indexDir));
	}

	/** {@inheritDoc} */
	@Override
	public void doClear() throws IndexException {
		for (Index<Number> index : getSubIndexes()) {
			index.clear();
		}
	}

	/** {@inheritDoc} */
	@Override
	public long doSize() throws IndexException {
		long size = 0;
		for (Index<Number> index : getSubIndexes()) {
			size += index.size();
		}
		return size;
	}

	private NumericIndex<? extends Number> createIndex(String predicate,
		Number value) {
		if (value instanceof Integer) {
			return createIntegerIndex(predicate);
		} else if (value instanceof Long) {
			return createLongIndex(predicate);
		} else if (value instanceof Double) {
			return createDoubleIndex(predicate);
		} else if (value instanceof Float) {
			return createFloatIndex(predicate);
		}
		throw new RuntimeException(value.getClass().getName()
			+ " is in invalid type for the numeric index");
	}

	private NumericIndex<Integer> createIntegerIndex(String predicate) {
		NumericIndexFactory<Integer> f = new NumericIndexFactory.IntegerIndexFactory();
		return createIndex(f, predicate);
	}

	private NumericIndex<Long> createLongIndex(String predicate) {
		NumericIndexFactory<Long> f = new NumericIndexFactory.LongIndexFactory();
		return createIndex(f, predicate);
	}

	private NumericIndex<Double> createDoubleIndex(String predicate) {
		NumericIndexFactory<Double> f = new NumericIndexFactory.DoubleIndexFactory();
		return createIndex(f, predicate);
	}

	private NumericIndex<Float> createFloatIndex(String predicate) {
		NumericIndexFactory<Float> f = new NumericIndexFactory.FloatIndexFactory();
		return createIndex(f, predicate);
	}

	/**
	 * Create an index for the given predicate and number type.
	 *
	 * @param factory an index factory
	 * @param predicate a predicate
	 * @return a new NumericIndex or null if the number type is not <code>Integer</code>,
	 *         <code>Long</code>, <code>Double</code>, or <code>Float</code>.
	 */
	private <T extends Number & Comparable<T>> NumericIndex<T> createIndex(
		NumericIndexFactory<T> factory, String predicate) {
		factory.setPredicate(predicate);
		NumericIndex<T> index = factory.createIndex(graph, graphName, indexDir);
		IndexPatternQuerierManager.getInstance().register(index,
			index.getQuerier());

		IndexManager.getInstance().register(graph, graphName, factory, index);
		subIndexes.put(predicate, index);

		return index;
	}

	/** {@inheritDoc} */
	@Override
	public boolean doAdd(Record<Number> r) throws IndexException {
		if (!(r instanceof TripleRecord)) {
			return false;
		}
		TripleRecord<Number> record = (TripleRecord<Number>) r;
		String pred = record.getTriple().getPredicate().getURI();
		NumericIndex<?> index = subIndexes.get(pred);
		if (null == index) {
			index = createIndex(pred, record.getValue());
		}
		if (null == index) {
			return false;
		}
		if (index.isClosed()) {
			index.open();
		}
		return add(index, r);
	}

	private static <T extends Number & Comparable<T>> boolean add(
		NumericIndex<T> index, Record<Number> r) throws IndexException {
		T value = index.getRecordFactory().getNumericClass().cast(r.getValue());
		Record<T> record = Record.create(r.getKey(), value);
		return index.add(record);
	}

	private static <T extends Number & Comparable<T>> boolean remove(
		NumericIndex<T> index, Record<Number> r) throws IndexException {
		T value = index.getRecordFactory().getNumericClass().cast(r.getValue());
		Record<T> record = Record.create(r.getKey(), value);
		return index.remove(record);
	}

	/** {@inheritDoc} */
	@Override
	public boolean doRemove(Record<Number> r) throws IndexException {
		if (!(r instanceof TripleRecord)) {
			return false;
		}
		TripleRecord<Number> record = (TripleRecord<Number>) r;
		String pred = record.getTriple().getPredicate().getURI();
		NumericIndex<?> index = subIndexes.get(pred);
		if (null == index) {
			return false;
		}
		return remove(index, record);
	}

	/** {@inheritDoc} */
	@Override
	public RecordFactory<Number> getRecordFactory() {
		return recordFactory;
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<Number>> doIterator() {
		final Iterator<Map.Entry<String, NumericIndex<? extends Number>>> nit = subIndexes.entrySet().iterator();

		Iterator<Record<Number>> it = new ClosableIterator<Record<Number>>() {
			private Iterator<?> current = null;
			private boolean hasNext = false;
			private boolean hasBeenNexted = true;
			private String predicate = null;

			@Override
			public boolean hasNext() {
				if (!hasBeenNexted) {
					return hasNext;
				}
				while (null == current || !current.hasNext()) {
					if (nit.hasNext()) {
						if (null != current) {
							NiceIterator.close(current);
						}
						Map.Entry<String, NumericIndex<? extends Number>> c = nit.next();
						predicate = c.getKey();
						current = c.getValue().iterator();
					} else {
						hasNext = false;
						hasBeenNexted = false;
						return hasNext;
					}
				}

				hasNext = current.hasNext();
				if (!hasNext) {
					NiceIterator.close(current);
					current = null;
				}
				hasBeenNexted = false;
				return hasNext;
			}

			@Override
			public Record<Number> next() {
				if (null == current) {
					if (!hasNext()) {
						throw new RuntimeException("No more items");
					}
				}
				// stupid generics..
				@SuppressWarnings("unchecked")
				Record<Number> r = (Record<Number>) current.next();

				// need to create a TripleRecord from the sub record in order to match what was added to this index
				Triple t = Triple.create(r.getKey(), Node.createURI(predicate), ResourceFactory.createTypedLiteral(r.getValue()).asNode());
				TripleRecord<Number> ret = TripleRecord.create(r.getKey(), r.getValue(), t);
				hasBeenNexted = true;
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void close() {
				if (null != current) {
					NiceIterator.close(current);
					current = null;
				}
				NiceIterator.close(nit);
				hasNext = false;
				hasBeenNexted = false;
			}
		};

		return it;
	}

	/** {@inheritDoc} */
	@Override
	public void register(Graph g, Node gName) {
		// do nothing
	}

	/** {@inheritDoc} */
	@Override
	public void unregister(Graph g, Node gName) {
		IndexManager manager = IndexManager.getInstance();
		for (Index<Number> sub : getSubIndexes()) {
			manager.unregister(graph, graphName, sub);
		}
	}

	@Override
	public void flush() throws IndexException {
		for (Index<Number> sub : getSubIndexes()) {
			sub.flush();
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void doAdd(Iterator<Record<Number>> records) throws IndexException {
		while (records.hasNext()) {
			doAdd(records.next());
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void doRemove(Iterator<Record<Number>> records)
		throws IndexException {
		while (records.hasNext()) {
			doRemove(records.next());
		}
	}
}
