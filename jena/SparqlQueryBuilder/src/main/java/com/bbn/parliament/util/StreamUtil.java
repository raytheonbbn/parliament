// Copyright (c) 2019, 2020 Raytheon BBN Technologies Corp.

package com.bbn.ix.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A utility class to convert iterators and iterables to streams.
 *
 * @author iemmons
 */
public class StreamUtil {
	private static final int DEFAULT_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;

	private StreamUtil() {}	// prevents instantiation

	/**
	 * Converts an iterator to a sequential stream. Assumes an ordered and immutable
	 * iterator.
	 *
	 * @param <T>  The element type of the iterator
	 * @param iter The iterator to convert
	 * @return A sequential stream containing the elements enumerated by the iterator
	 */
	public static <T> Stream<T> asStream(Iterator<T> iter) {
		return asStream(iter, DEFAULT_CHARACTERISTICS);
	}

	/**
	 * Converts an iterator to a parallel stream. Assumes an ordered and immutable
	 * iterator.
	 *
	 * @param <T>  The element type of the iterator
	 * @param iter The iterator to convert
	 * @return A parallel stream containing the elements enumerated by the iterator
	 */
	public static <T> Stream<T> asParallelStream(Iterator<T> iter) {
		return asParallelStream(iter, DEFAULT_CHARACTERISTICS);
	}

	/**
	 * Converts an iterator to a sequential stream.
	 *
	 * @param <T>                 The element type of the iterator
	 * @param iter                The iterator to convert
	 * @param iterCharacteristics The iterator's characteristics, as defined by
	 *                            constants in the Spliterator class
	 * @return A sequential stream containing the elements enumerated by the iterator
	 */
	public static <T> Stream<T> asStream(Iterator<T> iter, int iterCharacteristics) {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iter, iterCharacteristics),
			false);
	}

	/**
	 * Converts an iterator to a parallel stream.
	 *
	 * @param <T>                 The element type of the iterator
	 * @param iter                The iterator to convert
	 * @param iterCharacteristics The iterator's characteristics, as defined by
	 *                            constants in the Spliterator class
	 * @return A parallel stream containing the elements enumerated by the iterator
	 */
	public static <T> Stream<T> asParallelStream(Iterator<T> iter, int iterCharacteristics) {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iter, iterCharacteristics),
			true);
	}

	/**
	 * Converts an iterable to a sequential stream.
	 *
	 * @param <T>      The element type of the iterable
	 * @param iterable The iterable to convert
	 * @return A sequential stream containing the elements of the iterable
	 */
	public static <T> Stream<T> asStream(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Converts an iterable to a parallel stream.
	 *
	 * @param <T>      The element type of the iterable
	 * @param iterable The iterable to convert
	 * @return A parallel stream containing the elements of the iterable
	 */
	public static <T> Stream<T> asParallelStream(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), true);
	}
}
