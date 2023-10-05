package org.semwebcentral.parliament.util;

import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;

/**
 * A utility class for converting between IRIs and Q-names (qualified names).
 * All conversions attempt to do the correct thing when given bad inputs, i.e.,
 * null, empty, and already-converted inputs are returned as-is. This class may
 * be used two different ways. One set of methods are static, with the prefix
 * mapping passed as a parameter. The other set of methods are instance methods,
 * and use a prefix mapping passed to the constructor.
 */
public class QName {
	private static final Pattern IRI_PATTERN = Pattern.compile(
		"^(?:(?:http)|(?:https)|(?:tag)|(?:urn)):.*$");

	private final PrefixMapping prefixMapping;

	/**
	 * Constructs a QName instance with its internal prefix mapping as given.
	 *
	 * @param pm The prefix mapping to use for future conversions
	 */
	public QName(PrefixMapping pm) {
		prefixMapping = pm;
	}

	/**
	 * Converts a Q-name to a full IRI using the internal prefix mapping.
	 *
	 * @param qName The Q-name to convert
	 * @return The full IRI according to the internal prefix mapping. If the Q-name
	 *         is null, empty, or appears to be an already-expanded IRI, then it is
	 *         returned unchanged.
	 */
	public String expandPrefix(String qName) {
		return expandPrefix(qName, prefixMapping);
	}

	/**
	 * Converts a Jena Resource to a Q-name using the internal prefix mapping.
	 *
	 * @param iri The resource to convert
	 * @return The Q-name according to the internal prefix mapping. If the resource
	 *         is null, then null is returned.
	 */
	public String qnameFor(Resource iri) {
		return qnameFor(iri, prefixMapping);
	}

	/**
	 * Converts an IRI to a Q-name using the internal prefix mapping.
	 *
	 * @param iri The IRI to convert
	 * @return The Q-name according to the internal prefix mapping. If the IRI is
	 *         null, empty, or appears to be a Q-name already, then it is returned
	 *         unchanged.
	 */
	public String qnameFor(String iri) {
		return qnameFor(iri, prefixMapping);
	}

	/**
	 * Converts a Q-name to a full IRI using the supplied prefix mapping.
	 *
	 * @param qName The Q-name to convert
	 * @param pm    The prefix mapping to use for the conversion
	 * @return The full IRI according to the supplied prefix mapping. If the Q-name
	 *         is null, empty, or appears to be an already-expanded IRI, then it is
	 *         returned unchanged.
	 */
	public static String expandPrefix(String qName, PrefixMapping pm) {
		return (qName == null || qName.isEmpty() || IRI_PATTERN.matcher(qName).matches())
			? qName : pm.expandPrefix(qName);
	}

	/**
	 * Converts a Jena Resource to a Q-name using the supplied prefix mapping.
	 *
	 * @param iri The resource to convert
	 * @param pm  The prefix mapping to use for the conversion
	 * @return The Q-name according to the supplied prefix mapping. If the resource
	 *         is null, then null is returned.
	 */
	public static String qnameFor(Resource iri, PrefixMapping pm) {
		return (iri == null) ? null : qnameFor(iri.getURI(), pm);
	}

	/**
	 * Converts an IRI to a Q-name using the supplied prefix mapping.
	 *
	 * @param iri The IRI to convert
	 * @param pm  The prefix mapping to use for the conversion
	 * @return The Q-name according to the supplied prefix mapping. If the IRI is
	 *         null, empty, or appears to be a Q-name already, then it is returned
	 *         unchanged.
	 */
	public static String qnameFor(String iri, PrefixMapping pm) {
		return (iri == null || iri.isEmpty() || !IRI_PATTERN.matcher(iri).matches())
			? iri : pm.shortForm(iri);
	}
}
