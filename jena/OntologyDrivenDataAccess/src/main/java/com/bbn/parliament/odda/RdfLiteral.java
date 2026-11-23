package com.bbn.parliament.odda;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.XSD;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RdfLiteral implements Comparable<RdfLiteral> {
	private static final Map<Resource, LiteralJsonTranslator> ALLOWED_DATATYPES;

	private final String lexicalForm;
	private final Resource datatypeIri;

	static {
		ALLOWED_DATATYPES = new HashMap<>();
		ALLOWED_DATATYPES.put(XSD.xstring, JsonWriter::value);
		ALLOWED_DATATYPES.put(XSD.date, JsonWriter::value);
		ALLOWED_DATATYPES.put(XSD.time, JsonWriter::value);
		ALLOWED_DATATYPES.put(XSD.dateTime, JsonWriter::value);
		ALLOWED_DATATYPES.put(XSD.integer, (wtr, lexicalForm) -> wtr.value(Long.valueOf(lexicalForm)));
		ALLOWED_DATATYPES.put(XSD.decimal, (wtr, lexicalForm) -> wtr.value(new BigDecimal(lexicalForm)));
		ALLOWED_DATATYPES.put(XSD.xfloat, (wtr, lexicalForm) -> wtr.value(Float.valueOf(lexicalForm)));
		ALLOWED_DATATYPES.put(XSD.xdouble, (wtr, lexicalForm) -> wtr.value(Double.valueOf(lexicalForm)));
		ALLOWED_DATATYPES.put(XSD.xboolean, (wtr, lexicalForm) -> wtr.value(Boolean.parseBoolean(lexicalForm)));
		ALLOWED_DATATYPES.put(WktLiteralPoint.datatypeIri(), (wtr, lexicalForm) -> {
			WktLiteralPoint pt = new WktLiteralPoint(lexicalForm, WktLiteralPoint.datatypeIri());
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.name(EntityTypeAdapter.LAT_JSON_FIELD_NAME);
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp2 = wtr.value(pt.latitude());
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp3 = wtr.name(EntityTypeAdapter.LON_JSON_FIELD_NAME);
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp4 = wtr.value(pt.longitude());
		});
	}

	public RdfLiteral(Literal literal) {
		ArgCheck.throwIfNull(literal, "literal");
		lexicalForm = literal.getLexicalForm();
		ArgCheck.throwIfNull(lexicalForm, "literal.getLexicalForm()");
		datatypeIri = normalizeDtIri(ResourceFactory.createResource(literal.getDatatypeURI()));
	}

	public RdfLiteral(String lexicalForm, Resource datatype) {
		this.lexicalForm = lexicalForm;
		this.datatypeIri = normalizeDtIri(datatype);
	}

	public RdfLiteral(String lexicalForm, String datatypeIri) {
		this.lexicalForm = lexicalForm;
		this.datatypeIri = normalizeDtIri(ResourceFactory.createResource(datatypeIri));
	}

	public RdfLiteral(String value) {
		this(value, (String) null);
	}

	public RdfLiteral(WktLiteralPoint value) {
		this(value.lexicalForm(), WktLiteralPoint.datatypeIri());
	}

	public RdfLiteral(XMLGregorianCalendar value) {
		// Qname.toString() returns {namesapaceIri}localPart NOT namespaceIri#localPart:
		this(value.toXMLFormat(), String.format("%s#%s",
			value.getXMLSchemaType().getNamespaceURI(),
			value.getXMLSchemaType().getLocalPart()));

		if (value.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
			throw new IllegalArgumentException(
				"xsd:date, xsd:time, and xsd:dateTime literals must specify an explicit timezone");
		}
	}

	public RdfLiteral(long value) {
		this(Long.toString(value), XSD.integer);
	}

	public RdfLiteral(int value) {
		this(Integer.toString(value), XSD.integer);
	}

	public RdfLiteral(double value) {
		this(Double.toString(value), XSD.decimal);
	}

	public RdfLiteral(float value) {
		this(Float.toString(value), XSD.decimal);
	}

	public RdfLiteral(boolean value) {
		this(Boolean.toString(value), XSD.xboolean);
	}

	private static Resource normalizeDtIri(Resource dtIri) {
		Resource result = (dtIri == null) ? XSD.xstring : dtIri;
		if (!ALLOWED_DATATYPES.containsKey(result)) {
			String dtList = ALLOWED_DATATYPES.keySet().stream().map(Resource::getURI).collect(Collectors.joining(", "));
			throw new IllegalArgumentException(String.format("%1$s is not one of the recognized literal data types %2$s",
				result.getURI(), dtList));
		}
		return result;
	}

	public String lexicalForm() {
		return lexicalForm;
	}

	public String asString() {
		return lexicalForm;
	}

	public WktLiteralPoint asWktLiteral() {
		return new WktLiteralPoint(lexicalForm, datatypeIri);
	}

	public XMLGregorianCalendar asXMLGregCal() {
		return QSUtil.DT_FACT.newXMLGregorianCalendar(lexicalForm);
	}

	public long asLong() {
		return Long.parseLong(lexicalForm);
	}

	public int asInt() {
		return Integer.parseInt(lexicalForm);
	}

	public double asDouble() {
		return Double.parseDouble(lexicalForm);
	}

	public float asFloat() {
		return Float.parseFloat(lexicalForm);
	}

	public boolean asBool() {
		return Boolean.parseBoolean(lexicalForm);
	}

	public Resource datatypeIri() {
		return datatypeIri;
	}

	public Literal asLiteral(Model model) {
		return XSD.xstring.equals(datatypeIri)
			? model.createTypedLiteral(lexicalForm, (String) null)
			: model.createTypedLiteral(lexicalForm, datatypeIri.getURI());
	}

	@Override
	public String toString() {
		return XSD.xstring.equals(datatypeIri)
			? String.format("\"%1$s\"", lexicalForm)
			: String.format("\"%1$s\"*%%2$s", lexicalForm, datatypeIri);
	}

	// Too clever by half, but the lookup table avoid PMD's cyclomatic complexity limit:
	public void writeAsJson(JsonWriter wtr) throws IOException {
		LiteralJsonTranslator translator = ALLOWED_DATATYPES.get(datatypeIri);
		if (translator == null) {
			throw new IOException("Unrecognized datatype IRI: %1$s".formatted(datatypeIri.getURI()));
		} else {
			translator.accept(wtr, lexicalForm);
		}
	}

	public static RdfLiteral readFromJson(JsonReader rdr) throws IOException, IllegalStateException {
		if (rdr.peek() == JsonToken.NULL) {
			return null;
		} else if (rdr.peek() == JsonToken.BOOLEAN) {
			return new RdfLiteral(rdr.nextBoolean());
		} else if (rdr.peek() == JsonToken.NUMBER) {
			String lexForm = rdr.nextString();
			try {
				return new RdfLiteral(Long.parseLong(lexForm));
			} catch (NumberFormatException ex) {
				return new RdfLiteral(lexForm, XSD.decimal);
			}
		} else if (rdr.peek() == JsonToken.STRING) {
			String lexForm = rdr.nextString();
			try {
				return new RdfLiteral(QSUtil.DT_FACT.newXMLGregorianCalendar(lexForm));
			} catch (IllegalArgumentException ex) {
				try {
					return new RdfLiteral(new WktLiteralPoint(lexForm, WktLiteralPoint.datatypeIri()));
				} catch (IllegalArgumentException ex2) {
					return new RdfLiteral(lexForm);
				}
			}
		} else {
			throw new IllegalStateException("Expected a literal value in JSON stream");
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other == null || getClass() != other.getClass()) {
			return false;
		} else {
			RdfLiteral that = (RdfLiteral) other;
			return new EqualsBuilder()
				.append(lexicalForm, that.lexicalForm)
				.append(datatypeIri, that.datatypeIri)
				.isEquals();
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(lexicalForm)
			.append(datatypeIri)
			.toHashCode();
	}

	@Override

	public int compareTo(RdfLiteral rhs) {
		return new CompareToBuilder()
			.append(lexicalForm, rhs.lexicalForm)
			.append(datatypeIri, rhs.datatypeIri)
			.build();
	}

	@FunctionalInterface
	private interface LiteralJsonTranslator {
		void accept(JsonWriter wtr, String lexicalForm) throws IOException;
	}
}
