package com.bbn.parliament.jena.query.index.mock;

import java.util.List;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.hp.hpl.jena.graph.Triple;

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
