package com.bbn.parliament.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.RDFLanguages;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPartBodyPublisherBuilderTest {
	private static final File DATA_DIR = new File(System.getProperty("test.data.path"));
	private static final Logger LOG = LoggerFactory.getLogger(MultiPartBodyPublisherBuilderTest.class);

	@SuppressWarnings("static-method")
	@Test
	public void multiPartBodyPublisherBuilderTest() {
		Function<File, ContentType> contentTypeDeducer =
			f -> RDFLanguages.pathnameToLang(f.getName()).getContentType();
		var partIterable = new MultiPartBodyPublisherBuilder()
			.addPart("graph", "http://example.org/#TestGraph")
			.addPart("file", new File(DATA_DIR, "geo-example.ttl"), contentTypeDeducer)
			.addPart("file", new File(DATA_DIR, "univ-bench.owl"), contentTypeDeducer)
			.createPartIterator();
		var buffer = new byte[0];
		for (var part : partIterable) {
			var newBuffer = new byte[buffer.length + part.length];
			System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
			System.arraycopy(part, 0, newBuffer, buffer.length, part.length);
			buffer = newBuffer;
		}
		LOG.info("Multi-part request body:{}{}", System.lineSeparator(),
			new String(buffer, StandardCharsets.UTF_8));
		assertTrue(true);
	}
}
