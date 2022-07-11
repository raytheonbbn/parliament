// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge.util;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Utility methods for N-Triples encoding/decoding.
 *
 * Adapted from Sesame's NTriplesUtil.java
 *
 * @author sallen
 */
public class NTriplesUtil {
	/**
	 * Parses an N-Triples value, creates an object for it using the
	 * supplied Model and returns this object.
	 *
	 * @param nTriplesValue The N-Triples value to parse.
	 * @param model The Model to use for creating the object.
	 * @return An RDFNode object representing the parsed value.
	 * @exception IllegalArgumentException If the supplied value could not be
	 * parsed correctly.
	 */
	public static RDFNode parseValue(String nTriplesValue, Model model)
		throws IllegalArgumentException
	{
		if (nTriplesValue.startsWith("<")) {
			return parseURI(nTriplesValue, model);
		} else if (nTriplesValue.startsWith("_:")) {
			return parseBNode(nTriplesValue, model);
		} else if (nTriplesValue.startsWith("\"")) {
			return parseLiteral(nTriplesValue, model);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples value: " + nTriplesValue);
		}
	}

	/**
	 * Parses an N-Triples resource, creates an object for it using
	 * the supplied Model and returns this object.
	 *
	 * @param nTriplesResource The N-Triples resource to parse.
	 * @param model The Model to use for creating the object.
	 * @return A Resource object representing the parsed resource.
	 * @exception IllegalArgumentException If the supplied resource could not be
	 * parsed correctly.
	 */
	public static Resource parseResource(String nTriplesResource, Model model)
		throws IllegalArgumentException
	{
		if (nTriplesResource.startsWith("<")) {
			return parseURI(nTriplesResource, model);
		} else if (nTriplesResource.startsWith("_:")) {
			return parseBNode(nTriplesResource, model);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples resource: " + nTriplesResource);
		}
	}

	/**
	 * Parses an N-Triples URI, creates an object for it using the
	 * supplied Model and returns this object.
	 *
	 * @param nTriplesURI The N-Triples URI to parse.
	 * @param model The Model to use for creating the object.
	 * @return A Resource object representing the parsed URI.
	 * @exception IllegalArgumentException If the supplied URI could not be
	 * parsed correctly.
	 */
	public static Resource parseURI(String nTriplesURI, Model model)
		throws IllegalArgumentException
	{
		if (nTriplesURI.startsWith("<") && nTriplesURI.endsWith(">")) {
			String uri = nTriplesURI.substring(1, nTriplesURI.length() - 1);
			uri = unescapeString(uri);
			return model.createResource(uri);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples URI: " + nTriplesURI);
		}
	}

	/**
	 * Parses an N-Triples bNode, creates an object for it using the
	 * supplied Model and returns this object.
	 *
	 * @param nTriplesBNode The N-Triples bNode to parse.
	 * @param model The Model to use for creating the object.
	 * @return A Resource object representing the parsed bNode.
	 * @exception IllegalArgumentException If the supplied bNode could not be
	 * parsed correctly.
	 */
	public static Resource parseBNode(String nTriplesBNode, Model model)
		throws IllegalArgumentException
	{
		if (nTriplesBNode.startsWith("_:")) {
			String idStr = nTriplesBNode.substring(2);
			AnonId id = new AnonId(idStr);
			return model.createResource(id);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples URI: " + nTriplesBNode);
		}
	}

	/**
	 * Parses an N-Triples literal, creates an object for it using the
	 * supplied Model and returns this object.
	 *
	 * @param nTriplesLiteral The N-Triples literal to parse.
	 * @param model The Model to use for creating the object.
	 * @return A Literal object representing the parsed literal.
	 * @exception IllegalArgumentException If the supplied literal could not be
	 * parsed correctly.
	 */
	public static Literal parseLiteral(String nTriplesLiteral, Model model)
		throws IllegalArgumentException
	{
		if (nTriplesLiteral.startsWith("\"")) {
			// Find string separation points
			int endLabelIdx = _findEndOfLabel(nTriplesLiteral);

			if (endLabelIdx != -1) {
				int startLangIdx = nTriplesLiteral.indexOf("@", endLabelIdx);
				int startDtIdx = nTriplesLiteral.indexOf("^^", endLabelIdx);

				if (startLangIdx != -1 && startDtIdx != -1) {
					throw new IllegalArgumentException("Literals can not have both a language and a datatype");
				}

				// Get label
				String label = nTriplesLiteral.substring(1, endLabelIdx);
				label = unescapeString(label);

				if (startLangIdx != -1) {
					// Get language
					String language = nTriplesLiteral.substring(startLangIdx + 1);
					return model.createLiteral(label, language);
				} else if (startDtIdx != -1) {
					// Get datatype
					String datatype = nTriplesLiteral.substring(startDtIdx + 2);
					return model.createTypedLiteral(label, datatype);
				} else {
					return model.createLiteral(label);
				}
			}
		}

		throw new IllegalArgumentException(
			"Not a legal N-Triples literal: " + nTriplesLiteral);
	}

	/**
	 * Finds the end of the label in a literal string. This method
	 * takes into account that characters can be escaped using
	 * backslashes.
	 *
	 * @return The index of the double quote ending the label, or
	 * <tt>-1</tt> if it could not be found.
	 */
	private static int _findEndOfLabel(String nTriplesLiteral) {
		// First character of literal is guaranteed to be a double
		// quote, start search at second character.

		boolean previousWasBackslash = false;

		for (int i = 1; i < nTriplesLiteral.length(); i++) {
			char c = nTriplesLiteral.charAt(i);

			if (c == '"' && !previousWasBackslash) {
				return i;
			} else if (c == '\\' && !previousWasBackslash) {
				// start of escape
				previousWasBackslash = true;
			} else if (previousWasBackslash) {
				// c was escaped
				previousWasBackslash = false;
			}
		}

		return -1;
	}

	/** Creates an N-Triples string for the supplied value. */
	public static String toNTriplesString(RDFNode value) {
		if (value.isLiteral()) {
			return toNTriplesString((Literal)value);
		} else {
			return toNTriplesString((Resource)value);
		}
	}

	/** Creates an N-Triples string for the supplied resource. */
	public static String toNTriplesString(Resource resource) {
		if (resource.isAnon()) {
			return toNTriplesStringBlankNode(resource);
		} else {
			return toNTriplesStringResource(resource);
		}
	}

	/** Creates an N-Triples string for the supplied URI. */
	public static String toNTriplesStringResource(Resource uri) {
		return toNTriplesStringUri(uri.getURI());
	}

	/** Creates an N-Triples string for the supplied URI. */
	public static String toNTriplesStringUri(String uri) {
		return "<" + escapeString(uri) + ">";
	}

	/** Creates an N-Triples string for the supplied bNode. */
	public static String toNTriplesStringBlankNode(Resource bNode) {
		return "_:" + bNode.getId();
	}

	/** Creates an N-Triples string for the supplied literal. */
	public static String toNTriplesString(Literal lit) {
		// Do some character escaping on the label:
		StringBuilder result = new StringBuilder(128);
		result.append("\"");
		result.append(escapeString(lit.getString()));
		result.append("\"");

		if (lit.getDatatype() != null) {
			// Append the literal's datatype
			result.append("^^");
			result.append(toNTriplesStringUri(lit.getDatatypeURI()));
		} else if ((lit.getLanguage() != null) && (!"".equals(lit.getLanguage()))) {
			// Append the literal's language
			result.append("@");
			result.append(lit.getLanguage());
		}

		return result.toString();
	}

	/**
	 * Checks whether the supplied character is a letter or number
	 * according to the N-Triples specification.
	 * @see #isLetter
	 * @see #isNumber
	 */
	public static boolean isLetterOrNumber(int c) {
		return isLetter(c) || isNumber(c);
	}

	/**
	 * Checks whether the supplied character is a letter according to
	 * the N-Triples specification. N-Triples letters are A - Z and a - z.
	 */
	public static boolean isLetter(int c) {
		return (c >= 65 && c <= 90) || // A - Z
			(c >= 97 && c <= 122);  // a - z
	}

	/**
	 * Checks whether the supplied character is a number according to
	 * the N-Triples specification. N-Triples numbers are 0 - 9.
	 */
	public static boolean isNumber(int c) {
		return (c >= 48 && c <= 57); // 0 - 9
	}

	/**
	 * Escapes a Unicode string to an all-ASCII character sequence. Any special
	 * characters are escaped using backslashes (<tt>"</tt> becomes <tt>\"</tt>,
	 * etc.), and non-ascii/non-printable characters are escaped using Unicode
	 * escapes (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>).
	 */
	public static String escapeString(String label) {
		int labelLength = label.length();
		StringBuilder result = new StringBuilder(2 * labelLength);

		for (int i = 0; i < labelLength; i++) {
			char c = label.charAt(i);
			int cInt = c;

			if (c == '\\') {
				result.append("\\\\");
			} else if (c == '"') {
				result.append("\\\"");
			} else if (c == '\n') {
				result.append("\\n");
			} else if (c == '\r') {
				result.append("\\r");
			} else if (c == '\t') {
				result.append("\\t");
			} else if (c == '\'') {
				result.append("\\\'");
			} else if (c == '\b') {
				result.append("\\\b");
			} else if (c == '\f') {
				result.append("\\\f");
			} else if (
				cInt >= 0x0 && cInt <= 0x8 ||
				cInt == 0xB || cInt == 0xC ||
				cInt >= 0xE && cInt <= 0x1F ||
				cInt >= 0x7F && cInt <= 0xFFFF)
			{
				result.append("\\u");
				result.append(toHexString(cInt, 4));
			} else if (cInt >= 0x10000 && cInt <= 0x10FFFF) {
				result.append("\\U");
				result.append(toHexString(cInt, 8));
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	/**
	 * Unescapes an escaped Unicode string. Any Unicode sequences
	 * (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>) are restored to the
	 * value indicated by the hexadecimal argument and any backslash-escapes
	 * (<tt>\"</tt>, <tt>\\</tt>, etc.) are decoded to their original form.
	 *
	 * @param s An escaped Unicode string.
	 * @return The unescaped string.
	 * @exception IllegalArgumentException If the supplied string is not a
	 * correctly escaped N-Triples string.
	 */
	public static String unescapeString(String s) {
		int backSlashIdx = s.indexOf('\\');

		if (backSlashIdx == -1) {
			// No escaped characters found
			return s;
		}

		int startIdx = 0;
		int sLength = s.length();
		StringBuilder buf = new StringBuilder(sLength);

		while (backSlashIdx != -1) {
			buf.append(s.substring(startIdx, backSlashIdx));

			if (backSlashIdx + 1 >= sLength) {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			char c = s.charAt(backSlashIdx + 1);

			if (c == 't') {
				buf.append('\t');
				startIdx = backSlashIdx + 2;
			} else if (c == 'r') {
				buf.append('\r');
				startIdx = backSlashIdx + 2;
			} else if (c == 'n') {
				buf.append('\n');
				startIdx = backSlashIdx + 2;
			} else if (c == '"') {
				buf.append('"');
				startIdx = backSlashIdx + 2;
			} else if (c == '\'') {
				buf.append('\'');
				startIdx = backSlashIdx + 2;
			} else if (c == '\\') {
				buf.append('\\');
				startIdx = backSlashIdx + 2;
			} else if (c == 'f') {
				buf.append("\f");
				startIdx = backSlashIdx + 2;
			} else if (c == 'b') {
				buf.append("\b");
				startIdx = backSlashIdx + 2;
			} else if (c == 'u') {
				// \\uxxxx
				if (backSlashIdx + 5 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

				try {
					c = (char)Integer.parseInt(xx, 16);
					buf.append(c);

					startIdx = backSlashIdx + 6;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
				}
			} else if (c == 'U') {
				// \\Uxxxxxxxx
				if (backSlashIdx + 9 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

				try {
					c = (char)Integer.parseInt(xx, 16);
					buf.append(c);

					startIdx = backSlashIdx + 10;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
				}
			} else {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		buf.append( s.substring(startIdx) );

		return buf.toString();
	}

	/**
	 * Converts a decimal value to a hexadecimal string represention
	 * of the specified length.
	 *
	 * @param decimal A decimal value.
	 * @param stringLength The length of the resulting string.
	 */
	public static String toHexString(int decimal, int stringLength) {
		StringBuilder result = new StringBuilder(stringLength);

		String hexVal = Integer.toHexString(decimal).toUpperCase();

		// insert zeros if hexVal has less than stringLength characters:
		int nofZeros = stringLength - hexVal.length();
		for (int i = 0; i < nofZeros; i++) {
			result.append('0');
		}

		result.append(hexVal);

		return result.toString();
	}
}
