// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client.jena;

import java.util.Iterator;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;

/** @author dkolas */
public class NamedGraphResults implements Iterator<String> {
	private ResultSet resultSet;
	private String variableName;

	public NamedGraphResults(String variableName, ResultSet results) {
		this.variableName = variableName;
		this.resultSet = results;
	}

	/** @see java.util.Iterator#hasNext() */
	@Override
	public boolean hasNext() {
		return resultSet.hasNext();
	}

	/** @see java.util.Iterator#next() */
	@Override
	public String next() {
		return ((Resource) resultSet.nextSolution().get(variableName)).getURI();
	}

	/** @see java.util.Iterator#remove() */
	@Override
	public void remove() {
		throw new RuntimeException("Can't remove from this iterator!");
	}
}
