// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

/** @author dkolas */
public class TemporalExtentIterator implements ClosableIterator<Record<TemporalExtent>> {
	private InclusionDecider inclusionDecider;
	private Iterator<Record<TemporalExtent>> instants;

	private Record<TemporalExtent> current;

	public TemporalExtentIterator(Iterator<Record<TemporalExtent>> extents, InclusionDecider inclusionDecider){
		this.instants = extents;
		this.inclusionDecider = inclusionDecider;
	}

	@Override
	public boolean hasNext() {
		if (current != null){
			return true;
		}
		while(instants.hasNext()){
			current = instants.next();
			TemporalExtent extent = inclusionDecider.test((TemporalInstant)current.getValue());
			if (extent != null){
				return true;
			}
			current = null;
		}
		return false;
	}

	@Override
	public Record<TemporalExtent> next() {
		if (!hasNext()){
			throw new NoSuchElementException();
		}
		Record<TemporalExtent> result = current;
		current = null;
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
	}

	/** This interface decides whether to include the current element in the iterator. */
	public interface InclusionDecider {
		public abstract TemporalExtent test(TemporalInstant extent);
	}
}
