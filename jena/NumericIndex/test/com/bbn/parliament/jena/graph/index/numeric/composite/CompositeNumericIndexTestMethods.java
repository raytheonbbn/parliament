package com.bbn.parliament.jena.graph.index.numeric.composite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.IndexFactory.IndexFactoryHelper;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.Record.TripleRecord;
import com.bbn.parliament.jena.graph.index.numeric.NumericIndex;
import com.bbn.parliament.jena.query.index.IndexTestMethods;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class CompositeNumericIndexTestMethods extends IndexTestMethods<CompositeNumericIndex, Number> {
	private static final String INT_URI = "http://example.org#int";
	private static final String DOUBLE_URI = "http://example.org#double";
	private static final String EXAMPLE_URI = "http://example.org/";

	private static final double[] DOUBLE_VALUES = { 5.2d, 1.343d, 99.1d, 10.6d };
	private static final int[] INT_VALUES = { 5, 1, 99, 10 };

	@Override
	protected IndexFactory<CompositeNumericIndex, Number> getIndexFactory() {
		return new CompositeNumericIndexFactory();
	}

	@Override
	protected Record<Number> createRecord(int seed) {
		Node key = Node.createURI(EXAMPLE_URI + seed);
		Number value = null;
		String predicate = null;
		if (seed % 2 == 0) {
			value = DOUBLE_VALUES[seed / 2];
			predicate = DOUBLE_URI;
		} else {
			value = INT_VALUES[(seed - 1) / 2];
			predicate = INT_URI;
		}
		Triple triple = Triple.create(key, Node.createURI(predicate),
			ResourceFactory.createTypedLiteral(value).asNode());
		return TripleRecord.create(key, value, triple);
	}

	@Override
	protected void doSetup() {
	}

	@Override
	protected boolean checkDeleted(CompositeNumericIndex index, Graph graph, Node graphName) {
		File f = new File(IndexFactoryHelper.getIndexDirectory(graph, graphName), "numeric");
		return !f.exists();
	}

	public void testSubIndexes(CompositeNumericIndex index) {
		assertEquals(0, index.getSubIndexes().size());
		Record<Number> r;
		List<Index<Number>> indexes;
		r = createRecord(0);
		index.add(r);
		assertEquals(1, index.getSubIndexes().size());

		indexes = new ArrayList<>(index.getSubIndexes());
		assertEquals(NumericIndex.DoubleIndex.class, indexes.get(0).getClass());
		Index<Number> doubleIndex = indexes.get(0);
		assertEquals(1, doubleIndex.size());

		r = createRecord(1);
		index.add(r);
		assertEquals(2, index.getSubIndexes().size());
		indexes = new ArrayList<>(index.getSubIndexes());

		List<?> classes = new ArrayList<Class<? extends NumericIndex<?>>>(
			List.of(NumericIndex.DoubleIndex.class, NumericIndex.IntegerIndex.class));
		Index<Number> intIndex = null;
		for (Index<Number> subIndex : indexes) {
			assertEquals(1, subIndex.size());
			if (classes.contains(subIndex.getClass())) {
				classes.remove(subIndex.getClass());
			}
			if (!subIndex.equals(doubleIndex)) {
				intIndex = subIndex;
			}
		}
		assertEquals(0, classes.size());
		if (null == intIndex) {
			fail("IntIndex is null");
			return;
		}
		r = createRecord(2);
		index.add(r);
		assertEquals(2, doubleIndex.size());
		assertEquals(1, intIndex.size());
	}
}
