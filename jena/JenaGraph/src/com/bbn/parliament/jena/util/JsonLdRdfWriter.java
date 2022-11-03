package com.bbn.parliament.jena.util;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFErrorHandler;
import org.apache.jena.rdf.model.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

public class JsonLdRdfWriter implements RDFWriter {
	/** Mime type for JSON-LD */
	public static final String contentType = "application/ld+json";

	/** Format name for JSON-LD used by Model's read/write functions */
	public static final String formatName = RDFFormat.JSON_LD.toString();

	private static Logger log = LoggerFactory.getLogger(JsonLdRdfWriter.class);

	private RDFErrorHandler errHandler;

	public JsonLdRdfWriter() {
		log.debug("Constructing a {}", JsonLdRdfWriter.class.getSimpleName());
		errHandler = null;
	}

	@Override
	public RDFErrorHandler setErrorHandler(RDFErrorHandler errHandler) {
		log.debug("Setting error handler of type {}", errHandler.getClass().getSimpleName());
		RDFErrorHandler oldErrHandler = this.errHandler;
		this.errHandler = errHandler;
		return oldErrHandler;
	}

	/** Does nothing -- no options are supported by this class. */
	@Override
	public Object setProperty(String propName, Object propValue) {
		log.debug("Setting property {} to {}", propName, propValue);
		return null;
	}

	@Override
	public void write(Model model, Writer wtr, String base) {
		try {
			String jsonLdContent = getJsonLdContent(model, base);
			wtr.write(jsonLdContent);
		} catch (JsonLdError | IOException ex) {
			error(ex);
		}
	}

	@Override
	public void write(Model model, OutputStream out, String base) {
		try {
			String jsonLdContent = getJsonLdContent(model, base);
			out.write(jsonLdContent.getBytes("UTF-8"));
		} catch (JsonLdError | IOException ex) {
			error(ex);
		}
	}

	private static String getJsonLdContent(Model model, String base)
		throws JsonLdError, IOException {

		String ttlContent;
		try (CharArrayWriter wtr = new CharArrayWriter()) {
			model.write(wtr, "TURTLE", base);
			ttlContent = wtr.toString();
		}
		log.debug("Turtle content:  {}", ttlContent);

		JsonLdOptions options = new JsonLdOptions();
		options.format = "text/turtle";

		Object jsonObject = JsonLdProcessor.fromRDF(ttlContent, options);
		log.debug("jsonObject is {}", (jsonObject == null) ? "null" : "non-null");
		return JsonUtils.toPrettyString(jsonObject);
	}

	private void error(Exception ex) {
		log.error("Calling error handler", ex);
		if (errHandler != null) {
			errHandler.error(ex);
		}
	}
}
