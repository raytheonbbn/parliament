package com.bbn.parliament.jena.graph.index.numeric;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.RecordFactory;

/**
 * A record factory that creates numeric records. The factory contains abstract methods
 * for determining the size of a number and methods for converting to and from byte
 * representations. The subclasses provide implementations for indexing ints, longs,
 * doubles, and floats.
 *
 * @author rbattle
 */
public abstract class NumericRecordFactory<T extends Number & Comparable<T>>
implements RecordFactory<T> {

	private final String predicate;

	private final Class<T> numericClass;

	/**
	 * Create a new instance.
	 *
	 * @param predicate the predicate that is indexed
	 * @param numericClass the type of number that is indexed
	 */
	public NumericRecordFactory(String predicate, Class<T> numericClass) {
		this.predicate = predicate;
		this.numericClass = numericClass;
	}

	/** Get the numeric class. */
	public Class<T> getNumericClass() {
		return numericClass;
	}

	/** {@inheritDoc} */
	@Override
	public Record<T> createRecord(Triple triple) {
		if (!triple.getPredicate().isURI()) {
			return null;
		}
		if (!triple.getPredicate().getURI().equals(predicate)) {
			return null;
		}
		if (!triple.getObject().isLiteral()) {
			return null;
		}
		Object value = triple.getObject().getLiteralValue();
		if (!numericClass.isInstance(value)) {
			return null;
		}
		T number = numericClass.cast(value);

		Record<T> record = Record.create(triple.getSubject(), number);
		return record;
	}

	/** {@inheritDoc} */
	@Override
	public List<Triple> getTripleMatchers() {
		return List.of(Triple.create(Node.ANY, NodeFactory.createURI(predicate), Node.ANY));
	}

	/**
	 * Convert the number to bytes.
	 *
	 * @param value a number to convert
	 * @return the byte representation.
	 */
	public byte[] getBytesForNumber(T value) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			writeValue(dos, value);
			dos.flush();
		} catch (IOException e) {
			return new byte[0];
		}
		return bos.toByteArray();
	}

	/**
	 * Convert the bytes to a number.
	 *
	 * @param value the bytes to convert
	 * @return the numeric representation.
	 */
	public T getNumberForBytes(byte[] value) {
		ByteArrayInputStream bis = new ByteArrayInputStream(value);
		DataInputStream dis = new DataInputStream(bis);
		T n;
		try {
			n = readValue(dis);
		} catch (IOException e) {
			return null;
		}
		return n;
	}

	/**
	 * Write the number to the output stream.
	 *
	 * @param dos an output stream to write to
	 * @param value the value to write
	 * @throws IOException if an error occurs.
	 */
	protected abstract void writeValue(DataOutputStream dos, T value)
		throws IOException;

	/**
	 * Read a number from the input stream
	 *
	 * @param dis an input stream to read from
	 * @return a number
	 * @throws IOException if an error occurs.
	 */
	protected abstract T readValue(DataInputStream dis) throws IOException;

	/** Get the maximum number (the largest possible value) */
	public abstract T getMaximum();

	/** Get the minimum number (the smallest possible value) */
	public abstract T getMinimum();

	/** Get the number of bytes for the number. */
	public abstract int getNumberSize();

	/** A record factory for <code>Integer</code>s. */
	public static class IntegerRecordFactory extends
	NumericRecordFactory<Integer> {
		public IntegerRecordFactory(String predicate) {
			super(predicate, Integer.class);
		}

		/** {@inheritDoc} */
		@Override
		protected void writeValue(DataOutputStream dos, Integer value)
			throws IOException {
			dos.writeInt(value.intValue());
		}

		/** {@inheritDoc} */
		@Override
		public int getNumberSize() {
			return 4;
		}

		/** {@inheritDoc} */
		@Override
		protected Integer readValue(DataInputStream dis) throws IOException {
			return dis.readInt();
		}

		/** {@inheritDoc} */
		@Override
		public Integer getMaximum() {
			return Integer.MAX_VALUE;
		}

		/** {@inheritDoc} */
		@Override
		public Integer getMinimum() {
			return Integer.MIN_VALUE;
		}
	}

	/** A record factory for <code>Long</code>s. */
	public static class LongRecordFactory extends NumericRecordFactory<Long> {
		public LongRecordFactory(String predicate) {
			super(predicate, Long.class);
		}

		/** {@inheritDoc} */
		@Override
		protected void writeValue(DataOutputStream dos, Long value)
			throws IOException {
			dos.writeLong(value.longValue());
		}

		/** {@inheritDoc} */
		@Override
		public int getNumberSize() {
			return 8;
		}

		/** {@inheritDoc} */
		@Override
		protected Long readValue(DataInputStream dis) throws IOException {
			return dis.readLong();
		}

		/** {@inheritDoc} */
		@Override
		public Long getMaximum() {
			return Long.MAX_VALUE;
		}

		/** {@inheritDoc} */
		@Override
		public Long getMinimum() {
			return Long.MIN_VALUE;
		}
	}

	/** A record factory for <code>Float</code>s. */
	public static class FloatRecordFactory extends NumericRecordFactory<Float> {
		public FloatRecordFactory(String predicate) {
			super(predicate, Float.class);
		}

		/** {@inheritDoc} */
		@Override
		protected void writeValue(DataOutputStream dos, Float value)
			throws IOException {
			dos.writeFloat(value.floatValue());

		}

		/** {@inheritDoc} */
		@Override
		protected Float readValue(DataInputStream dis) throws IOException {
			return dis.readFloat();
		}

		/** {@inheritDoc} */
		@Override
		public int getNumberSize() {
			return 4;
		}

		/** {@inheritDoc} */
		@Override
		public Float getMaximum() {
			return Float.MAX_VALUE;
		}

		/** {@inheritDoc} */
		@Override
		public Float getMinimum() {
			return Float.MIN_VALUE;
		}
	}

	/** A record factory for <code>Double</code>s. */
	public static class DoubleRecordFactory extends NumericRecordFactory<Double> {
		public DoubleRecordFactory(String predicate) {
			super(predicate, Double.class);
		}

		/** {@inheritDoc} */
		@Override
		protected void writeValue(DataOutputStream dos, Double value)
			throws IOException {
			dos.writeDouble(value.doubleValue());
		}

		/** {@inheritDoc} */
		@Override
		protected Double readValue(DataInputStream dis) throws IOException {
			return dis.readDouble();
		}

		/** {@inheritDoc} */
		@Override
		public int getNumberSize() {
			return 8;
		}

		/** {@inheritDoc} */
		@Override
		public Double getMaximum() {
			return Double.MAX_VALUE;
		}

		/** {@inheritDoc} */
		@Override
		public Double getMinimum() {
			return Double.MIN_VALUE;
		}
	}
}
