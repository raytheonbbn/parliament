// Copyright (c) 2019, 2020 Raytheon BBN Technologies Corp.

package org.semwebcentral.parliament.sparql_query_builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.datatype.Duration;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_GreaterThan;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_LessThan;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalNot;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * A factory class for creating various kinds of Jena Expr objects for use in
 * queries.
 *
 * @author iemmons
 */
public class ExprFactory {
	private ExprFactory() {}	// Prevents instantiation

	/**
	 * Creates a Boolean NodeValue expression from the given value.
	 *
	 * @param value The boolean from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(boolean value) {
		return NodeValue.makeBoolean(value);
	}

	/**
	 * Creates a Decimal NodeValue expression from the given value.
	 *
	 * @param value The BigDecimal from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(BigDecimal value) {
		return (value == null) ? null : NodeValue.makeDecimal(value);
	}

	/**
	 * Creates a double NodeValue expression from the given value.
	 *
	 * @param value The double from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(double value) {
		return NodeValue.makeDouble(value);
	}

	/**
	 * Creates a Duration NodeValue expression from the given value.
	 *
	 * @param value The Duration from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(Duration value) {
		return (value == null) ? null : NodeValue.makeDuration(value);
	}

	/**
	 * Creates a float NodeValue expression from the given value.
	 *
	 * @param value The float from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(float value) {
		return NodeValue.makeFloat(value);
	}

	/**
	 * Creates an integral NodeValue expression from the given value.
	 *
	 * @param value The BigInteger from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(BigInteger value) {
		return (value == null) ? null : NodeValue.makeInteger(value);
	}

	/**
	 * Creates an integral NodeValue expression from the given value.
	 *
	 * @param value The long from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(long value) {
		return NodeValue.makeInteger(value);
	}

	/**
	 * Creates a NodeValue expression from the given Node.
	 *
	 * @param value The Node from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(Node value) {
		return (value == null) ? null : NodeValue.makeNode(value);
	}

	/**
	 * Creates a String NodeValue expression from the given value.
	 *
	 * @param value The String from which to create
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(String value) {
		return (value == null) ? null : NodeValue.makeString(value);
	}

	/**
	 * Creates a literal NodeValue expression from the given value.
	 *
	 * @param lex The lexical form of the literal
	 * @param dt The datatype URI of the literal
	 * @return The resulting NodeValue
	 */
	public static NodeValue nodeValue(String lex, Node dt) {
		return (lex == null || dt == null) ? null
			: NodeValue.makeNode(lex, TypeMapper.getInstance().getSafeTypeByName(dt.getURI()));
	}



	/**
	 * Create a filter of the form {@code regex(?var, "regex", "flags")} on the
	 * specified variable, or null if regex is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param regex Regular expression pattern
	 * @param flags Regular expression flags, as defined by XPath
	 * @return The specified filter
	 */
	public static Expr regEx(String var, String regex, RegExFlag... flags) {
		String flagStr = Arrays.stream(flags)
			.map(RegExFlag::getXPathFlag)
			.collect(Collectors.joining());
		return (regex == null) ? null : new E_Regex(new ExprVar(var), regex, flagStr);
	}



	/**
	 * Create a filter of the form {@code bound(?var)} on the
	 * specified variable, or null if var is null or empty.
	 *
	 * @param var The name of the variable to test
	 * @return The specified filter
	 */
	public static Expr bound(String var) {
		return (var == null || var.isEmpty()) ? null : new E_Bound(new ExprVar(var));
	}



	/**
	 * Creates the negation of the given expression
	 *
	 * @param expr The given expression
	 * @return The negation of the given expression
	 */
	public static Expr not(Expr expr) {
		return (expr == null) ? null : new E_LogicalNot(expr);
	}

	/**
	 * Creates the conjunction of the given expressions
	 *
	 * @param expr1 The first expression in the conjunction
	 * @param expr2 The second expression in the conjunction
	 * @param moreExprs Subsequent expressions in the conjunction
	 * @return The conjunction of the given expressions
	 */
	public static Expr and(Expr expr1, Expr expr2, Expr... moreExprs) {
		return aggregateViaBinaryOp(E_LogicalAnd::new, expr1, expr2, moreExprs);
	}

	/**
	 * Creates the disjunction of the given expressions
	 *
	 * @param expr1 The first expression in the disjunction
	 * @param expr2 The second expression in the disjunction
	 * @param moreExprs Subsequent expressions in the disjunction
	 * @return The disjunction of the given expressions
	 */
	public static Expr or(Expr expr1, Expr expr2, Expr... moreExprs) {
		return aggregateViaBinaryOp(E_LogicalOr::new, expr1, expr2, moreExprs);
	}

	private static Expr aggregateViaBinaryOp(BinaryOperator<Expr> op, Expr expr1, Expr expr2, Expr... moreExprs) {
		if (op == null || expr1 == null || expr2 == null || moreExprs == null) {
			return null;
		} else {
			List<Expr> exprs = Stream.concat(Stream.of(expr1, expr2), Arrays.stream(moreExprs))
				.collect(Collectors.toList());
			return aggregateViaBinaryOp(op, exprs);
		}
	}

	private static Expr aggregateViaBinaryOp(BinaryOperator<Expr> op, List<Expr> exprs) {
		if (op == null || exprs == null || exprs.isEmpty()) {
			return null;
		} else if (exprs.size() == 1) {	// NOPMD - AvoidLiteralsInIfCondition
			return exprs.get(0);
		} else {
			int half = exprs.size() / 2;
			List<Expr> intialExprs = exprs.subList(0, half);
			List<Expr> finalExprs = exprs.subList(half, exprs.size());
			return op.apply(aggregateViaBinaryOp(op, intialExprs), aggregateViaBinaryOp(op, finalExprs));
		}
	}



	/**
	 * Create an equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Resource value) {
		return (value == null) ? null : equal(var, value.asNode());
	}

	/**
	 * Create an equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Node value) {
		return (value == null) ? null : new E_Equals(new ExprVar(var),
			NodeValue.makeNode(value));
	}

	/**
	 * Create an equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, String value) {
		return (value == null) ? null : new E_Equals(new ExprVar(var),
			NodeValue.makeString(value));
	}

	/**
	 * Create an equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Instant value) {
		return (value == null) ? null : new E_Equals(new ExprVar(var),
			NodeValue.makeDateTime(value.toString()));
	}

	/**
	 * Create an equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Integer value) {
		return (value == null) ? null : equal(var, value.intValue());
	}

	/**
	 * Create an equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Long value) {
		return (value == null) ? null : equal(var, value.longValue());
	}

	/**
	 * Create an equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, long value) {
		return new E_Equals(new ExprVar(var), NodeValue.makeInteger(value));
	}

	/**
	 * Create an equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Float value) {
		return (value == null) ? null : equal(var, value.floatValue());
	}

	/**
	 * Create an equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, Double value) {
		return (value == null) ? null : equal(var, value.doubleValue());
	}

	/**
	 * Create an equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr equal(String var, double value) {
		return new E_Equals(new ExprVar(var), NodeValue.makeDouble(value));
	}



	/**
	 * Create a less-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, String value) {
		return (value == null) ? null : new E_LessThanOrEqual(new ExprVar(var),
			NodeValue.makeString(value));
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, Instant value) {
		return (value == null) ? null : new E_LessThanOrEqual(new ExprVar(var),
			NodeValue.makeDateTime(value.toString()));
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, Integer value) {
		return (value == null) ? null : lessThanOrEqual(var, value.intValue());
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, Long value) {
		return (value == null) ? null : lessThanOrEqual(var, value.longValue());
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, long value) {
		return new E_LessThanOrEqual(new ExprVar(var), NodeValue.makeInteger(value));
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, Float value) {
		return (value == null) ? null : lessThanOrEqual(var, value.floatValue());
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, Double value) {
		return (value == null) ? null : lessThanOrEqual(var, value.doubleValue());
	}

	/**
	 * Create a less-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThanOrEqual(String var, double value) {
		return new E_LessThanOrEqual(new ExprVar(var), NodeValue.makeDouble(value));
	}



	/**
	 * Create a less-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, String value) {
		return (value == null) ? null : new E_LessThan(new ExprVar(var),
			NodeValue.makeString(value));
	}

	/**
	 * Create a less-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, Instant value) {
		return (value == null) ? null : new E_LessThan(new ExprVar(var),
			NodeValue.makeDateTime(value.toString()));
	}

	/**
	 * Create a less-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, Integer value) {
		return (value == null) ? null : lessThan(var, value.intValue());
	}

	/**
	 * Create a less-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, Long value) {
		return (value == null) ? null : lessThan(var, value.longValue());
	}

	/**
	 * Create a less-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, long value) {
		return new E_LessThan(new ExprVar(var), NodeValue.makeInteger(value));
	}

	/**
	 * Create a less-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, Float value) {
		return (value == null) ? null : lessThan(var, value.floatValue());
	}

	/**
	 * Create a less-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, Double value) {
		return (value == null) ? null : lessThan(var, value.doubleValue());
	}

	/**
	 * Create a less-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr lessThan(String var, double value) {
		return new E_LessThan(new ExprVar(var), NodeValue.makeDouble(value));
	}



	/**
	 * Create a greater-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, String value) {
		return (value == null) ? null : new E_GreaterThanOrEqual(new ExprVar(var),
			NodeValue.makeString(value));
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, Instant value) {
		return (value == null) ? null : new E_GreaterThanOrEqual(new ExprVar(var),
			NodeValue.makeDateTime(value.toString()));
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, Integer value) {
		return (value == null) ? null : greaterThanOrEqual(var, value.intValue());
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, Long value) {
		return (value == null) ? null : greaterThanOrEqual(var, value.longValue());
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, long value) {
		return new E_GreaterThanOrEqual(new ExprVar(var), NodeValue.makeInteger(value));
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, Float value) {
		return (value == null) ? null : greaterThanOrEqual(var, value.floatValue());
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, Double value) {
		return (value == null) ? null : greaterThanOrEqual(var, value.doubleValue());
	}

	/**
	 * Create a greater-than-or-equal-to filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThanOrEqual(String var, double value) {
		return new E_GreaterThanOrEqual(new ExprVar(var), NodeValue.makeDouble(value));
	}



	/**
	 * Create a greater-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, String value) {
		return (value == null) ? null : new E_GreaterThan(new ExprVar(var),
			NodeValue.makeString(value));
	}

	/**
	 * Create a greater-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, Instant value) {
		return (value == null) ? null : new E_GreaterThan(new ExprVar(var),
			NodeValue.makeDateTime(value.toString()));
	}

	/**
	 * Create a greater-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, Integer value) {
		return (value == null) ? null : greaterThan(var, value.intValue());
	}

	/**
	 * Create a greater-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, Long value) {
		return (value == null) ? null : greaterThan(var, value.longValue());
	}

	/**
	 * Create a greater-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, long value) {
		return new E_GreaterThan(new ExprVar(var), NodeValue.makeInteger(value));
	}

	/**
	 * Create a greater-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, Float value) {
		return (value == null) ? null : greaterThan(var, value.floatValue());
	}

	/**
	 * Create a greater-than filter on the specified variable, or null
	 * if value is null.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, Double value) {
		return (value == null) ? null : greaterThan(var, value.doubleValue());
	}

	/**
	 * Create a greater-than filter on the specified variable.
	 *
	 * @param var Name of the variable ("?" is not necessary)
	 * @param value Value to test against
	 * @return The specified filter
	 */
	public static Expr greaterThan(String var, double value) {
		return new E_GreaterThan(new ExprVar(var), NodeValue.makeDouble(value));
	}
}
