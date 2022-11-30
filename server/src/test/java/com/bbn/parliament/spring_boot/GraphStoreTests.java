package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.jena.atlas.web.HttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.client.QuerySolutionStream;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.test_util.RdfResourceLoader;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class GraphStoreTests {
	private static final String HOST = "localhost";
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final Logger LOG = LoggerFactory.getLogger(GraphStoreTests.class);

	private static final String EVERYTHING_QUERY = """
		select distinct ?s ?o ?p ?g where {
			{ ?s ?p ?o }
			union
			{ graph ?g { ?s ?p ?o } }
		}
		""";
	private static final String CLASS_QUERY = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		select distinct ?class where {
			?class a owl:Class .
			filter (!isblank(?class))
		}
		""";

	@LocalServerPort
	private int serverPort;

	private String sparqlUrl;
	private String graphStoreUrl;

	@BeforeEach
	public void beforeEach() {
		sparqlUrl = "http://%1$s:%2$s/parliament/sparql".formatted(HOST, serverPort);
		graphStoreUrl = "http://%1$s:%2$s/parliament/graphstore".formatted(HOST, serverPort);
	}

	@Test
@Disabled
	public void graphStoreInsertTest() {
		try (QuerySolutionStream stream = doSelectQuery(EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		loadSampleData();

		try (QuerySolutionStream stream = doSelectQuery(CLASS_QUERY)) {
			assertEquals(43, stream.count());
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		deleteDefaultGraph();

		try (QuerySolutionStream stream = doSelectQuery(CLASS_QUERY)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}
	}

	private QuerySolutionStream doSelectQuery(String queryFmt, Object... args) {
		return new QuerySolutionStream(queryFmt.formatted(args), sparqlUrl);
	}

	private void loadSampleData() {
		Stream.of(RSRCS_TO_LOAD).forEach(
			rsrcName -> RdfResourceLoader.load(rsrcName, this::loadRdf));
	}

	private void loadRdf(String rsrcName, RDFFormat rdfFormat, InputStream input) {
		int status = loadRdf(input, rdfFormat.getMediaType());
		if (status != HttpStatus.OK.value()) {
			throw new HttpException(status, "",
				"Failure inserting RDF from %1$s".formatted(rsrcName));
		}
	}

	private int loadRdf(InputStream in, String mediaType) {
		var request = HttpRequest.newBuilder()
			.uri(URI.create(graphStoreUrl + "?default"))
			.POST(HttpRequest.BodyPublishers.ofInputStream(() -> in))
			.header(HttpHeaders.CONTENT_TYPE, mediaType)
			.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.build();
		return sendRequest(request).statusCode();
	}

//	private String loadStatements(InputStream in, String mediaType) {
//		Publisher<DataBuffer> pub;
//		return WebClient.create(graphStoreUrl)
//			.post()
//			.uri(uriBuilder -> uriBuilder
//				.path("")
//				.queryParam("default", "")
//				.build())
//			.body(BodyInserters.fromDataBuffers(pub))
//			.header(HttpHeaders.CONTENT_TYPE, mediaType)
//			.accept(MediaType.ALL)
//			.acceptCharset(StandardCharsets.UTF_8)
//			.exchangeToMono(response -> {
//				LOG.info("HTTP response status code: {}", response.rawStatusCode());
//				LOG.info("HTTP response headers: {}", response.headers().asHttpHeaders());
//				if (response.statusCode().equals(HttpStatus.OK)) {
//					return response.bodyToMono(String.class);
//				} else if (response.statusCode().is4xxClientError()) {
//					return Mono.just("Error response");
//				} else {
//					return response.createException().flatMap(Mono::error);
//				}
//			})
//			.block()
//			.toString();
//	}

	private int deleteDefaultGraph() {
		var request = HttpRequest.newBuilder()
			.uri(URI.create(graphStoreUrl + "?default"))
			.DELETE()
			.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.build();
		return sendRequest(request).statusCode();
	}

	private static HttpResponse<String> sendRequest(HttpRequest request) {
		HttpResponse<String> response = HttpClient
									.newHttpClient()
									.sendAsync(request, HttpResponse.BodyHandlers.ofString())
									.join();
		LOG.info("Response body:%n%1$s%n".formatted(response.body()));
		return response;
	}

}
