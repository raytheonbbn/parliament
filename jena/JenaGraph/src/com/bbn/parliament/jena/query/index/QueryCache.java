package com.bbn.parliament.jena.query.index;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;

/** @author rbattle */
public class QueryCache<T> extends LinkedHashMap<Node, T> {
	private static final long serialVersionUID = 1L;

	int maxSize;
	private Map<Var, Node> blankNodeMap;

	public QueryCache(int size) {
		super(size);
		maxSize = size;
		blankNodeMap = new LinkedHashMap<>(size) {
			private static final long serialVersionUID = 1L;

			/** {@inheritDoc} */
			@Override
			protected boolean removeEldestEntry(Entry<Var, Node> eldest) {
				return size() > maxSize;
			}
		};
	}

	/** {@inheritDoc} */
	@Override
	protected boolean removeEldestEntry(Entry<Node, T> eldest) {
		return size() > maxSize;
	}

	public Map<Var, Node> getBlankNodeMap() {
		return blankNodeMap;
	}
}
