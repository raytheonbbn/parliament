package com.bbn.parliament.spring_boot.service;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

public enum AcceptableMediaType {
	/** Turtle format */
	TURTLE(QueryResultCategory.RDF, "turtle",
		new String[]{ "text/turtle", "application/x-turtle" },
		RDFFormat.TURTLE, null, null),

	/** RDF/XML format */
	RDF_XML(QueryResultCategory.RDF, "rdfxml",
		new String[]{ "application/rdf+xml" },
		RDFFormat.RDFXML, null, null),

	/** N-Triples format */
	N_TRIPLES(QueryResultCategory.RDF, "ntriples",
		new String[]{ "application/n-triples", "text/plain" },
		RDFFormat.NTRIPLES, null, null),

	/** N3 format */
	N3(QueryResultCategory.RDF, "n3",
		new String[]{ "text/n3" },
		RDFFormat.N3, null, null),

	/** JSON-LD format */
	JSON_LD(QueryResultCategory.RDF, "jsonld",
		new String[]{ "application/ld+json", "application/json" },
		RDFFormat.JSON_LD, null, null),

	XML_RESULTS(QueryResultCategory.RESULT_SET, "xml",
		new String[]{ "application/sparql-results+xml", "application/xml", "text/xml" },
		null,
		(out, resultSet) -> ResultSetFormatter.outputAsXML(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsXML(out, result)),

	JSON_RESULTS(QueryResultCategory.RESULT_SET, "json",
		new String[]{ "application/sparql-results+json", "application/json", "text/json" },
		null,
		(out, resultSet) -> ResultSetFormatter.outputAsJSON(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsJSON(out, result)),

	CSV_RESULTS(QueryResultCategory.RESULT_SET, "csv",
		new String[]{ "text/csv" },
		null,
		(out, resultSet) -> ResultSetFormatter.outputAsCSV(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsCSV(out, result)),

	TSV_RESULTS(QueryResultCategory.RESULT_SET, "tsv",
		new String[]{ "text/tab-separated-values" },
		null,
		(out, resultSet) -> ResultSetFormatter.outputAsTSV(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsTSV(out, result));

	private final QueryResultCategory category;
	private final String queryStringFormat;
	private final String[] mediaTypes;
	private final RDFFormat rdfFormat;
	private final BiConsumer<OutputStream, ResultSet> serializeResultSet;
	private final BiConsumer<OutputStream, Boolean> serializeAskResults;

	private AcceptableMediaType(QueryResultCategory category, String queryStringFormat,
		String[] mediaTypes, RDFFormat rdfFormat,
		BiConsumer<OutputStream, ResultSet> serializeResultSet,
		BiConsumer<OutputStream, Boolean> serializeAskResults) {
		this.category = Objects.requireNonNull(category, "category");
		this.queryStringFormat = Objects.requireNonNull(queryStringFormat, "queryStringFormat");
		this.mediaTypes = Objects.requireNonNull(mediaTypes, "mediaTypes");
		if (category == QueryResultCategory.RDF) {
			this.rdfFormat = Objects.requireNonNull(rdfFormat, "rdfFormat");
			this.serializeResultSet = null;
			this.serializeAskResults = null;
		} else {
			this.rdfFormat = null;
			this.serializeResultSet = Objects.requireNonNull(serializeResultSet, "serializeResultSet");
			this.serializeAskResults = Objects.requireNonNull(serializeAskResults, "serializeAskResults");
		}
	}

	public QueryResultCategory getCategory() {
		return category;
	}

	public String getQueryStringFormat() {
		return queryStringFormat;
	}

	public boolean hasQueryStringFormat(String format) {
		return queryStringFormat.equalsIgnoreCase(format);
	}

	public boolean hasMediaType(String mediaType, String mediaSubType) {
		String mediaTypeStr = "%1$s/%2$s".formatted(
			Objects.requireNonNull(mediaType, "mediaType").trim(),
			Objects.requireNonNull(mediaSubType, "mediaSubType").trim());
		return Arrays.stream(mediaTypes)
			.anyMatch(mediaTypeStr::equalsIgnoreCase);
	}

	public String getPrimaryMediaType() {
		return mediaTypes[0];
	}

	public RDFFormat getRdfFormat() {
		return rdfFormat;
	}

	public void serializeResultSet(OutputStream out, ResultSet resultSet) {
		serializeResultSet.accept(out, resultSet);
	}

	public void serializeResultSet(OutputStream out, boolean resultSet) {
		serializeAskResults.accept(out, resultSet);
	}

	public static Optional<AcceptableMediaType> find(String mediaType, String mediaSubType) {
		return Arrays.stream(AcceptableMediaType.values())
			.filter(amt -> amt.hasMediaType(mediaType, mediaSubType))
			.findFirst();
	}

	public static Optional<AcceptableMediaType> find(String queryStringFormat) {
		return Arrays.stream(AcceptableMediaType.values())
			.filter(amt -> amt.hasQueryStringFormat(queryStringFormat))
			.findFirst();
	}
}
