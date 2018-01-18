package com.bbn.parliament.jena.query.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Test;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.QueryableIndex;
import com.bbn.parliament.jena.graph.index.Record;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.util.iterator.NiceIterator;

public abstract class QueryableIndexTestBase<T extends QueryableIndex<I>, I>
extends IndexTestBase<T, I> {

	@Test
	public void testLookup() {
		testLookup(defaultGraphIndex);
		testLookup(namedGraphIndex);
	}

	private void testLookup(T index) {
		try {
			index.add(createRecord(0));
			index.add(createRecord(1));
			index.add(createRecord(2));
			index.add(createRecord(3));
			index.add(createRecord(4));
		} catch (IndexException e) {
			e.printStackTrace();
			fail();
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
