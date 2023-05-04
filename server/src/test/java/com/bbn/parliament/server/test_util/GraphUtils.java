package com.bbn.parliament.server.test_util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

import com.bbn.parliament.client.jena.MultiPartBodyPublisherBuilder;
import com.bbn.parliament.client.jena.QuerySolutionStream;
import com.bbn.parliament.client.jena.RDFFormat;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.server.controller.QueryController;
import com.bbn.parliament.sparql_query_builder.QueryBuilder;

import reactor.core.publisher.Mono;

public class GraphUtils {
	private static final String NG_QUERY = """
			select distinct ?g where {
				graph ?g {}
			}
			""";
	private static final String DEFAULT_COUNT_QUERY = """
		select distinct (count(distinct *) as ?count) where {
			?s ?p ?o
		}
		""";
	private static final String NAMED_COUNT_QUERY = """
		select distinct (count(distinct *) as ?count) where {
			graph ?_g { ?s ?p ?o }
		}
		""";
	private static final Logger LOG = LoggerFactory.getLogger(GraphUtils.class);

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
		return getReponseAsString(request).body();
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

	public static Set<String> getAvailableNamedGraphs(String sparqlUrl) {
		try (var stream = new QuerySolutionStream(NG_QUERY, sparqlUrl)) {
			return stream
				.map(qs -> qs.getResource("g"))
				.map(Resource::asNode)
				.filter(g -> !KbGraphStore.MASTER_GRAPH.equals(g))
				.map(Node::getURI)
				.collect(Collectors.toSet());
		}
	}

	public static Map<String, Long> getGraphCounts(String sparqlUrl) {
		var result = getAvailableNamedGraphs(sparqlUrl).stream()
			.map(ng -> Pair.of(ng, getNamedGraphStatementCount(ng, sparqlUrl)))
			.collect(Collectors.toMap(
				pair -> pair.getLeft(),		// key mapper
				pair -> pair.getRight()));	// value mapper
		result.put("", getDefaultGraphStatementCount(sparqlUrl));
		return result;
	}

	public static long getDefaultGraphStatementCount(String sparqlUrl) {
		try (var stream = new QuerySolutionStream(DEFAULT_COUNT_QUERY, sparqlUrl)) {
			return stream
				.map(qs -> qs.getLiteral("count"))
				.map(lit -> lit.getLong())
				.findFirst()
				.orElse(0L);
		}
	}

	public static long getNamedGraphStatementCount(String graphIri, String sparqlUrl) {
		var query = QueryBuilder.fromString(NAMED_COUNT_QUERY)
			.setIriArg("_g", graphIri)
			.asQuery();
		try (var stream = new QuerySolutionStream(query, sparqlUrl)) {
			return stream
				.map(qs -> qs.getLiteral("count"))
				.map(lit -> lit.getLong())
				.findFirst()
				.orElse(0L);
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
				System.out.format("HTTP response status code: %1$d%n", response.statusCode().value());
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

	public static int insertStatements(String graphStoreUrl, String stmt, RDFFormat format,
			String graphName) throws IOException {
		try (InputStream is = new ByteArrayInputStream(stmt.getBytes(StandardCharsets.UTF_8))) {
			return insertStatements(graphStoreUrl, is, format, graphName);
		}
	}

	public static int insertStatements(String graphStoreUrl, InputStream in,
			RDFFormat format, String graphName) {
		return insertStatements(graphStoreUrl, graphName,
			HttpRequest.BodyPublishers.ofInputStream(() -> in), format.getMediaType());
	}

	public static int insertStatements(String graphStoreUrl, String graphName, File file)
			throws IOException {
		return insertStatements(graphStoreUrl, graphName,
			HttpRequest.BodyPublishers.ofFile(file.toPath()),
			RDFFormat.parseFilename(file).getMediaType());
	}

	private static int insertStatements(String graphStoreUrl, String graphName,
			BodyPublisher bp, String mediaType) {
		var request = HttpRequest.newBuilder()
			.uri(getRequestUrl(graphStoreUrl, graphName))
			.POST(bp)
			.header(HttpHeaders.CONTENT_TYPE, mediaType)
			.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.build();
		return getReponseAsString(request).statusCode();
	}

	public static int insertStatements(String graphStoreUrl, String graphName, List<File> filesToLoad) {
		var builder = new MultiPartBodyPublisherBuilder();
		for (var file : filesToLoad) {
			builder = builder.addPart("file", file, f -> RDFFormat.parseFilename(f).getMediaType());
		}
		var request = HttpRequest.newBuilder()
					.uri(getRequestUrl(graphStoreUrl, graphName))
					.POST(builder.build())
					.header(HttpHeaders.CONTENT_TYPE, builder.getRequestContentType())
					.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
					.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
					.build();
		return getReponseAsString(request).statusCode();
	}

	public static int replaceGraph(String graphStoreUrl, String stmt, RDFFormat format,
			String graphName) throws IOException {
		try (InputStream is = new ByteArrayInputStream(stmt.getBytes(StandardCharsets.UTF_8))) {
			return replaceGraph(graphStoreUrl, is, format, graphName);
		}
	}

	public static int replaceGraph(String graphStoreUrl, InputStream in,
			RDFFormat format, String graphName) {
		return replaceGraph(graphStoreUrl, graphName,
			HttpRequest.BodyPublishers.ofInputStream(() -> in), format.getMediaType());
	}

	public static int replaceGraph(String graphStoreUrl, String graphName, File file)
			throws IOException {
		return replaceGraph(graphStoreUrl, graphName,
			HttpRequest.BodyPublishers.ofFile(file.toPath()),
			RDFFormat.parseFilename(file).getMediaType());
	}

	private static int replaceGraph(String graphStoreUrl, String graphName,
			BodyPublisher bp, String mediaType) {
		var request = HttpRequest.newBuilder()
			.uri(getRequestUrl(graphStoreUrl, graphName))
			.PUT(bp)
			.header(HttpHeaders.CONTENT_TYPE, mediaType)
			.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.build();
		return getReponseAsString(request).statusCode();
	}

	public static HttpResponse<InputStream> getStatements(String graphStoreUrl, String graphName, RDFFormat rdfFmt) {
		var request = HttpRequest.newBuilder()
			.uri(getRequestUrl(graphStoreUrl, graphName))
			.header(HttpHeaders.ACCEPT, rdfFmt.getMediaType())
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.GET()
			.build();
		return getReponseAsStream(request);
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
		return getReponseAsString(request).statusCode();
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
		return getReponseAsString(request).statusCode();
	}

	private static HttpResponse<InputStream> getReponseAsStream(HttpRequest request) {
		return HttpClient.newHttpClient()
				.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
				.join();
	}

	private static HttpResponse<String> getReponseAsString(HttpRequest request) {
		HttpResponse<String> response = HttpClient.newHttpClient()
			.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.join();
		if (LOG.isDebugEnabled()) {
			var respBody = response.body();
			if (StringUtils.isBlank(respBody)) {
				LOG.debug("Blank response body");
			} else {
				LOG.debug("Response body: {}", respBody);
			}
		}
		return response;
	}

	private static URI getRequestUrl(String graphStoreUrl, String graphName) {
		var requestUrl = StringUtils.isBlank(graphName)
			? graphStoreUrl + "?default"
			: graphStoreUrl + "?graph=" + URLEncoder.encode(graphName, StandardCharsets.UTF_8);
		return URI.create(requestUrl);
	}
}
