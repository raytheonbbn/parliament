// Copyright (c) 2019, 2020 Raytheon BBN Technologies Corp.

package org.semwebcentral.parliament.sparql_query_builder;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.semwebcentral.parliament.util.JavaResource;

/**
 * Assembles and updates parameterized query snippets. Since the internal
 * implementation relies on Jena's {@link Query}, this only supports
 * manipulating SELECT, ASK, DESCRIBE, and CONSTRUCT type queries, i.e.
 * SPARQL-Update is not supported.
 *
 * @author mallen
 */
public class QueryBuilder {
	private static final TypeMapper TYPE_MAPPER  = new TypeMapper();
	private static final Pattern URI_PATTERN = Pattern.compile(
		"^(?:(?:http)|(?:https)|(?:tag)|(?:urn)):.*$");

	private static PrefixMapping defaultPrefixMapping = null;

	private transient String originalQuery;
	private transient PrefixMapping prefixMapping;
	private transient Query query;

	/**
	 * Adds the given prefix mappings to QueryBuilder's default prefix mappings.
	 *
	 * @param additionalPrefixMapping The prefix mappings to add
	 */
	public static void addToDefaultPrefixMapping(PrefixMapping additionalPrefixMapping) {
		if (defaultPrefixMapping == null) {
			defaultPrefixMapping = additionalPrefixMapping;
		} else {
			defaultPrefixMapping.setNsPrefixes(additionalPrefixMapping);
		}
	}

	/**
	 * Creates a QueryBuilder from the given query string, using the default
	 * prefix mappings.
	 *
	 * @param queryStr The query string used as the basis for query building
	 * @return A new QueryBuilder
	 */
	public static QueryBuilder fromString(String queryStr) {
		return new QueryBuilder(queryStr);
	}

	/**
	 * Creates a QueryBuilder from the given query string, using the given
	 * prefix mappings.
	 *
	 * @param queryStr The query string used as the basis for query building
	 * @param prefixMapping The prefix mappings to use for this query
	 * @return A new QueryBuilder
	 */
	public static QueryBuilder fromString(String queryStr, PrefixMapping prefixMapping) {
		return new QueryBuilder(queryStr, prefixMapping);
	}

	/**
	 * Creates a QueryBuilder from a resource, using the default prefix
	 * mappings.
	 *
	 * @param rsrcName The name of the resource to load
	 * @param cls A class indicating the package in which to find the resource
	 * @return A new QueryBuilder
	 */
	public static QueryBuilder fromRsrc(String rsrcName, Class<?> cls) {
		return new QueryBuilder(JavaResource.getAsString(rsrcName, cls));
	}

	/**
	 * Creates a QueryBuilder from a resource, using the given prefix mappings.
	 *
	 * @param rsrcName The name of the resource to load
	 * @param cls A class indicating the package in which to find the resource
	 * @param prefixMapping The prefix mappings to use for this query
	 * @return A new QueryBuilder
	 */
	public static QueryBuilder fromRsrc(String rsrcName, Class<?> cls, PrefixMapping prefixMapping) {
		return new QueryBuilder(JavaResource.getAsString(rsrcName, cls), prefixMapping);
	}

	/**
	 * Creates a QueryBuilder from a resource, using the default prefix
	 * mappings.
	 *
	 * @param rsrcPath The path of the resource to load
	 * @return A new QueryBuilder
	 */
	public static QueryBuilder fromRsrc(String rsrcPath) {
		return new QueryBuilder(JavaResource.getAsString(rsrcPath));
	}

	/**
	 * Creates a QueryBuilder from a resource, using the given prefix mappings.
	 *
	 * @param rsrcPath The path of the resource to load
	 * @param prefixMapping The prefix mappings to use for this query
	 * @return A new QueryBuilder
	 */
	public static QueryBuilder fromRsrc(String rsrcPath, PrefixMapping prefixMapping) {
		return new QueryBuilder(JavaResource.getAsString(rsrcPath), prefixMapping);
	}

	private QueryBuilder(String queryStr) {
		reset(queryStr);
	}

	private QueryBuilder(String queryStr, PrefixMapping pm) {
		reset(queryStr, pm);
	}

	/**
	 * Resets the QueryBuilder to the original query it was given, i.e., to the
	 * same state as when it was originally created.
	 *
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder reset() {
		ParameterizedSparqlString pss = (prefixMapping == null)
			? new ParameterizedSparqlString(originalQuery)
			: new ParameterizedSparqlString(originalQuery, prefixMapping);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Resets the QueryBuilder with the given query string and the default
	 * prefix mapping.
	 *
	 * @param queryStr The new query string
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder reset(String queryStr) {
		originalQuery = queryStr;
		prefixMapping = defaultPrefixMapping;
		return reset();
	}

	/**
	 * Resets the QueryBuilder with the given query string and prefix mapping.
	 *
	 * @param queryStr The new query string
	 * @param pm The new prefix mapping
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder reset(String queryStr, PrefixMapping pm) {
		originalQuery = queryStr;
		prefixMapping = pm;
		return reset();
	}

	/**
	 * Gets the original query text supplied at creation.
	 *
	 * @return The original query text supplied at creation
	 */
	public String getOriginalQueryText() {
		return originalQuery;
	}

	/**
	 * Gets the assembled query.
	 *
	 * @return The assembled query
	 */
	public Query asQuery() {
		return query;
	}

	/** Gets the assembled query in string form. */
	@Override
	public String toString() {
		return query.toString();
	}

	/**
	 * Replace a query variable with an RDF node. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value RDF node value
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setArg(String var, RDFNode value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setParam(var, value);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with an RDF node. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value RDF node value
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setArg(String var, Node value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setParam(var, value);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with a URI value. If the query is a select query
	 * and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value URI value, can be prefixed or long-form
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setIriArg(String var, String value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setIri(var, expandPrefix(value));
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with a string literal. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value String literal value
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setArg(String var, String value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setLiteral(var, value);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with a language string literal. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value String literal value
	 * @param lang The language of the literal
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setLangArg(String var, String value, String lang) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setLiteral(var, value, lang);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with a Boolean literal. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value Boolean literal value
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setArg(String var, boolean value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setLiteral(var, value);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with an integer literal. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value Integer literal value
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setArg(String var, long value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setLiteral(var, value);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with a date-time literal. If the query is a select
	 * query and the variable appears in the select list, it is removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value Date-time literal value
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setArg(String var, Calendar value) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		pss.setLiteral(var, value);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Replace a query variable with a literal of the specified datatype. If the
	 * query is a select query and the variable appears in the select list, it is
	 * removed.
	 *
	 * @param var Name of the variable to replace
	 * @param value Literal value
	 * @param xsdType The datatype URI of the literal
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setTypedArg(String var, String value, String xsdType) {
		removeResultVar(var);
		ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
		RDFDatatype dt = xsdDataTypeToJenaDataType(xsdType);
		pss.setLiteral(var, value, dt);
		query = pss.asQuery();
		return this;
	}

	/**
	 * Reset the values in a single-variable values clause.
	 *
	 * @param var The variable name in the values clause
	 * @param markerValue The first value in the clause prior to replacement.
	 *        Used to be sure the correct values clause is targeted.
	 * @param newValues A list of values to substitute into the values clause.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setValues(String var, String markerValue, Collection<RDFNode> newValues) {
		ElementVisitor visitor = new ValuesClauseSetter(var, markerValue, newValues);
		query.getQueryPattern().visit(visitor);
		return this;
	}

	/**
	 * Reset the values in a single-variable values clause.
	 *
	 * @param var The variable name in the values clause
	 * @param markerValue The first value in the clause prior to replacement.
	 *        Used to be sure the correct values clause is targeted.
	 * @param newValues A list of values to substitute into the values clause.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setValues(String var, String markerValue, RDFNode... newValues) {
		ElementVisitor visitor = new ValuesClauseSetter(var, markerValue, newValues);
		query.getQueryPattern().visit(visitor);
		return this;
	}

	/**
	 * Reset the values in a multi-variable values clause.
	 *
	 * @param variables The variable names in the values clause
	 * @param markerValue The first value in the first tuple in the clause prior
	 *        to replacement. Used to be sure the correct values clause is
	 *        targeted.
	 * @param newValues A list of value tuples to substitute into the values
	 *        clause.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setValues(List<String> variables, String markerValue, RDFNode[]... newValues) {
		ElementVisitor visitor = new ValuesClauseSetter(variables, markerValue, newValues);
		query.getQueryPattern().visit(visitor);
		return this;
	}

	/**
	 * Reset the values in a multi-variable values clause.
	 *
	 * @param variables The variable names in the values clause
	 * @param markerValue The first value in the first tuple in the clause prior
	 *        to replacement. Used to be sure the correct values clause is
	 *        targeted.
	 * @param newValues A list of value tuples to substitute into the values
	 *        clause.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder setValues(List<String> variables, String markerValue, Collection<RDFNode[]> newValues) {
		ElementVisitor visitor = new ValuesClauseSetter(variables, markerValue, newValues);
		query.getQueryPattern().visit(visitor);
		return this;
	}

	/**
	 * Reset the values in a multi-variable values clause.
	 *
	 * @param variables The variable names in the values clause
	 * @param markerValue The first value in the first tuple in the clause prior
	 *        to replacement. Used to be sure the correct values clause is
	 *        targeted.
	 * @param newValues A list of value tuples to substitute into the values
	 *        clause.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	@SafeVarargs
	public final QueryBuilder setValues(List<String> variables, String markerValue, Collection<RDFNode>... newValues) {
		ElementVisitor visitor = new ValuesClauseSetter(variables, markerValue, newValues);
		query.getQueryPattern().visit(visitor);
		return this;
	}

	/**
	 * UNION the entire body of the WHERE clause with another query pattern,
	 * resulting in a query of the form:
	 *
	 * <pre>
	 * SELECT ... WHERE {
	 *     { #existing body }
	 *     UNION
	 *     { #new body }
	 * }
	 * </pre>
	 *
	 * @param otherQuery Another query
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder union(Query otherQuery) {
		if (otherQuery == null) {
			return this;
		}
		ElementUnion union = new ElementUnion();
		union.addElement(query.getQueryPattern());
		union.addElement(otherQuery.getQueryPattern());
		query.setQueryPattern(union);
		return this;
	}

	/**
	 * Append the query pattern from another query to the body of this query, and
	 * add result variables to this SELECT clause.
	 *
	 * @param otherQuery The query whose body is to be added to this one
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder addPatternToBody(Query otherQuery) {
		if (otherQuery == null) {
			return this;
		}
		// groups of elements inside each query body
		ElementGroup otherBody = (ElementGroup) otherQuery.getQueryPattern();
		addElementsToBody(otherBody);

		// Update SELECT clause
		for (String var : otherQuery.getResultVars()) {
			query.addResultVar(var);
		}

		return this;
	}

	/**
	 * Prepend the given query element to the body of this query.
	 *
	 * @param el The element to prepend
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder prependElementToBody(Element el) {
		if (el == null) {
			return this;
		}
		ElementGroup oldBody = (ElementGroup) query.getQueryPattern();
		ElementGroup newBody = new ElementGroup();
		newBody.getElements().add(el);
		newBody.getElements().addAll(oldBody.getElements());
		query.setQueryPattern(newBody);
		return this;
	}

	/**
	 * Append the given query element to the body of this query.
	 *
	 * @param el The element to append
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder appendElementToBody(Element el) {
		if (el == null) {
			return this;
		}
		ElementGroup body = (ElementGroup) query.getQueryPattern();
		body.getElements().add(el);
		query.setQueryPattern(body);
		return this;
	}

	/**
	 * Append the elements in the given query element group to the body of this
	 * query.
	 *
	 * @param newElements The group of elements to append
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder addElementsToBody(ElementGroup newElements) {
		if (newElements == null) {
			return this;
		}
		ElementGroup body = (ElementGroup) query.getQueryPattern();
		body.getElements().addAll(newElements.getElements());
		query.setQueryPattern(body);
		return this;
	}

	/**
	 * Add a filter containing an arbitrary expression.
	 *
	 * @param expr The filter expression to add
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder addFilter(Expr expr) {
		return (expr == null) ? this : appendElementToBody(new ElementFilter(expr));
	}

	/**
	 * Prepend a sub-query to this query. Take care to match projected variables from
	 * the sub-query with variables which are matched in the outer query. Should
	 * work with SELECT, CONSTRUCT, or ASK type queries but only SELECT has been
	 * tested.
	 *
	 * @param subQuery a Jena query to add as a sub-query.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder prependSubQuery(Query subQuery) {
		if (subQuery == null) {
			return this;
		}
		// Use an empty prefix mapping so the sub-query doesn't have prefix definitions:
		subQuery.setPrefixMapping(PrefixMapping.Factory.create());
		ElementSubQuery subQueryEl = new ElementSubQuery(subQuery);
		return prependElementToBody(subQueryEl);
	}

	/**
	 * Prepend a sub-query to this query. Take care to match projected variables from
	 * the sub-query with variables which are matched in the outer query. Should
	 * work with SELECT, CONSTRUCT, or ASK type queries but only SELECT has been
	 * tested.
	 *
	 * @param other Another QueryBuilder to add as a sub-query.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder prependSubQuery(QueryBuilder other) {
		return (other == null) ? this : prependSubQuery(other.asQuery());
	}

	/**
	 * Append a sub-query to this query. Take care to match projected variables from
	 * the sub-query with variables which are matched in the outer query. Should
	 * work with SELECT, CONSTRUCT, or ASK type queries but only SELECT has been
	 * tested.
	 *
	 * @param subQuery a Jena query to add as a sub-query.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder appendSubQuery(Query subQuery) {
		if (subQuery == null) {
			return this;
		}
		// Use an empty prefix mapping so the sub-query doesn't have prefix definitions:
		subQuery.setPrefixMapping(PrefixMapping.Factory.create());
		ElementSubQuery subQueryEl = new ElementSubQuery(subQuery);
		return appendElementToBody(subQueryEl);
	}

	/**
	 * Append a sub-query to this query. Take care to match projected variables from
	 * the sub-query with variables which are matched in the outer query. Should
	 * work with SELECT, CONSTRUCT, or ASK type queries but only SELECT has been
	 * tested.
	 *
	 * @param other Another QueryBuilder to add as a sub-query.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder appendSubQuery(QueryBuilder other) {
		return (other == null) ? this : appendSubQuery(other.asQuery());
	}

	/**
	 * Wrap the entire WHERE clause in a <code>GRAPH [uri] { ... }</code> clause.
	 *
	 * Does not check for existing named graph elements, so be sure that the
	 * query pattern is not ALREADY scoped to a graph.
	 *
	 * @param graphUri Can be prefixed or long form.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder scopeToGraph(String graphUri) {
		if (graphUri == null) {
			return this;
		}
		ElementGroup existingBody = (ElementGroup) query.getQueryPattern();
		Node graph = NodeFactory.createURI(expandPrefix(graphUri));
		ElementNamedGraph ng = new ElementNamedGraph(graph, existingBody);
		query.setQueryPattern(ng);
		return this;
	}

	/**
	 * Wrap the entire body of the SELECT portion of the query in a
	 * <code>GRAPH [var] { ... }</code> clause.
	 *
	 * Does not check for existing named graph elements, so be sure that the
	 * query pattern is not ALREADY scoped to a graph.
	 *
	 * @param varName The name of the variable in the GRAPH clause
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder scopeToGraphVariable(String varName) {
		if (varName == null) {
			return this;
		}
		ElementGroup existingBody = (ElementGroup) query.getQueryPattern();
		Node graph = Var.alloc(varName);
		ElementNamedGraph ng = new ElementNamedGraph(graph, existingBody);
		query.setQueryPattern(ng);
		return this;
	}

	/**
	 * Add a variable to the SELECT result clause.
	 *
	 * @param var Name of the variable to add.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder addResultVar(String var) {
		if (query.isSelectType()) {
			query.addResultVar(Var.alloc(var));
		}
		return this;
	}

	/**
	 * Remove a variable from the SELECT clause.
	 *
	 * @param varToRemove Name of the variable to remove.
	 * @return This QueryBuilder instance, to enable method chaining.
	 */
	public QueryBuilder removeResultVar(String varToRemove) {
		if (query.isSelectType()) {
			Iterator<Var> varIter = query.getProjectVars().iterator();
			while (varIter.hasNext()) {
				Var var = varIter.next();
				if (var.getVarName().equals(varToRemove)) {
					varIter.remove();
					// don't return; check all the bindings
				}
			}
		}
		return this;
	}

	/**
	 * Translates a qualified name to a URI using the internal prefix mapping
	 *
	 * @param qName The qualified name to translate
	 * @return If the argument is null, empty, or already represents a URI, it is
	 *         returned unchanged. Otherwise, returns the URI corresponding to the
	 *         given qualified name.
	 */
	public String expandPrefix(String qName) {
		return (qName == null || qName.isEmpty() || URI_PATTERN.matcher(qName).matches())
			? qName : query.expandPrefixedName(qName);
	}

	/**
	 * Translates a URI to a qualified name using the internal prefix mapping
	 *
	 * @param uri The URI to translate
	 * @return If the argument is null, null is returned. Otherwise, returns the
	 *         qualified name corresponding to the given URI.
	 */
	public String qnameFor(Resource uri) {
		return (uri == null)
			? null : qnameFor(uri.getURI());
	}

	/**
	 * Translates a URI to a qualified name using the internal prefix mapping
	 *
	 * @param uri The URI to translate
	 * @return If the argument is null, empty, or does not represent a URI, it is
	 *         returned unchanged. Otherwise, returns the qualified name
	 *         corresponding to the given URI.
	 */
	public String qnameFor(String uri) {
		return (uri == null || uri.isEmpty() || !URI_PATTERN.matcher(uri).matches())
			? uri : query.shortForm(uri);
	}

	private RDFDatatype xsdDataTypeToJenaDataType(String xsdType) {
		return TYPE_MAPPER.getSafeTypeByName(expandPrefix(xsdType));
	}
}
