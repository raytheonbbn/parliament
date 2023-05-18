// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.joseki.josekibridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.jena.vocabulary.RDF;

import com.bbn.parliament.client.RemoteModel;
import com.bbn.parliament.kb_graph.KbGraphStore;

/** @author dkolas */
public class InsertionTester {

	public static void main(String[] args) throws Exception{
		//sendInsertRequest(GraphFactoryGraphModel.NAMED_GRAPHS_GRAPH, statementsForAGraphDeclaration("http://foo/#foo", "foo"));
		//sendInsertRequest("http://foo/#foo", "<http://foo/#Dave> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo/#Person> .");
		//sendInsertRequest(null, "<http://foo/#foo> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo/#AGoodGraphToQuery> .");

		sendInsertRequest(KbGraphStore.MASTER_GRAPH.getURI(), """
			<http://foo/#UnionFoo> <%1$s> <%2$s> ;
				<%3$s> <http://foo/#foo> ;
				<%4$s> <http://foo/#foo2> .
			""".formatted(RDF.type.getURI(), KbGraphStore.UNION_GRAPH_CLASS,
				KbGraphStore.LEFT_GRAPH_PROPERTY,
				KbGraphStore.RIGHT_GRAPH_PROPERTY));

		sendInsertRequest(KbGraphStore.MASTER_GRAPH.getURI(),
			statementsForAGraphDeclaration("http://foo/#foo2", "foo2"));
		sendInsertRequest("http://foo/#foo2",
			"<http://foo/#Dave> <%1$s> <http://foo/#Tool> .".formatted(RDF.type.getURI()));
	}

	private static String statementsForAGraphDeclaration(String uri, String graphDir) {
		return """
			<%1$s> <%2$s> <%3$s> ;
				<%4$s> "%5$s" .
			""".formatted(uri, RDF.type.getURI(), KbGraphStore.GRAPH_CLASS,
				KbGraphStore.GRAPH_DIR_PROPERTY, graphDir);
	}

	private static void sendInsertRequest(String graph, String statements) throws IOException {
		Map<String, String> params = new HashMap<>();
		params.put("insert",statements);
		if (graph != null && !graph.isEmpty()){
			params.put("graph", graph);
		}
		params.put("dataFormat", "N3");
		System.out.println("Statements:\n"+statements);
		sendRequest(params);
	}

	private static void sendRequest(Map<String, String> params) throws IOException{
		// Construct data
		String data = params.entrySet().stream()
			.map(InsertionTester::encodeMapEntry)
			.collect(Collectors.joining("&"));

		// Send data
		URL url = new URL(RemoteModel.DEFAULT_SPARQL_ENDPOINT_URL.formatted("localhost", 8089));
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		try (
			OutputStream ostrm = conn.getOutputStream();
			Writer wtr = new OutputStreamWriter(ostrm, StandardCharsets.UTF_8);
		) {
			wtr.write(data);
			wtr.flush();

			// Get the response
			try (
				InputStream istrm = conn.getInputStream();
				Reader iStrmRdr = new InputStreamReader(istrm, StandardCharsets.UTF_8);
				BufferedReader rdr = new BufferedReader(iStrmRdr);
			) {
				rdr.lines().forEach(line -> System.out.println(line));
			}
		}
	}

	private static String encodeMapEntry(Map.Entry<String, String> entry) {
		try {
			return URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			// This will never happen, because UTF-8 is always supported:
			throw new RuntimeException(ex);
		}
	}
}
