package com.bbn.parliament.jena.query.index;

import java.util.ArrayList;
import java.util.List;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPattern;
import com.hp.hpl.jena.sparql.core.BasicPattern;

public class IndexPattern extends BasicPattern {
	private List<IndexSubPattern> subPatterns;

	public IndexPattern() {
		super();
		subPatterns = new ArrayList<>();
	}

	public void addAll(IndexSubPattern other) {
		subPatterns.add(other);
	}

	public List<IndexSubPattern> getSubPatterns() {
		return subPatterns;
	}
}
