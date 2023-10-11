// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

/**
 * Contains a number of facilities for manipulating SPARQL queries at runtime
 * from Java code. A limitation is that SPARQL-Update is not supported.
 *
 * <p>
 * The primary entry point into these facilities is {@code QueryBuilder}, which
 * performs a number of manipulations on queries:
 * <ul>
 * <li>Retrieving queries from Java resources and setting their prefix mapping
 * <li>Replacing a query variable with a value (IRI or literal)
 * <li>Inserting values into a SPARQL VALUES clause
 * <li>Inserting a union
 * <li>Concatenating queries
 * <li>Appending or prepending elements and element groups of various kinds to a
 * query's WHERE clause
 * <li>Inserting a FILTER or sub-query
 * <li>Inserting a GRAPH clause
 * <li>Changing the variables in the SELECT list
 * </ul>
 *
 * <p>
 * The other classes in the package support {@code QueryBuilder} by building
 * pieces of queries that are inserted through {@code QueryBuilder} methods.
 *
 * @author iemmons
 */
package org.semwebcentral.parliament.sparql_query_builder;
