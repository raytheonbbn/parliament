package com.bbn.parliament.jena.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.impl.TurtleTripleCallback;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFReader;

public class JsonLdRdfReader implements RDFReader {
	/** Mime type for JSON-LD */
	public static final String contentType = JsonLdRdfWriter.contentType;

	/** Format name for JSON-LD used by Model's read/write functions */
	public static final String formatName = JsonLdRdfWriter.formatName;

	private static Logger log = LoggerFactory.getLogger(JsonLdRdfReader.class);

	private RDFErrorHandler errHandler;

	public JsonLdRdfReader() {
		log.debug("Constructing a {}", JsonLdRdfReader.class.getSimpleName());
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
	public void read(Model model, String url) {
		try (CloseableHttpClient httpClient = JsonUtils.getDefaultHttpClient()) {
			Object jsonObj = JsonUtils.fromURL(new URL(url), httpClient);
			insertJsonIntoModel(model, url, jsonObj);
		} catch (IOException | JsonLdError ex) {
			error(ex);
		}
	}

	@Override
	public void read(Model model, Reader rdr, String base) {
		try {
			Object jsonObj = JsonUtils.fromReader(rdr);
			insertJsonIntoModel(model, base, jsonObj);
		} catch (IOException | JsonLdError ex) {
			error(ex);
		}
	}

	@Override
	public void read(Model model, InputStream in, String base) {
		try {
			Object jsonObj = JsonUtils.fromInputStream(in);
			insertJsonIntoModel(model, base, jsonObj);
		} catch (IOException | JsonLdError ex) {
			error(ex);
		}
	}

	private static void insertJsonIntoModel(Model model, String base, Object jsonObj)
		throws JsonLdError, IOException {
		Object turtleOutput = JsonLdProcessor.toRDF(jsonObj, new TurtleTripleCallback());
		String turtleOutputStr = (String) turtleOutput;
		try (Reader rdr = new StringReader(turtleOutputStr)) {
			model.read(rdr, base, RDFFormat.TURTLE.toString());
		}
	}

	private void error(Exception ex) {
		log.error("Calling error handler", ex);
		if (errHandler != null) {
			errHandler.error(ex);
		}
	}
}
