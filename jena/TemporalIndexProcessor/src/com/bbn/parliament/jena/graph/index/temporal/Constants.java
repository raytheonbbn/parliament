package com.bbn.parliament.jena.graph.index.temporal;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.hp.hpl.jena.graph.Node;

public class Constants {
	public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
	public static final String OT_NS = "http://www.w3.org/2006/time#";	// OWL Time
	public static final String PT_NS = "http://bbn.com/ParliamentTime#";	// Parliament Time

	public static final String INDEX_TYPE = "indexType";
	public static final String INDEX_PERSISTENT = "bdb";
	public static final String INDEX_MEMORY = "mem";

	public static final String ALWAYS_USE_FIRST = "alwaysUseFirst";

	public static final int QUERY_CACHE_SIZE = 100;

	public static final Node XSDDATETIME_URI = createNode(XSD_NS, "dateTime");

	public static final Node TEMPORAL_ENTITY = createNode(OT_NS, "TemporalEntity");
	public static final Node DATE_TIME_INTERVAL = createNode(OT_NS, "DateTimeInterval");
	public static final Node PROPER_INTERVAL = createNode(OT_NS, "ProperInterval");
	public static final Node OT_INSTANT = createNode(OT_NS, "Instant");
	public static final Node OT_INTERVAL = createNode(OT_NS, "Interval");
	public static final Node DATE_TIME = createNode(OT_NS, "xsdDateTime");
	public static final Node INTERVAL_STARTED_BY = createNode(OT_NS, "intervalStartedBy");
	public static final Node INTERVAL_FINISHED_BY = createNode(OT_NS, "intervalFinishedBy");

	public static final Node PT_AS_INSTANT = createNode(PT_NS, "asInstant");
	public static final Node PT_AS_INTERVAL = createNode(PT_NS, "asInterval");
	// see Operand.java for pt:instantEquals
	public static final Node PT_TIME_INTERVAL = createNode(PT_NS, "intervalLiteral");
	public static final Node PT_EXTENT = createNode(PT_NS, "temporalExtent");

	public static final DatatypeFactory XML_DT_FACTORY;

	static {
		try {
			XML_DT_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("Error occured from a single line of initialization.");
		}
	}

	private static Node createNode(String ns, String localName) {
		return Node.createURI(ns + localName);
	}
}
