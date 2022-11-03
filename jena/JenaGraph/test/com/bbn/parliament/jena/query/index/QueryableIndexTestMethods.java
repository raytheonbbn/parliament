package com.bbn.parliament.jena.query.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.stream.IntStream;

import org.apache.jena.graph.Node;
import org.apache.jena.util.iterator.NiceIterator;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.QueryableIndex;
import com.bbn.parliament.jena.graph.index.Record;

public abstract class QueryableIndexTestMethods<T extends QueryableIndex<I>, I> extends IndexTestMethods<T, I> {
	// Test method
	public void testLookup(T index) {
		try {
			IntStream.range(0, 5)
			.mapToObj(this::createRecord)
			.forEach(index::add);
		} catch (IndexException ex) {
			fail(ex);
		}

		Record<I> result;

		Node test1Node = createRecord(4).getKey();
		result = index.find(test1Node);
		assertNotNull(result);
		I find1Value = result.getValue();

		Node test2Node = createRecord(3).getKey();
		result = index.find(test2Node);
		assertNotNull(result);
		I find2Value = result.getValue();

		Iterator<Record<I>> results;

		results = index.query(find1Value);
		assertNotNull(results);
		assertTrue(results.hasNext());
		result = results.next();
		Node find1TestNode = result.getKey();
		NiceIterator.close(results);

		results = index.query(find2Value);
		assertNotNull(results);
		assertTrue(results.hasNext());
		result = results.next();
		Node find2TestNode = result.getKey();
		NiceIterator.close(results);

		assertEquals(test1Node, find1TestNode);
		assertEquals(test2Node, find2TestNode);
		assertNotEquals(find1Value, find2Value);
	}
}
