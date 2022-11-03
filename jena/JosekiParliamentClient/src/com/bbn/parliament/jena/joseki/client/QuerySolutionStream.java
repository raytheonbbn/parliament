package com.bbn.parliament.jena.joseki.client;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;


/**
 * An implementation of Stream&lt;QuerySolution&gt; that converts a Jena
 * QueryExecution into a stream of QuerySolution objects. Note that this class
 * should always be instantiated via a try-with-resources statement to ensure
 * proper closure of the underlying QueryExecution.
 *
 * @author iemmons
 */
public class QuerySolutionStream implements Stream<QuerySolution> {
	private final QueryExecution qe;
	private final Stream<QuerySolution> underlyingStream;

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects by calling
	 * {@code QueryExecutionFactory.sparqlService(service, queryStr)}
	 *
	 * @param queryStr The query to execute
	 * @param service The service against which to execute the query
	 */
	public QuerySolutionStream(String queryStr, String service) {
		this(QueryExecutionFactory.sparqlService(service, queryStr));
	}

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects by calling
	 * {@code QueryExecutionFactory.sparqlService(service, query)}
	 *
	 * @param query The query to execute
	 * @param service The service against which to execute the query
	 */
	public QuerySolutionStream(Query query, String service) {
		this(QueryExecutionFactory.sparqlService(service, query));
	}

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects by calling
	 * {@code QueryExecutionFactory.create(queryStr, dataset)}
	 *
	 * @param queryStr The query to execute
	 * @param dataset The dataset against which to execute the query
	 */
	public QuerySolutionStream(String queryStr, Dataset dataset) {
		this(QueryExecutionFactory.create(queryStr, dataset));
	}

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects by calling
	 * {@code QueryExecutionFactory.create(query, dataset)}
	 *
	 * @param query The query to execute
	 * @param dataset The dataset against which to execute the query
	 */
	public QuerySolutionStream(Query query, Dataset dataset) {
		this(QueryExecutionFactory.create(query, dataset));
	}

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects by calling
	 * {@code QueryExecutionFactory.create(queryStr, model)}
	 *
	 * @param queryStr The query to execute
	 * @param model The model against which to execute the query
	 */
	public QuerySolutionStream(String queryStr, Model model) {
		this(QueryExecutionFactory.create(queryStr, model));
	}

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects by calling
	 * {@code QueryExecutionFactory.create(query, model)}
	 *
	 * @param query The query to execute
	 * @param model The model against which to execute the query
	 */
	public QuerySolutionStream(Query query, Model model) {
		this(QueryExecutionFactory.create(query, model));
	}

	/**
	 * Creates an AutoCloseable stream of QuerySolution objects that wraps its
	 * argument
	 *
	 * @param queryExecution The QueryExecution object from which the stream of
	 *                       QuerySolution objects is obtained
	 */
	public QuerySolutionStream(QueryExecution queryExecution) {
		qe = queryExecution;
		ResultSet rs = qe.execSelect();
		Spliterator<QuerySolution> spl = Spliterators.spliteratorUnknownSize(rs, Spliterator.IMMUTABLE);
		underlyingStream = StreamSupport.stream(spl, false);
	}

	@Override
	public Iterator<QuerySolution> iterator() {
		return underlyingStream.iterator();
	}

	@Override
	public Spliterator<QuerySolution> spliterator() {
		return underlyingStream.spliterator();
	}

	@Override
	public boolean isParallel() {
		return underlyingStream.isParallel();
	}

	@Override
	public Stream<QuerySolution> sequential() {
		return underlyingStream.sequential();
	}

	@Override
	public Stream<QuerySolution> parallel() {
		return underlyingStream.parallel();
	}

	@Override
	public Stream<QuerySolution> unordered() {
		return underlyingStream.unordered();
	}

	@Override
	public Stream<QuerySolution> onClose(Runnable closeHandler) {
		return underlyingStream.onClose(closeHandler);
	}

	@Override
	public void close() {
		qe.close();
	}

	@Override
	public Stream<QuerySolution> filter(Predicate<? super QuerySolution> predicate) {
		return underlyingStream.filter(predicate);
	}

	@Override
	public <R> Stream<R> map(Function<? super QuerySolution, ? extends R> mapper) {
		return underlyingStream.map(mapper);
	}

	@Override
	public IntStream mapToInt(ToIntFunction<? super QuerySolution> mapper) {
		return underlyingStream.mapToInt(mapper);
	}

	@Override
	public LongStream mapToLong(ToLongFunction<? super QuerySolution> mapper) {
		return underlyingStream.mapToLong(mapper);
	}

	@Override
	public DoubleStream mapToDouble(ToDoubleFunction<? super QuerySolution> mapper) {
		return underlyingStream.mapToDouble(mapper);
	}

	@Override
	public <R> Stream<R> flatMap(Function<? super QuerySolution, ? extends Stream<? extends R>> mapper) {
		return underlyingStream.flatMap(mapper);
	}

	@Override
	public IntStream flatMapToInt(Function<? super QuerySolution, ? extends IntStream> mapper) {
		return underlyingStream.flatMapToInt(mapper);
	}

	@Override
	public LongStream flatMapToLong(Function<? super QuerySolution, ? extends LongStream> mapper) {
		return underlyingStream.flatMapToLong(mapper);
	}

	@Override
	public DoubleStream flatMapToDouble(Function<? super QuerySolution, ? extends DoubleStream> mapper) {
		return underlyingStream.flatMapToDouble(mapper);
	}

	@Override
	public Stream<QuerySolution> distinct() {
		return underlyingStream.distinct();
	}

	@Override
	public Stream<QuerySolution> sorted() {
		return underlyingStream.sorted();
	}

	@Override
	public Stream<QuerySolution> sorted(Comparator<? super QuerySolution> comparator) {
		return underlyingStream.sorted(comparator);
	}

	@Override
	public Stream<QuerySolution> peek(Consumer<? super QuerySolution> action) {
		return underlyingStream.peek(action);
	}

	@Override
	public Stream<QuerySolution> limit(long maxSize) {
		return underlyingStream.limit(maxSize);
	}

	@Override
	public Stream<QuerySolution> skip(long n) {
		return underlyingStream.skip(n);
	}

	@Override
	public void forEach(Consumer<? super QuerySolution> action) {
		underlyingStream.forEach(action);
	}

	@Override
	public void forEachOrdered(Consumer<? super QuerySolution> action) {
		underlyingStream.forEachOrdered(action);
	}

	@Override
	public Object[] toArray() {
		return underlyingStream.toArray();
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return underlyingStream.toArray(generator);
	}

	@Override
	public QuerySolution reduce(QuerySolution identity, BinaryOperator<QuerySolution> accumulator) {
		return underlyingStream.reduce(identity, accumulator);
	}

	@Override
	public Optional<QuerySolution> reduce(BinaryOperator<QuerySolution> accumulator) {
		return underlyingStream.reduce(accumulator);
	}

	@Override
	public <U> U reduce(U identity, BiFunction<U, ? super QuerySolution, U> accumulator, BinaryOperator<U> combiner) {
		return underlyingStream.reduce(identity, accumulator, combiner);
	}

	@Override
	public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super QuerySolution> accumulator,
		BiConsumer<R, R> combiner) {
		return underlyingStream.collect(supplier, accumulator, combiner);
	}

	@Override
	public <R, A> R collect(Collector<? super QuerySolution, A, R> collector) {
		return underlyingStream.collect(collector);
	}

	@Override
	public Optional<QuerySolution> min(Comparator<? super QuerySolution> comparator) {
		return underlyingStream.min(comparator);
	}

	@Override
	public Optional<QuerySolution> max(Comparator<? super QuerySolution> comparator) {
		return underlyingStream.max(comparator);
	}

	@Override
	public long count() {
		return underlyingStream.count();
	}

	@Override
	public boolean anyMatch(Predicate<? super QuerySolution> predicate) {
		return underlyingStream.anyMatch(predicate);
	}

	@Override
	public boolean allMatch(Predicate<? super QuerySolution> predicate) {
		return underlyingStream.allMatch(predicate);
	}

	@Override
	public boolean noneMatch(Predicate<? super QuerySolution> predicate) {
		return underlyingStream.noneMatch(predicate);
	}

	@Override
	public Optional<QuerySolution> findFirst() {
		return underlyingStream.findFirst();
	}

	@Override
	public Optional<QuerySolution> findAny() {
		return underlyingStream.findAny();
	}
}
