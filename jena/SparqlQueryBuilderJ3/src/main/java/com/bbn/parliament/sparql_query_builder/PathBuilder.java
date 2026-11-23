// Copyright (c) 2019, 2020 RTX BBN Technologies Corp.

package com.bbn.parliament.sparql_query_builder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;

/**
 * Implements the standard fluent builder pattern for Jena property paths. The
 * correct mental model for using this builder is a reverse Polish notation
 * (RPN) calculator, in other words it uses post-fix ordering of operands and
 * operators. For instance, to produce the property path {@code :foo/:bar/:baz}
 * use this code:
 *
 * <pre><code>
 *    Node fooPredicate = ...
 *    Node barPredicate = ...
 *    Node bazPredicate = ...
 *    Path path = new PathBuilder()
 *       .pushIri(fooPredicate)
 *       .pushIri(barPredicate)
 *       .sequence()
 *       .pushIri(bazPredicate)
 *       .sequence()
 *       .build();</code></pre>
 *
 * Most of the methods in this class throw {@code IllegalStateException} in case
 * of error. For instance, the {@code sequence} method will throw if there are
 * not two arguments preceding it in the stack.
 *
 * @author iemmons
 */
public class PathBuilder {
	private static final int MIN_PATH_OPERATOR_ARGS = 2;

	/** Indicates that no minimum or maximum path length is being specified */
	public static final long UNSPECIFIED = PathFactory.UNSET;

	private transient final Deque<Path> tokens;

	/**
	 * Create an empty {@code PathBuilder}
	 */
	public PathBuilder() {
		tokens = new ArrayDeque<>();
	}

	/**
	 * Push a predicate onto the stack
	 *
	 * @param resource The predicate, represented as a {@code Resource}
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder pushIri(Resource resource) {
		return pushIri(resource.asNode());
	}

	/**
	 * Push a predicate onto the stack
	 *
	 * @param node The predicate, represented as a {@code Node}
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder pushIri(Node node) {
		tokens.addFirst(PathFactory.pathLink(node));
		return this;
	}

	/**
	 * Push a predicate onto the stack
	 *
	 * @param iri The predicate, represented as a {@code String}
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder pushIri(String iri) {
		return pushIri(NodeFactory.createURI(iri));
	}

	/**
	 * Consume the previous two items from the stack, combine them as alternatives,
	 * and push the result back onto the stack.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder alternative() {
		return build("alternative", PathFactory::pathAlt);
	}

	/**
	 * Consume the previous item from the stack, add the distinct modifier, and push
	 * the result back onto the stack. This is an extension to the SPARQL standard.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder distinct() {
		return build("distinct", PathFactory::pathDistinct);
	}

	/**
	 * Consume the previous item from the stack, invert it, and push the result back
	 * onto the stack.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder invert() {
		return build("inverse", PathFactory::pathInverse);
	}

	/**
	 * Consume the previous item from the stack, add the length-between modifier,
	 * and push the result back onto the stack. This is an extension to the SPARQL
	 * standard.
	 *
	 * @param minimum The minimum length of the path. Set to {@code UNSPECIFIED} (or
	 *                zero) to leave the minimum unspecified.
	 * @param maximum The maximum length of the path. Set to {@code UNSPECIFIED} to
	 *                leave the maximum unspecified.
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder lengthBetween(long minimum, long maximum) {
		long min = (minimum == 0) ? UNSPECIFIED : minimum;
		if (min == UNSPECIFIED && maximum == UNSPECIFIED) {
			return zeroOrMore();
		} else if (min == maximum) {
			return build("mod", path -> PathFactory.pathFixedLength(path, maximum));
		} else {
			return build("lengthBetween", path -> PathFactory.pathMod(path, min, maximum));
		}
	}

	/**
	 * Consume the previous item from the stack, add the multi modifier, and push
	 * the result back onto the stack. This is an extension to the SPARQL standard.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder multi() {
		return build("multi", PathFactory::pathMulti);
	}

	/**
	 * Consume the previous item from the stack, negate it, and push the result back
	 * onto the stack. (Currently not implemented.)
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	@SuppressWarnings("static-method")
	public PathBuilder negate() {
		throw new UnsupportedOperationException(
			"Jena's PathFactory class does not support property path negation");
	}

	/**
	 * Consume the previous item from the stack, add the one-or-more operator, and
	 * push the result back onto the stack.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder oneOrMore() {
		return build("oneOrMore", PathFactory::pathOneOrMore1);
	}

	/**
	 * Consume the previous item from the stack, add the one-or-more-N modifier, and
	 * push the result back onto the stack. This is an extension to the SPARQL
	 * standard.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder oneOrMoreN() {
		return build("oneOrMoreN", PathFactory::pathOneOrMoreN);
	}

	/**
	 * Consume the previous two items from the stack, combine them as a sequence,
	 * and push the result back onto the stack.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder sequence() {
		return build("sequence", PathFactory::pathSeq);
	}

	/**
	 * Consume the previous item from the stack, add the shortest modifier, and push
	 * the result back onto the stack. This is an extension to the SPARQL standard.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder shortest() {
		return build("shortest", PathFactory::pathShortest);
	}

	/**
	 * Consume the previous item from the stack, add the zero-or-more operator, and
	 * push the result back onto the stack.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder zeroOrMore() {
		return build("zeroOrMore", PathFactory::pathZeroOrMore1);
	}

	/**
	 * Consume the previous item from the stack, add the zero-or-more-N modifier,
	 * and push the result back onto the stack. This is an extension to the SPARQL
	 * standard.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder zeroOrMoreN() {
		return build("zeroOrMoreN", PathFactory::pathZeroOrMoreN);
	}

	/**
	 * Consume the previous item from the stack, add the zero-or-one operator, and
	 * push the result back onto the stack.
	 *
	 * @return This {@code PathBuilder} to enable method chaining
	 */
	public PathBuilder zeroOrOne() {
		return build("zeroOrOne", PathFactory::pathZeroOrOne);
	}

	private PathBuilder build(String label, UnaryOperator<Path> operator) {
		if (tokens.isEmpty()) {
			throw new IllegalStateException("No argument available for " + label + " path operator");
		}
		Path arg = tokens.removeFirst();
		tokens.addFirst(operator.apply(arg));
		return this;
	}

	private PathBuilder build(String label, BinaryOperator<Path> operator) {
		if (tokens.size() < MIN_PATH_OPERATOR_ARGS) {
			throw new IllegalStateException("Insufficient arguments available for " + label + " path operator");
		}
		Path rhs = tokens.removeFirst();
		Path lhs = tokens.removeFirst();
		tokens.addFirst(operator.apply(lhs, rhs));
		return this;
	}

	/**
	 * Build the completed path, leaving the {@code PathBuilder} ready to build
	 * another property path. Throws {@code IllegalStateException} if there is not
	 * exactly one entry on the stack.
	 *
	 * @return The completed path
	 */
	public Path build() {
		if (tokens.isEmpty()) {
			throw new IllegalStateException("Path is empty");
		} else if (tokens.size() > 1) {	// NOPMD - AvoidLiteralsInIfCondition
			throw new IllegalStateException("Path has leftover arguments");
		}
		return tokens.removeFirst();
	}
}
