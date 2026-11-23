// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.bbn.parliament.kb_graph.KbGraphStore;

/** @author sallen */
public class ExplorerUtil {
	private static Map<Resource, String> labelMap;

	static {
		labelMap = new HashMap<>(64);
		labelMap.put(RDF.type, "type");
		labelMap.put(RDF.Property, "Property");
		labelMap.put(RDF.Statement, "Statement");
		// The RDF class is missing XMLLiteral for some reason. Lets define it ourselves.
		labelMap.put(ResourceFactory.createResource(RDF.getURI() + "XMLLiteral"), "XMLLiteral");
		labelMap.put(RDF.subject, "subject");
		labelMap.put(RDF.predicate, "predicate");
		labelMap.put(RDF.object, "object");
		labelMap.put(RDF.first, "first");
		labelMap.put(RDF.rest, "rest");
		labelMap.put(RDF.Alt, "Alt");
		labelMap.put(RDF.Bag, "Bag");
		labelMap.put(RDF.Seq, "Seq");
		labelMap.put(RDF.List, "List");
		labelMap.put(RDF.nil, "nil");

		labelMap.put(RDFS.Class, "Class");
		labelMap.put(RDFS.Resource, "Resource");
		labelMap.put(RDFS.Literal, "Literal");
		labelMap.put(RDFS.subClassOf, "subClassOf");
		labelMap.put(RDFS.subPropertyOf, "subPropertyOf");
		labelMap.put(RDFS.domain, "domain");
		labelMap.put(RDFS.range, "range");
		labelMap.put(RDFS.comment, "comment");
		labelMap.put(RDFS.label, "label");
		labelMap.put(RDFS.isDefinedBy, "isDefinedBy");
		labelMap.put(RDFS.seeAlso, "seeAlso");
		labelMap.put(RDFS.member, "member");
		labelMap.put(RDFS.Datatype, "Datatype");
		labelMap.put(RDFS.Container, "Container");
		labelMap.put(RDFS.ContainerMembershipProperty, "ContainerMemberShipProperty");

		// add default graph resource
		labelMap.put(
			ResourceFactory.createResource(KbGraphStore.DEFAULT_GRAPH_NODE.getURI()),
			KbGraphStore.DEFAULT_GRAPH_BASENAME);
	}

	public static class BlankNodeLabeler {
		private Map<Resource, String> _blankNodeLabelMap;
		private int _blankNodeSeq;

		public BlankNodeLabeler() {
			_blankNodeLabelMap = new HashMap<>();
			_blankNodeSeq = 0;
		}

		public void reset() {
			_blankNodeLabelMap.clear();
			_blankNodeSeq = 0;
		}

		public String getLabel(Resource bnode) {
			String toReturn = _blankNodeLabelMap.get(bnode);
			if (null == toReturn) {
				toReturn = next();
				_blankNodeLabelMap.put(bnode, toReturn);
			}
			return toReturn;
		}

		private String next() {
			return "_:b" + _blankNodeSeq++;
		}
	}

	public static String getDisplayString(RDFNode value,
		BlankNodeLabeler bnodeLabeler) {
		String toReturn = value.toString();

		if (value.isResource()) {
			Resource resource = (Resource) value;
			if (resource.isAnon()) {
				return bnodeLabeler.getLabel(resource);
			}
			return resource.getURI();
		} else if (value.isLiteral()) {
			Literal lit = (Literal) value;

			StringBuilder buf = new StringBuilder(32);
			buf.append("\"");
			buf.append(lit.getString());
			buf.append("\"");

			if ((lit.getLanguage() != null) && (!"".equals(lit.getLanguage()))) {
				buf.append("@").append(lit.getLanguage());
			} else if (lit.getDatatype() != null) {
				buf.append("^^").append(lit.getDatatype().getURI());
			}

			return buf.toString();
		}

		return toReturn;
	}

	public static String getLabelForResource(Resource resource, Model model) {
		// Check standard labels
		String label = labelMap.get(resource);

		if (label == null) {
			// Get label from repository
			StmtIterator labelIter = model.listStatements(resource, RDFS.label,
				(RDFNode) null);

			while (labelIter.hasNext()) {
				// Value labelObj = labelIter.next().getObject();
				RDFNode labelObj = labelIter.nextStatement().getObject();
				if (labelObj instanceof Literal labelLit) {
					label = labelLit.getString();
					break;
				}
			}

			labelIter.close();
		}

		return label;
	}

	/*
	public static String getQueryString(RDFNode value, boolean useLabels) {
		Map<String, String> params = new HashMap<>(4);

		params.put("value", NTriplesUtil.toNTriplesString(value));
		if (useLabels) {
			params.put("useLabels", "yes");
		}

		return buildQueryString(params);
	}
	*/

	/**
	 * Escapes any special characters in the supplied text so that it can be
	 * included as character data in an XML document. The characters that are
	 * escaped are <tt>&amp;</tt>, <tt>&lt;</tt>, <tt>&gt;</tt> and <tt>carriage
	 * return (\r)</tt>.
	 */
	public static String escapeCharacterData(String text) {
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("<", "&lt;");
		text = text.replaceAll(">", "&gt;");
		text = text.replaceAll("\r", "&#xD;");
		return text;
	}

	/**
	 * Escapes any special characters in the supplied value so that it can be
	 * used as an double-quoted attribute value in an XML document. The
	 * characters that are escaped are <tt>&amp;</tt>, <tt>&lt;</tt>,
	 * <tt>&gt;</tt>, <tt>tab (\t)</tt>, <tt>carriage return (\r)</tt>,
	 * <tt>line feed (\n)</tt> and <tt>"</tt>.
	 */
	public static String escapeDoubleQuotedAttValue(String value) {
		value = _escapeAttValue(value);
		value = value.replaceAll("\"", "&quot;");
		return value;
	}

	/**
	 * Escapes any special characters in the supplied value so that it can be
	 * used as an single-quoted attribute value in an XML document. The
	 * characters that are escaped are <tt>&amp;</tt>, <tt>&lt;</tt>,
	 * <tt>&gt;</tt>, <tt>tab (\t)</tt>, <tt>carriage return (\r)</tt>,
	 * <tt>line feed (\n)</tt> and <tt>'</tt>.
	 */
	public static String escapeSingleQuotedAttValue(String value) {
		value = _escapeAttValue(value);
		value = value.replaceAll("'", "&apos;");
		return value;
	}

	private static String _escapeAttValue(String value) {
		value = value.replaceAll("&", "&amp;");
		value = value.replaceAll("<", "&lt;");
		value = value.replaceAll(">", "&gt;");
		value = value.replaceAll("\t", "&#x9;");
		value = value.replaceAll("\n", "&#xA;");
		value = value.replaceAll("\r", "&#xD;");
		return value;
	}

	/**
	 * Builds a query string from the provided key-value-pairs. All spaces are
	 * substituted by '+' characters, and all non US-ASCII characters are escaped
	 * to hexadecimal notation (%xx).
	 */


	/*
	public static String buildQueryString(Map<String, String> keyValuePairs) {
		StringBuilder result = new StringBuilder(20 * keyValuePairs.size());

		boolean isFirstIteration = true;
		for (Map.Entry<String, String> keyValuePair : keyValuePairs.entrySet()) {
			if (!isFirstIteration) {
				result.append('&');
			}

			String key = keyValuePair.getKey();
			String value = keyValuePair.getValue();

			// Escape both key and value and combine them with an '='
			result.append(Convert.encWWWForm(key));
			result.append('=');
			result.append(Convert.encWWWForm(value));

			isFirstIteration = false;
		}

		return result.toString();
	}
	*/

	public static StmtIterator getEmptyStmtIterator() {
		StmtIterator toReturn = new StmtIterator() {
			@Override
			public Statement nextStatement() throws NoSuchElementException {
				return null;
			}

			@Override
			public <X extends Statement> ExtendedIterator<Statement> andThen(Iterator<X> other) {
				return null;
			}

//			@Override
//			public ExtendedIterator<Statement> filterDrop(Filter<Statement> f) {
//				return null;
//			}
//
//			@Override
//			public ExtendedIterator<Statement> filterKeep(Filter<Statement> f) {
//				return null;
//			}

			@Override
			public <U> ExtendedIterator<U> mapWith(Function<Statement, U> map) {
				return null;
			}

			@Override
			public Statement removeNext() {
				return null;
			}

			@Override
			public List<Statement> toList() {
				return null;
			}

			@Override
			public Set<Statement> toSet() {
				return null;
			}

			@Override
			public void close() {
			}

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Statement next() {
				return null;
			}

			@Override
			public void remove() {
			}

			@Override
			public ExtendedIterator<Statement> filterKeep(Predicate<Statement> f) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ExtendedIterator<Statement> filterDrop(Predicate<Statement> f) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		return toReturn;
	}
}
