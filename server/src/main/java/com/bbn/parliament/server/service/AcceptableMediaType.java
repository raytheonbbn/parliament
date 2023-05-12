package com.bbn.parliament.server.service;

import static org.apache.jena.riot.WebContent.ctJSON;
import static org.apache.jena.riot.WebContent.ctResultsJSON;
import static org.apache.jena.riot.WebContent.ctResultsXML;
import static org.apache.jena.riot.WebContent.ctTextCSV;
import static org.apache.jena.riot.WebContent.ctTextTSV;
import static org.apache.jena.riot.WebContent.ctXML;
import static org.apache.jena.riot.WebContent.ctXMLAlt;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.Lang;

public enum AcceptableMediaType {
	// Media types for construct and describe queries:
	TURTLE("turtle", Lang.TURTLE),
	RDF_XML("rdfxml", Lang.RDFXML),
	N_TRIPLES("ntriples", Lang.NTRIPLES),
	N3("n3", Lang.N3),
	JSON_LD("jsonld", Lang.JSONLD),

	// Media types for select and ask queries:
	XML_RESULTS("xml",
		List.of(ctResultsXML, ctXML, ctXMLAlt),
		(out, resultSet) -> ResultSetFormatter.outputAsXML(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsXML(out, result)),
	JSON_RESULTS("json",
		List.of(ctResultsJSON, ctJSON),
		(out, resultSet) -> ResultSetFormatter.outputAsJSON(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsJSON(out, result)),
	CSV_RESULTS("csv",
		List.of(ctTextCSV),
		(out, resultSet) -> ResultSetFormatter.outputAsCSV(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsCSV(out, result)),
	TSV_RESULTS("tsv",
		List.of(ctTextTSV),
		(out, resultSet) -> ResultSetFormatter.outputAsTSV(out, resultSet),
		(out, result) -> ResultSetFormatter.outputAsTSV(out, result));

	private final QueryResultCategory category;
	private final String queryStringFormat;
	private final Lang rdfLang;
	private final List<ContentType> mediaTypes;
	private final BiConsumer<OutputStream, ResultSet> serializeResultSet;
	private final BiConsumer<OutputStream, Boolean> serializeAskResults;

	// For QueryResultCategory.RDF:
	private AcceptableMediaType(String queryStringFormat, Lang rdfLang) {
		this.category = QueryResultCategory.RDF;
		this.queryStringFormat = Objects.requireNonNull(queryStringFormat, "queryStringFormat");
		this.rdfLang = Objects.requireNonNull(rdfLang, "rdfLang");
		this.mediaTypes = extractContentTypes(this.rdfLang);
		this.serializeResultSet = null;
		this.serializeAskResults = null;
	}

	// For QueryResultCategory.RESULT_SET:
	private AcceptableMediaType(String queryStringFormat, List<ContentType> mediaTypes,
		BiConsumer<OutputStream, ResultSet> serializeResultSet,
		BiConsumer<OutputStream, Boolean> serializeAskResults) {
		this.category = QueryResultCategory.RESULT_SET;
		this.queryStringFormat = Objects.requireNonNull(queryStringFormat, "queryStringFormat");
		this.rdfLang = null;
		this.mediaTypes = Objects.requireNonNull(mediaTypes, "mediaTypes");
		this.serializeResultSet = Objects.requireNonNull(serializeResultSet, "serializeResultSet");
		this.serializeAskResults = Objects.requireNonNull(serializeAskResults, "serializeAskResults");
	}

	private static List<ContentType> extractContentTypes(Lang lang) {
		var mainCTStream = Stream.of(lang.getContentType());
		var altCTStream = lang.getAltContentTypes().stream()
			.map(ContentType::create);
		return Stream.concat(mainCTStream, altCTStream).toList();
	}

	public QueryResultCategory getCategory() {
		return category;
	}

	public String getQueryStringFormat() {
		return queryStringFormat;
	}

	public boolean hasMediaType(String mediaType, String mediaSubType) {
		String mediaTypeStr = "%1$s/%2$s".formatted(
			Objects.requireNonNull(mediaType, "mediaType").strip(),
			Objects.requireNonNull(mediaSubType, "mediaSubType").strip());
		return mediaTypes.stream()
			.map(ContentType::getContentTypeStr)
			.anyMatch(mediaTypeStr::equalsIgnoreCase);
	}

	public ContentType getPrimaryMediaType() {
		return mediaTypes.get(0);
	}

	public String getPrimaryFileExtension() {
		return (rdfLang == null)
			? null
			: rdfLang.getFileExtensions().get(0);
	}

	public Lang getRdfLang() {
		return rdfLang;
	}

	public void serializeResultSet(OutputStream out, ResultSet resultSet) {
		serializeResultSet.accept(out, resultSet);
	}

	public void serializeResultSet(OutputStream out, boolean resultSet) {
		serializeAskResults.accept(out, resultSet);
	}

	public static Optional<AcceptableMediaType> find(String mediaType, String mediaSubType) {
		return Stream.of(AcceptableMediaType.values())
			.filter(amt -> amt.hasMediaType(mediaType, mediaSubType))
			.findFirst();
	}

	public static Optional<AcceptableMediaType> find(String queryStringFormat) {
		return Stream.of(AcceptableMediaType.values())
			.filter(amt -> amt.queryStringFormat.equalsIgnoreCase(queryStringFormat))
			.findFirst();
	}
}
