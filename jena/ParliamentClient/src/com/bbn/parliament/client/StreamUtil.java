package com.bbn.parliament.client;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
	private static final int DEFAULT_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;
	private static final boolean DEFAULT_PARALLEL = false;

	private StreamUtil() {}	// prevents instantiation

	public static <T> Stream<T> asStream(Iterator<T> iter) {
		return asStream(iter, DEFAULT_CHARACTERISTICS, DEFAULT_PARALLEL);
	}

	public static <T> Stream<T> asStream(Iterator<T> iter, boolean parallel) {
		return asStream(iter, DEFAULT_CHARACTERISTICS, parallel);
	}

	public static <T> Stream<T> asStream(Iterator<T> iter, int characteristics) {
		return asStream(iter, characteristics, DEFAULT_PARALLEL);
	}

	public static <T> Stream<T> asStream(Iterator<T> iter, int characteristics, boolean parallel) {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iter, characteristics),
			parallel);
	}

	public static <T> Stream<T> asStream(Iterable<T> iterable) {
		return asStream(iterable, DEFAULT_PARALLEL);
	}

	public static <T> Stream<T> asStream(Iterable<T> iterable, boolean parallel) {
		return StreamSupport.stream(iterable.spliterator(), parallel);
	}
}
