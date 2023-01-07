package com.bbn.parliament.test_util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.bbn.parliament.jena.joseki.client.QuerySolutionStream;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.spring_boot.controller.QueryController;

import reactor.core.publisher.Mono;

public class GraphUtils {

	private static final Logger LOG = LoggerFactory.getLogger(GraphUtils.class);
	private static final String NG_QUERY = """
			select distinct ?g where {
				graph ?g {}
			}
			""";

	public static QuerySolutionStream doSelectQuery(String sparqlUrl, String queryFmt, Object... args) {
		return new QuerySolutionStream(queryFmt.formatted(args), sparqlUrl);
	}

	public static boolean doAskQuery(String sparqlUrl, String queryFmt, Object... args) {
		String query = queryFmt.formatted(args);
		LOG.debug("askquery: {}", query);
		try (var qe = QueryExecutionFactory.sparqlService(sparqlUrl, query)) {
			return qe.execAsk();
		}
	}

	public static Model doConstructQuery(String sparqlUrl, String queryFmt, Object... args) {
		String query = queryFmt.formatted(args);
		try (var qe = QueryExecutionFactory.sparqlService(sparqlUrl, query)) {
			return qe.execConstruct();
		}
	}

	public static void doUpdate(String updateUrl, String queryFmt, Object... args) {
		UpdateRequest ur = UpdateFactory.create(queryFmt.formatted(args));
		UpdateExecutionFactory.createRemote(ur, updateUrl).execute();
	}

	public static void insert(String updateUrl, String sub, String pred, Node obj, String graphName) {
		QuadDataAcc qd = createQuadData(sub, pred, obj, graphName);
		UpdateDataInsert update = new UpdateDataInsert(qd);
		UpdateExecutionFactory.createRemote(update, updateUrl).execute();
	}

	public static String doSelectToCsv(String sparqlUrl, String query) {
		var request = HttpRequest.newBuilder()
					.uri(URI.create(sparqlUrl))
					.POST(HttpRequest.BodyPublishers.ofString(query))
					.header(HttpHeaders.CONTENT_TYPE, "application/sparql-query")
					.header(HttpHeaders.ACCEPT, "text/csv")
					.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
					.build();
		return sendRequest(request).body();
	}

	public static QuadDataAcc createQuadData(String sub, String pred, Node obj, String graphName) {
		Node s = NodeFactory.createURI(sub);
		Node p = NodeFactory.createURI(pred);
		QuadDataAcc qd = new QuadDataAcc();
		if (StringUtils.isBlank(graphName)) {
			qd.addTriple(new Triple(s, p, obj));
		} else {
			qd.addQuad(new Quad(NodeFactory.createURI(graphName), s, p, obj));
		}
		return qd;
	}

	public static HttpResponse<String> sendRequest(HttpRequest request) {
		HttpResponse<String> response = HttpClient
									.newHttpClient()
									.sendAsync(request, HttpResponse.BodyHandlers.ofString())
									.join();
		LOG.info("Response body:%n%1$s%n".formatted(response.body()));
		return response;
	}

	public static Set<String> getAvailableNamedGraphs(String sparqlUrl) {
		try (QuerySolutionStream stream = doSelectQuery(sparqlUrl, NG_QUERY)) {
			return stream
				.map(qs -> qs.getResource("g"))
				.map(Resource::getURI)
				.filter(uri -> !"http://parliament.semwebcentral.org/parliament#MasterGraph".equals(uri))
				.collect(Collectors.toSet());
		}
	}

	public static String getAvailableNamedGraphsNoJena(String sparqlUrl) {
		return WebClient.create(sparqlUrl)
			.post()
			.uri("")
			.bodyValue(NG_QUERY)
			.header(HttpHeaders.CONTENT_TYPE, QueryController.SPARQL_QUERY)
			.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
			.acceptCharset(StandardCharsets.UTF_8)
			.exchangeToMono(response -> {
				System.out.format("HTTP response status code: %1$d%n", response.rawStatusCode());
				System.out.format("HTTP response headers: %1$s%n", response.headers().asHttpHeaders());
				if (response.statusCode().equals(HttpStatus.OK)) {
					return response.bodyToMono(String.class);
				} else if (response.statusCode().is4xxClientError()) {
					return Mono.just("Error response");
				} else {
					return response.createException().flatMap(Mono::error);
				}
			})
			.block()
			.toString();
	}

	public static void delete(String updateUrl, String sub, String pred, Node obj, String graphName) {
		QuadDataAcc qd = createQuadData(sub, pred, obj, graphName);
		UpdateDataDelete update = new UpdateDataDelete(qd);
		UpdateExecutionFactory.createRemote(update, updateUrl).execute();
	}


	// Graph store protocol methods

	public static int insertStatements(String graphStoreUrl, String stmt, RDFFormat format, String graphName) throws IOException {
		try (InputStream bstrm = new ByteArrayInputStream(stmt.getBytes(StandardCharsets.UTF_8))) {
			return loadRdf(graphStoreUrl, bstrm, format.getMediaType(), graphName);
		}
	}

	public static int insertStatements(String graphStoreUrl, InputStream in, RDFFormat format, String graphName) {
		return loadRdf(graphStoreUrl, in, format.getMediaType(), graphName);
	}

	private static int loadRdf(String graphStoreUrl, InputStream in, String mediaType, String graphName) {
		var request = HttpRequest.newBuilder()
					.uri(getRequestUrl(graphStoreUrl, graphName))
					.POST(HttpRequest.BodyPublishers.ofInputStream(() -> in))
					.header(HttpHeaders.CONTENT_TYPE, mediaType)
					.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
					.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
					.build();
		LOG.debug("insertStatements request.bodyPublisher().isEmpty():"+request.bodyPublisher().get().contentLength());
		return sendRequest(request).statusCode();
	}

	public static int createGraph(String graphStoreUrl, String mediaType, String graphName) {
		var request = HttpRequest.newBuilder()
				.uri(getRequestUrl(graphStoreUrl, graphName))
				.POST(HttpRequest.BodyPublishers.noBody())
				.header(HttpHeaders.CONTENT_TYPE, mediaType)
				.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
				.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
				.build();
		LOG.debug("createGraph request.bodyPublisher():"+request.bodyPublisher().get().contentLength());
		return sendRequest(request).statusCode();
	}

	public static void clearAll(String graphStoreUrl, String sparqlUrl) {
		Set<String> allGraphs = getAvailableNamedGraphs(sparqlUrl);
		allGraphs.add("");
		allGraphs.stream().forEach(uri -> deleteGraph(graphStoreUrl, uri));
	}

	public static int deleteGraph(String graphStoreUrl, String graphName) {
		var request = HttpRequest.newBuilder()
			.uri(getRequestUrl(graphStoreUrl, graphName))
			.DELETE()
			.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.build();
		return sendRequest(request).statusCode();
	}

	private static URI getRequestUrl(String graphStoreUrl, String graphName) {
		var requestUrl = (graphName == null || graphName.isBlank())
			? graphStoreUrl + "?default"
			: graphStoreUrl + "?graph=" + URLEncoder.encode(graphName, StandardCharsets.UTF_8);
		return URI.create(requestUrl);
	}
}
