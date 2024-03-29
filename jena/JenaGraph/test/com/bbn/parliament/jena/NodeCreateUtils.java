/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This code was "borrowed" from the Jena Core 2.7.2 test code to work around
 * the fact that the jena-core-test JAR file disappeared from binary Jena
 * distributions starting with version 2.7.4.
 */

package com.bbn.parliament.jena;

import java.util.StringTokenizer;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.PrefixMapping;

/** Creating nodes from string specifications. Taken from Jena's test code. */
public class NodeCreateUtils {
	/**
	 * Returns a Node described by the string, primarily for testing purposes.
	 * The string represents a URI, a numeric literal, a string literal, a bnode
	 * label, or a variable.
	 * <ul>
	 * <li>'some text' :: a string literal with that text
	 * <li>'some text'someLanguage:: a string literal with that text and language
	 * <li>'some text'someURI:: a typed literal with that text and datatype
	 * <li>digits :: a literal [OF WHAT TYPE] with that [numeric] value
	 * <li>_XXX :: a bnode with an AnonId built from _XXX
	 * <li>?VVV :: a variable with name VVV
	 * <li>&PPP :: to be done
	 * <li>name:stuff :: the URI; name may be expanded using the Extended map
	 * </ul>
	 *
	 * @param x the string describing the node
	 * @return a node of the appropriate type with the appropriate label
	 */
	public static Node create(String x) {
		return create(PrefixMapping.Extended, x);
	}

	/**
	 * Returns a Node described by the string, primarily for testing purposes.
	 * The string represents a URI, a numeric literal, a string literal, a bnode
	 * label, or a variable.
	 * <ul>
	 * <li>'some text' :: a string literal with that text
	 * <li>'some text'someLanguage:: a string literal with that text and language
	 * <li>'some text'someURI:: a typed literal with that text and datatype
	 * <li>digits :: a literal [OF WHAT TYPE] with that [numeric] value
	 * <li>_XXX :: a bnode with an AnonId built from _XXX
	 * <li>?VVV :: a variable with name VVV
	 * <li>&PPP :: to be done
	 * <li>name:stuff :: the URI; name may be expanded using the Extended map
	 * </ul>
	 *
	 * @param pm the PrefixMapping for translating pre:X strings
	 * @param x the string encoding the node to create
	 * @return a node with the appropriate type and label
	 */
	public static Node create(PrefixMapping pm, String x) {
		if (x == null || x.isEmpty()) {
			throw new JenaException("Node.create does not accept an empty string as argument");
		}
		char first = x.charAt(0);
		if (first == '\'' || first == '\"') {
			return Node.createLiteral(newString(pm, first, x));
		} else if (Character.isDigit(first)) {
			return Node.createLiteral(x, "", XSDDatatype.XSDinteger);
		} else if (first == '_') {
			return Node.createAnon(new AnonId(x));
		} else if (x.equals("??")) {
			return Node.ANY;
		} else if (first == '?') {
			return Node.createVariable(x.substring(1));
		} else if (first == '&') {
			return Node.createURI("q:" + x.substring(1));
		} else {
			int colon = x.indexOf(':');
			String d = pm.getNsPrefixURI("");
			return colon < 0 ? Node.createURI((d == null ? "eh:/" : d) + x) : Node.createURI(pm.expandPrefix(x));
		}
	}

	public static String unEscape(String spelling) {
		if (spelling.indexOf('\\') < 0) {
			return spelling;
		}
		StringBuilder result = new StringBuilder(spelling.length());
		int start = 0;
		while (true) {
			int b = spelling.indexOf('\\', start);
			if (b < 0) {
				break;
			}
			result.append(spelling.substring(start, b));
			result.append(unEscape(spelling.charAt(b + 1)));
			start = b + 2;
		}
		result.append(spelling.substring(start));
		return result.toString();
	}

	public static char unEscape(char ch) {
		return switch (ch) {
			case '\\', '\"', '\'' -> ch;
			case 'n' -> '\n';
			case 's' -> ' ';
			case 't' -> '\t';
			default -> 'Z';
		};
	}

	public static LiteralLabel literal(PrefixMapping pm, String spelling, String langOrType) {
		String content = unEscape(spelling);
		int colon = langOrType.indexOf(':');
		return colon < 0
			? LiteralLabelFactory.create(content, langOrType, false)
			: LiteralLabelFactory.createLiteralLabel(content, "", Node.getType(pm.expandPrefix(langOrType)));
	}

	public static LiteralLabel newString(PrefixMapping pm, char quote, String nodeString) {
		int close = nodeString.lastIndexOf(quote);
		return literal(pm, nodeString.substring(1, close), nodeString.substring(close + 1));
	}

	/** Utility factory as for create(String), but with an explicit PrefixMapping */
	public static Triple createTriple(PrefixMapping pm, String fact) {
		StringTokenizer st = new StringTokenizer(fact);
		Node sub = create(pm, st.nextToken());
		Node pred = create(pm, st.nextToken());
		Node obj = create(pm, st.nextToken());
		return Triple.create(sub, pred, obj);
	}

	/**
	 * Utility factory method for creating a triple based on the content of an "S
	 * P O" string. The S, P, O are processed by Node.create, see which for
	 * details of the supported syntax. This method exists to support test code.
	 * Nodes are interpreted using the Standard prefix mapping.
	 */
	public static Triple createTriple(String fact) {
		return createTriple(PrefixMapping.Standard, fact);
	}
}
