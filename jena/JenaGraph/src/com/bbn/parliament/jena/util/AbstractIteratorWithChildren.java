package com.bbn.parliament.jena.util;

import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIter;

public abstract class AbstractIteratorWithChildren extends QueryIter {
	private QueryIterator[] children;
	private QueryIterator lastIterator;
	private QueryIterator input;

	private boolean firstTime;
	private boolean hasBeenNexted;
	private boolean hasNextValue;
	private Object iteratorLock;


	public AbstractIteratorWithChildren(QueryIterator input, ExecutionContext context) {
		super(context);
		this.firstTime = true;
		this.hasBeenNexted = true;
		this.hasNextValue = false;
		this.iteratorLock = new Object();
		this.input = input;
	}

	private void initialize() {
		this.children = new QueryIterator[sizeOfChildren()];
		this.children[0] = createChildIterator(0, input);
	}

	protected abstract int sizeOfChildren();

	protected abstract QueryIterator createChildIterator(int index, QueryIterator inputIter);

	@Override
	protected boolean hasNextBinding() {
		if (!hasBeenNexted) {
			return hasNextValue;
		}

		synchronized (iteratorLock) {
			int last = -1;
			int index = 0;

			if (firstTime) {
				index = 0;
				firstTime = false;
				initialize();
				last = children.length - 1;
			} else {
				last = children.length - 1;
				index = last;
			}

			while (index > -1) {
				// get iterator and check if at the end of the chain
				QueryIterator it = children[index];
				if (it.hasNext()) {
					if (index == last) {
						hasBeenNexted = false;
						hasNextValue = true;
						// TODO: Check this...
						lastIterator = it;
						return true;
					}
					// spin inward
					index++;
					// create next iterator for next subquery
					QueryIterator next = createChildIterator(index, it);
					children[index] = next;
					if (index == last) {
						lastIterator = next;
					}
				} else {
					// spin outward
					index--;
				}
			}
		}

		hasBeenNexted = false;
		hasNextValue = false;

		return false;
	}

	@Override
	protected Binding moveToNextBinding() {
		synchronized (iteratorLock) {
			Binding next = lastIterator.next();
			hasBeenNexted = true;
			return next;
		}
	}

	@Override
	protected void closeIterator() {
		synchronized (iteratorLock) {
			if (null == children) {
				return;
			}
			for (int i = children.length - 1; i > -1; i--) {
				QueryIterator it = children[i];
				if (null != it) {
					it.close();
				}
			}
		}
	}

	@Override
	protected void requestCancel() {
		// TODO can we cancel?
	}
}
