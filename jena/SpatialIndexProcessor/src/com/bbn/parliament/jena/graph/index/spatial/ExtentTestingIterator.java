package com.bbn.parliament.jena.graph.index.spatial;

import java.util.Iterator;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.Record;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * An iterator that, given an iterator of items, will check each item and return
 * only those that satisfy the {@link ExtentTester}.
 *
 * @author rbattle
 */
public class ExtentTestingIterator implements ClosableIterator<Record<Geometry>> {
	private Iterator<Record<Geometry>> it;
	private ExtentTester et;
	private Record<Geometry> current;
	private boolean hasBeenNexted = true;
	private boolean hasNextValue = false;
	private Geometry extent;

	public ExtentTestingIterator(Iterator<Record<Geometry>> it, Geometry extent, ExtentTester et) {
		this.it = it;
		this.et = et;
		this.extent = extent;
	}

	@Override
	public boolean hasNext() {
		if (!hasBeenNexted) {
			return hasNextValue;
		}

		hasNextValue = false;
		while (it.hasNext()) {
			Record<Geometry> r = it.next();
			if (et.testExtents(r.getValue(), extent)) {
				current = r;
				hasNextValue = true;
				break;
			}
		}
		hasBeenNexted = false;
		return hasNextValue;
	}

	@Override
	public Record<Geometry> next() {
		if (hasBeenNexted) {
			if (!hasNext()) {
				throw new RuntimeException("No more items");
			}
		}
		hasBeenNexted = true;
		return current;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		NiceIterator.close(it);
	}
}
