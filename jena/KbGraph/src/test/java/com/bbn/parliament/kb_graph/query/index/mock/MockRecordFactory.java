package com.bbn.parliament.kb_graph.query.index.mock;

import java.util.List;

import org.apache.jena.graph.Triple;

import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.RecordFactory;

public class MockRecordFactory implements RecordFactory<Integer> {
	@Override
	public Record<Integer> createRecord(Triple triple) {
		if (triple.getPredicate().getNameSpace().equals(MockIndex.NAMESPACE)) {
			return Record.create(triple.getSubject(), 0);
		}
		return null;
	}

	@Override
	public List<Triple> getTripleMatchers() {
		// TODO Auto-generated method stub
		return null;
	}
}
