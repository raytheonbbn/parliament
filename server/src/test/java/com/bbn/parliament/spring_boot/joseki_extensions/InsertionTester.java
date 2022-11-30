// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.spring_boot.joseki_extensions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.test_util.GraphUtils;

/** @author dkolas */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class InsertionTester {

	private static final String HOST = "localhost";
	private static final String TEST_SUBJECT = "http://foo/#foo";
	private static final String TEST_SUBJECT2 = "http://foo/#foo2";
	private static final String TEST_SUBJECT3 = "http://foo/#Dave";
	private static final String TEST_UNION_SUBJECT = "http://foo/#UnionFoo";
	private static final String TEST_OBJECT = "http://foo/#Tool";
	private static final String TEST_GRAPH = "foo2";
	private static final Logger LOG = LoggerFactory.getLogger(InsertionTester.class);

	@LocalServerPort
	private int serverPort;

	private String sparqlUrl;
	private String updateUrl;
	private String graphStoreUrl;

	@BeforeEach
	public void beforeEach() {
		sparqlUrl = "http://%1$s:%2$s/parliament/sparql".formatted(HOST, serverPort);
		updateUrl = "http://%1$s:%2$s/parliament/update".formatted(HOST, serverPort);
		graphStoreUrl = "http://%1$s:%2$s/parliament/graphstore".formatted(HOST, serverPort);
	}

	@AfterEach
	public void afterEach() {
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);
	}

	@Test
	public void test() throws Exception{
		String triples = """
			<%1$s> <%2$s> <%3$s> ;
				<%4$s> <%5$s> ;
				<%6$s> <%7$s> .
			""".formatted(TEST_UNION_SUBJECT, RDF.type, KbGraphStore.UNION_GRAPH_CLASS,
				KbGraphStore.LEFT_GRAPH_PROPERTY,
				TEST_SUBJECT,
				KbGraphStore.RIGHT_GRAPH_PROPERTY,
				TEST_SUBJECT2);
		GraphUtils.insertStatements(graphStoreUrl, triples, RDFFormat.NTRIPLES, URLEncoder.encode(KbGraphStore.MASTER_GRAPH, StandardCharsets.UTF_8.toString()));

		GraphUtils.insertStatements(graphStoreUrl,
			statementsForAGraphDeclaration(TEST_SUBJECT2, TEST_GRAPH),
			RDFFormat.NTRIPLES, URLEncoder.encode(KbGraphStore.MASTER_GRAPH, StandardCharsets.UTF_8.toString()));

		GraphUtils.insertStatements(graphStoreUrl, "<%1$s> <%2$s> <%3$s> .".formatted(TEST_SUBJECT3, RDF.type, TEST_OBJECT),
				RDFFormat.NTRIPLES, TEST_SUBJECT2);
	}

	private static String statementsForAGraphDeclaration(String uri, String graphDir) {
		return """
			<%1$s> <%2$s> <%3$s> ;
				<%4$s> "%5$s" .
			""".formatted(uri, RDF.type.getURI(), KbGraphStore.GRAPH_CLASS,
				KbGraphStore.GRAPH_DIR_PROPERTY, graphDir);
	}

}
