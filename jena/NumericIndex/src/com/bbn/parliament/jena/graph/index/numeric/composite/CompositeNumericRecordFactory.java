package com.bbn.parliament.jena.graph.index.numeric.composite;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.Record.TripleRecord;
import com.bbn.parliament.jena.graph.index.RecordFactory;

/**
 * Record factory for the <code>CompositeNumericIndex</code>. This factory creates
 * {@link TripleRecord}s for triples that contain a numeric object.
 *
 * @author rbattle
 */
public class CompositeNumericRecordFactory implements RecordFactory<Number> {

	private CompositeNumericIndex index;

	/** Construct a new instance. */
	public CompositeNumericRecordFactory(CompositeNumericIndex index) {
		this.index = index;
	}

	/** {@inheritDoc} */
	@Override
	public TripleRecord<Number> createRecord(Triple triple) {
		Node pred = triple.getPredicate();
		if (!pred.isURI()) {
			return null;
		}
		Node obj = triple.getObject();
		if (!obj.isLiteral()) {
			return null;
		}
		return (obj.getLiteralValue() instanceof Number numberValue)
			? TripleRecord.create(triple.getSubject(), numberValue, triple)
			: null;
	}

	@Override
	public List<Triple> getTripleMatchers() {
		List<Triple> triples = new ArrayList<>();
		for (Index<Number> indexes : index.getSubIndexes()) {
			triples.addAll(indexes.getRecordFactory().getTripleMatchers());
		}
		return triples;
	}
}
