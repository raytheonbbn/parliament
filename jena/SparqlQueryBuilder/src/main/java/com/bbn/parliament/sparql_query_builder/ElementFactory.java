// Copyright (c) 2019, 2020 RTX BBN Technologies Corp.

package com.bbn.parliament.sparql_query_builder;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;

/**
 * A factory class for creating various kinds of Jena {@code Element} objects
 * for use in queries.
 *
 * @author iemmons
 */
public class ElementFactory {
	private ElementFactory() {}	// Prevents instantiation

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Resource s, Resource p, Resource o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s.asNode(), p.asNode(), o.asNode());
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Resource s, Resource p, Node o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s.asNode(), p.asNode(), o);
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Resource s, Node p, Resource o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s.asNode(), p, o.asNode());
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Node s, Resource p, Resource o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s, p.asNode(), o.asNode());
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Resource s, Node p, Node o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s.asNode(), p, o);
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Node s, Resource p, Node o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s, p.asNode(), o);
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Node s, Node p, Resource o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		return basicGraphPattern(s, p, o.asNode());
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern, using
	 * either variables or binding known values to any argument. Note that any
	 * variables are NOT automatically added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param p Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Node s, Node p, Node o) {
		if (s == null || p == null || o == null) {
			return null;
		}
		ElementTriplesBlock block = new ElementTriplesBlock();
		block.addTriple(Triple.create(s, p, o));
		return block;
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern whose
	 * predicate is a path expression. The subject and object may be either
	 * variables or known values. Note that any variables are NOT automatically
	 * added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param path Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Resource s, Path path, Resource o) {
		if (s == null || path == null || o == null) {
			return null;
		}
		return basicGraphPattern(s.asNode(), path, o.asNode());
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern whose
	 * predicate is a path expression. The subject and object may be either
	 * variables or known values. Note that any variables are NOT automatically
	 * added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param path Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Node s, Path path, Resource o) {
		if (s == null || path == null || o == null) {
			return null;
		}
		return basicGraphPattern(s, path, o.asNode());
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern whose
	 * predicate is a path expression. The subject and object may be either
	 * variables or known values. Note that any variables are NOT automatically
	 * added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param path Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Resource s, Path path, Node o) {
		if (s == null || path == null || o == null) {
			return null;
		}
		return basicGraphPattern(s.asNode(), path, o);
	}

	/**
	 * Creates a basic graph pattern containing a single triple pattern whose
	 * predicate is a path expression. The subject and object may be either
	 * variables or known values. Note that any variables are NOT automatically
	 * added to the SELECT clause.
	 *
	 * @param s Subject of the triple pattern
	 * @param path Predicate of the triple pattern
	 * @param o Object of the triple pattern
	 * @return The specified Element
	 */
	public static Element basicGraphPattern(Node s, Path path, Node o) {
		if (s == null || path == null || o == null) {
			return null;
		}
		ElementPathBlock block = new ElementPathBlock();
		block.addTriple(new TriplePath(s, path, o));
		return block;
	}

	/**
	 * Create a group block containing the given element.
	 *
	 * @param element The content of the group block
	 * @return The specified Element
	 */
	public static ElementGroup group(Element element) {
		if (element == null) {
			return null;
		}
		ElementGroup group = new ElementGroup();
		group.addElement(element);
		return group;
	}

	/**
	 * Create a "FILTER NOT EXISTS" block containing the given element.
	 *
	 * @param element The content of the "FILTER NOT EXISTS"
	 * @return The specified Element
	 */
	public static Element filterNotExists(Element element) {
		return (element == null) ? null : new ElementNotExists(element);
	}

	/**
	 * Create a bind expression.
	 *
	 * @param var The variable to which to bind
	 * @param expr The expression to bind to the variable
	 * @return The specified Element
	 */
	public static Element bind(Var var, Expr expr) {
		return (expr == null) ? null : new ElementBind(var, expr);
	}
}
