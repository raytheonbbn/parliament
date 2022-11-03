package com.bbn.parliament.jena.query.index.mock;

import java.util.Collections;
import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;

import com.bbn.parliament.jena.graph.index.IndexBase;
import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.RangeIndex;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.bbn.parliament.jena.query.index.QueryCache;


public class MockIndex extends IndexBase<Integer> implements RangeIndex<Integer> {
	public static final String NAMESPACE = "http://mock.example.org/";

	private boolean added;
	private boolean removed;
	private boolean deleted;
	private boolean registered;
	private boolean unregistered;
	private boolean cleared;
	private boolean flushed;
	private boolean rangeIteratorCalled;
	private QueryCache<Integer> cache;

	public MockIndex() {
		super();
		added = false;
		removed = false;
		deleted = false;
		cleared = false;
		rangeIteratorCalled = false;
		flushed = false;
		cache = new QueryCache<>(10);
	}

	@Override
	public RecordFactory<Integer> getRecordFactory() {
		return new MockRecordFactory();
	}

	@Override
	public void register(Graph graph, Node graphName) {
		PropertyFunctionRegistry.get().put(
			MockPropertyFunction.URI, new MockPropertyFunction.MockPropFxnFactory());
		registered = true;
	}

	@Override
	public void unregister(Graph graph, Node graphName) {
		PropertyFunctionRegistry.get().remove(MockPropertyFunction.URI);
		unregistered = true;
	}

	@Override
	protected void doClose() throws IndexException {
	}

	@Override
	protected void doOpen() throws IndexException {
	}

	@Override
	protected void doDelete() throws IndexException {
		deleted = true;
	}

	@Override
	protected void doClear() throws IndexException {
		cleared = true;
	}

	@Override
	protected boolean doAdd(Record<Integer> record) throws IndexException {
		added = true;
		return true;
	}

	@Override
	protected boolean doRemove(Record<Integer> record) throws IndexException {
		removed = true;
		return true;
	}

	@Override
	protected Iterator<Record<Integer>> doIterator() {
		return Collections.<Record<Integer>> emptyList().iterator();
	}

	@Override
	protected long doSize() throws IndexException {
		return 1;
	}

	public boolean isAdded() {
		return added;
	}

	public boolean isRemoved() {
		return removed;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public boolean isRegistered() {
		return registered;
	}

	public boolean isUnregistered() {
		return unregistered;
	}

	public boolean isCleared() {
		return cleared;
	}

	@Override
	public Record<Integer> find(Node node) {
		return Record.create(node, 0);
	}

	@Override
	public Iterator<Record<Integer>> query(Integer value) {
		return Collections
			.singletonList(Record.create(NodeFactory.createURI("http://example.org/node"), value))
			.iterator();
	}

	@Override
	public QueryCache<Integer> getQueryCache() {
		return cache;
	}

	@Override
	public Iterator<Record<Integer>> iterator(Integer start, Integer end) {
		rangeIteratorCalled = true;
		return Collections
			.singletonList(Record.create(NodeFactory
				.createURI("http://example.org/node"), end))
			.iterator();
	}

	public boolean isRangeIteratorCalled() {
		return rangeIteratorCalled;
	}

	@Override
	public void flush() throws IndexException {
		flushed = true;
	}

	public boolean isFlushed() {
		return flushed;
	}

	@Override
	protected void doAdd(Iterator<Record<Integer>> records) throws IndexException {
	}

	@Override
	protected void doRemove(Iterator<Record<Integer>> records) throws IndexException {
	}
}
