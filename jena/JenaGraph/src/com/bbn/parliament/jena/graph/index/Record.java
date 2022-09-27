package com.bbn.parliament.jena.graph.index;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * A representation of data that is indexed. The record is a single piece of indexed data.
 * It contains a key and a value.
 *
 * @author rbattle
 * @param <T> the type of object to index.
 */
public class Record<T> {
	/**
	 * Create a new instance.
	 *
	 * @param <T> the type of data that is indexed.
	 * @param key a key
	 * @param value a value
	 * @return a new record.
	 */
	public static <T> Record<T> create(Node key, T value) {
		return new Record<>(key, value);
	}

	/** The key for this instance. */
	private Node key;

	/** The value to index. */
	private T value;

	/**
	 * Construct a new instance.
	 *
	 * @param key a key
	 * @param value a value
	 */
	protected Record(Node key, T value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public Node getKey() {
		return key;
	}

	/**
	 * Get the value.
	 *
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "%1$s: %2$s".formatted(key, value);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Record<T> other = (Record<T>) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	/**
	 * A record that contains a triple. The triple record is useful for indexes that need
	 * to know the specific triple that created the record.
	 *
	 * @author rbattle
	 * @param <T> the type of data that is indexed.
	 */
	public static class TripleRecord<T> extends Record<T> {
		/**
		 * Create a new instance.
		 *
		 * @param <T> the type of data that is indexed.
		 * @param key a key
		 * @param value a value
		 * @param triple the triple
		 * @return a new record.
		 */
		public static <T> TripleRecord<T> create(Node key, T value, Triple triple) {
			return new TripleRecord<>(key, value, triple);
		}

		private Triple triple;

		/**
		 * Construct a new instance.
		 *
		 * @param key a key
		 * @param value a value
		 * @param triple a triple
		 */
		TripleRecord(Node key, T value, Triple triple) {
			super(key, value);
			this.triple = triple;
		}

		/**
		 * Get the triple.
		 *
		 * @return the triple.
		 */
		public Triple getTriple() {
			return triple;
		}

		/** {@inheritDoc} */
		@Override
		public String toString() {
			return "TripleRecord [triple=%1$s, key=%2$s, value=%3$s]"
				.formatted(triple, getKey(), getValue());
		}

		/** {@inheritDoc} */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((triple == null) ? 0 : triple.hashCode());
			return result;
		}

		/** {@inheritDoc} */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			TripleRecord<?> other = (TripleRecord<?>) obj;
			if (triple == null) {
				if (other.triple != null)
					return false;
			} else if (!triple.equals(other.triple))
				return false;
			return true;
		}
	}
}
