package org.semwebcentral.parliament.odda;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.XSD;
import org.semwebcentral.parliament.misc_needing_refactor.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QSUtil {
	private static final Logger LOG = LoggerFactory.getLogger(QSUtil.class);
	public static final DatatypeFactory DT_FACT;

	static {
		try {
			DT_FACT = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException ex) {
			String msg = "Error while initializing javax.xml.datatype.DatatypeFactory instance";
			LOG.error(msg, ex);
			throw new IllegalStateException(msg, ex);
		}
	}

	private QSUtil() {} // Prevent instantiation

	public static boolean getBool(QuerySolution qs, String varName, boolean defaultValue) {
		Literal lit = qs.getLiteral(varName);
		return (lit == null) ? defaultValue : lit.getBoolean();
	}

	public static Long getInteger(QuerySolution qs, String varName) {
		Literal lit = qs.getLiteral(varName);
		return (lit == null) ? null : lit.getLong();
	}

	public static Double getDouble(QuerySolution qs, String varName) {
		Literal lit = qs.getLiteral(varName);
		return (lit == null) ? null : lit.getDouble();
	}

	public static String getString(QuerySolution qs, String varName) {
		Literal lit = qs.getLiteral(varName);
		return (lit == null) ? null : lit.getLexicalForm();
	}

	public static WktLiteralPoint getWktLiteralPoint(QuerySolution qs, String varName) {
		Literal lit = qs.getLiteral(varName);
		return (lit == null) ? null : new WktLiteralPoint(lit);
	}

	public static XMLGregorianCalendar getDate(QuerySolution qs, String varName) {
		return getTimeRelatedLiteral(qs, varName, XSD.date);
	}

	public static XMLGregorianCalendar getDateTime(QuerySolution qs, String varName) {
		return getTimeRelatedLiteral(qs, varName, XSD.dateTime);
	}

	public static XMLGregorianCalendar getTime(QuerySolution qs, String varName) {
		return getTimeRelatedLiteral(qs, varName, XSD.time);
	}

	private static XMLGregorianCalendar getTimeRelatedLiteral(QuerySolution qs, String varName,
		Resource expectedDatatype) {
		XMLGregorianCalendar result = null;
		Literal lit = qs.getLiteral(varName);
		if (lit != null) {
			if (expectedDatatype.getURI().equals(lit.getDatatypeURI())) {
				result = xgcFromString(lit.getLexicalForm());
			} else {
				LOG.warn("Expected RDF literal of type {}, but found PLFA",
					QName.asQName(expectedDatatype), QName.asQName(lit.getDatatypeURI()));
			}
		}
		return result;
	}

	public static XMLGregorianCalendar xgcFromString(String lexicalForm) {
		XMLGregorianCalendar result = null;
		if (lexicalForm == null || lexicalForm.isEmpty()) {
			LOG.debug("lexicalForm is null or empty");
		} else {
			result = DT_FACT.newXMLGregorianCalendar(lexicalForm);
			if (result == null) {
				LOG.warn("Unable to translate '{}' to an XMLGregorianCalendar", lexicalForm);
			}
		}
		return result;
	}
}
