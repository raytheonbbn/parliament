package com.bbn.parliament.odda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

// Comparable only to store instances in a consistent order, which need not be useful.
public class WktLiteralPoint implements Comparable<WktLiteralPoint> {
	private static final Pattern POINT_PATTERN = Pattern.compile(
		"\\h*point\\h*\\(\\h*(-?[0-9.]+)\\h+(-?[0-9.]+)\\h*\\)\\h*",
		Pattern.CASE_INSENSITIVE);
	private static final String POINT_FMT = "Point(%1$s %2$s)";

	private final double latitude;
	private final double longitude;

	public WktLiteralPoint(Literal point) {
		this(point.getLexicalForm(), ResourceFactory.createResource(point.getDatatypeURI()));
	}

	public WktLiteralPoint(String lexicalForm, Resource datatypeIri) {
		if (!Geo.wktLiteral.equals(datatypeIri)) {
			throw new IllegalArgumentException(String.format(
				"Unexpected datatype \"%1$s\" on WKT literal", datatypeIri));
		}
		Matcher matcher = POINT_PATTERN.matcher(lexicalForm);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(String.format(
				"Unrecognized WKT literal syntax: \"%1$s\"", lexicalForm));
		}
		String doubleStr = null;

		try {
			doubleStr = matcher.group(1);
			latitude = Double.parseDouble(doubleStr);
			doubleStr = matcher.group(2);
			longitude = Double.parseDouble(doubleStr);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(String.format(
				"Error parsing double \"%1$s\": %2$s", doubleStr, ex.getMessage()), ex);
		}
	}

	public WktLiteralPoint(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public static Resource datatypeIri() {
		return Geo.wktLiteral;
	}

	public String lexicalForm() {
		return String.format(POINT_FMT, Double.toString(latitude), Double.toString(longitude));
	}

	@Override
	public String toString() {
		return lexicalForm();
	}

	public double latitude() {
		return latitude;
	}

	public double longitude() {
		return longitude;
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		}
		if (rhs == null || getClass() != rhs.getClass()) {
			return false;
		}
		WktLiteralPoint other = (WktLiteralPoint) rhs;
		return new EqualsBuilder()
			.append(latitude, other.latitude)
			.append(longitude, other.longitude)
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(latitude)
			.append(longitude)
			.toHashCode();
	}

	@Override
	public int compareTo(WktLiteralPoint rhs) {
		if (rhs == null) {
			return 1;
		}
		return new CompareToBuilder()
			.append(latitude, rhs.latitude)
			.append(longitude, rhs.longitude)
			.build();
	}
}
