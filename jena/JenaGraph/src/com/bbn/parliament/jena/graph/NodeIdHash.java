// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import org.apache.jena.graph.Node;
import org.apache.jena.mem.HashCommon;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;

/** @author dkolas */
public class NodeIdHash extends HashCommon<Node>
{
	protected long[] values;

	protected NodeIdHash(int initialCapacity)
	{
		super(initialCapacity);
		values = new long[capacity];
		clear();
	}

	/**
	 * Clear this map: all entries are removed. The keys <i>and value</i> array
	 * elements are set to null (so the values may be garbage-collected).
	 */
	public void clear()
	{
		for (int i = 0; i < capacity; i += 1)
		{
			keys[i] = null;
			values[i] = -1;
		}
	}

	public long get(Node key)
	{
		int slot = findSlot(key);
		return slot < 0 ? values[~slot] : -1;
	}

	public void put(Node key, long value)
	{
		int slot = findSlot(key);
		if (slot < 0)
			values[~slot] = value;
		else
		{
			keys[slot] = key;
			values[slot] = value;
			size += 1;
			if (size == threshold)
				grow();
		}
	}

	protected void grow()
	{
		Node[] oldContents = keys;
		long[] oldValues = values;
		final int oldCapacity = capacity;
		growCapacityAndThreshold();
		keys = newKeyArray(capacity);
		values = new long[capacity];
		for (int i = 0; i < oldCapacity; i += 1)
		{
			Node key = oldContents[i];
			if (key != null)
			{
				int j = findSlot(key);
				keys[j] = key;
				values[j] = oldValues[i];
			}
		}
	}

	@Override
	public void remove(Node key)
	{
		int slot = findSlot(key);
		if (slot < 0)
			removeFrom(~slot);
	}

	@Override
	protected void removeAssociatedValues(int here)
	{
		values[here] = -1;
	}

	@Override
	protected void moveAssociatedValues(int here, int scan)
	{
		values[here] = values[scan];
	}

	@Override
	public ExtendedIterator<Node> keyIterator()
	{
		return new NiceIterator<>()
		{
			int index = capacity - 1;

			@SuppressWarnings("synthetic-access")
			@Override
			public boolean hasNext()
			{
				while (index >= 0 && keys[index] == null)
				{
					index -= 1;
				}
				return index >= 0;
			}

			@SuppressWarnings("synthetic-access")
			@Override
			public Node next()
			{
				if (hasNext() == false)
				{
					noElements("node keys");
				}
				return keys[index--];
			}
		};
	}

	@Override
	protected Node[] newKeyArray(int arraysize) {
		return new Node[arraysize];
	}
}
