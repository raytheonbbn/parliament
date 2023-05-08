package com.bbn.parliament.kb_graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.client.ResourceUtil;
import com.bbn.parliament.client.StreamUtil;
import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.KbGraphFactory;
import com.bbn.parliament.kb_graph.KbGraphStore;
import com.bbn.parliament.kb_graph.index.IndexFactoryRegistry;
import com.bbn.parliament.kb_graph.index.IndexManager;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary.Geo;
import com.bbn.parliament.kb_graph.index.spatial.standard.StdConstants;
import com.bbn.parliament.kb_graph.query.QueryTestUtil;

public class SpatialTestDataset {
	private static final Logger LOG = LoggerFactory.getLogger(SpatialTestDataset.class);
	private static final String BASE_URI = "http://parliament.semwebcentral.org/spatial/examples";
	private static final String[][] COMMON_PREFIXES = {
		{ "rdf", RDF.getURI() },
		{ "rdfs", RDFS.getURI() },
		{ "owl", OWL.getURI() },
		{ "xsd", XSD.getURI() },
		{ "snap", "http://www.ifomis.org/bfo/1.0/snap#" },
		{ "span", "http://www.ifomis.org/bfo/1.0/span#" },
		{ "time", "http://www.w3.org/2006/time#" },
		{ "georss", StdConstants.GEORSS_NS },
		{ "gml", StdConstants.GML_NS },
		{ "rcc", StdConstants.RCC_EXT_NS },
		{ "ogc", StdConstants.OGC_EXT_NS },
		{ "spatial", StdConstants.BUFFER_NS },
		{ "geof", StdConstants.OGC_FUNCTION_NS },
		{ "geo", Geo.uri },
		{ "example", BASE_URI + "#" },
		{ "example1", BASE_URI + "/example1#" },
		{ "example2", BASE_URI + "/example2#" },
		{ "example3", BASE_URI + "/example3#" },
		{ "cities", BASE_URI + "/cities#" },
	};
	private static final PrefixMapping PFX_MAP;

	public static final String JDBC_URL = "jdbc:postgresql://localhost/spatial_index_test";
	public static final String USERNAME = "spatial";
	public static final String PASSWORD = "data";

	static {
		PFX_MAP = PrefixMapping.Factory.create();
		PFX_MAP.setNsPrefixes(PrefixMapping.Standard);
		Stream.of(COMMON_PREFIXES)
			.forEach(pair -> PFX_MAP.setNsPrefix(pair[0], pair[1]));
	}

	private Properties factoryProps;
	private SpatialIndexFactory indexFactory;
	private Model model;
	private KbGraph graph;
	private KbGraphStore graphStore;

	private SpatialIndex index;

	// Call from @BeforeAll
	public SpatialTestDataset(Properties factoryProperties) {
		factoryProps = factoryProperties;

		indexFactory = new SpatialIndexFactory();
		indexFactory.configure(factoryProps);
		IndexFactoryRegistry.getInstance().register(indexFactory);

		graph = KbGraphFactory.createDefaultGraph();
		graphStore = new KbGraphStore(graph);
		graphStore.initialize();

		model = ModelFactory.createModelForGraph(graph);
		clearKb();
	}

	// Call from @AfterAll
	public void tearDownKb() {
		clearKb();
		model.close();
		graphStore.clear();
	}

	// Call from @BeforeEach
	public final void setupIndex() {
		clearKb();
		index = createIndexForGraph(graph, null);

		loadData("queries/ontology.ttl");

		StreamUtil.asStream(graphStore.listGraphNodes())
			.filter(graphName -> !KbGraphStore.MASTER_GRAPH.equals(graphName))
			.forEach(graphName -> createIndexForGraph(graphStore.getGraph(graphName), graphName));
	}

	// Call from @AfterEach
	public final void removeIndex() {
		IndexManager.getInstance().unregister(graph, null, index);
	}

	private SpatialIndex createIndexForGraph(Graph g, Node gName) {
		SpatialIndex result = indexFactory.createIndex(g, gName);
		IndexManager.getInstance().register(g, gName, indexFactory, result);
		result.open();
		result.clear();
		return result;
	}

	public void clearKb() {
		model.removeAll();
	}

	public void loadData(String rsrcName) {
		var lang = RDFLanguages.resourceNameToLang(rsrcName, Lang.TURTLE);
		try (var is = ResourceUtil.getAsStream(rsrcName)) {
			RDFDataMgr.read(model, is, null, lang);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static void printQuerySolution(QuerySolution querySolution) {
		if (LOG.isInfoEnabled()) {
			LOG.info("QuerySolution:");
			for (Iterator<String> it = querySolution.varNames(); it.hasNext();) {
				String var = it.next();
				LOG.info("{} -> {}", var, querySolution.get(var));
			}
		}
	}

	public QueryExecution performQuery(String queryWithoutPrefixes) {
		return performQuery(getDataset(), queryWithoutPrefixes);
	}

	public static QueryExecution performQuery(Dataset dataset, String queryWithoutPrefixes) {
		ParameterizedSparqlString pss = new ParameterizedSparqlString(queryWithoutPrefixes, PFX_MAP);
		return QueryExecutionFactory.create(pss.asQuery(), dataset);
	}

	public static void checkResults(QueryExecution qexec, String... expectedResultQNames) {
		Set<String> expectedResults = Stream.of(expectedResultQNames)
			.map(PFX_MAP::expandPrefix)
			.collect(Collectors.toCollection(TreeSet::new));
		Set<String> actualResults = StreamUtil.asStream(qexec.execSelect())
			.map(qs -> qs.get("a"))
			.filter(Objects::nonNull)
			.filter(RDFNode::isURIResource)
			.map(RDFNode::asResource)
			.map(Resource::getURI)
			.collect(Collectors.toCollection(TreeSet::new));

		Set<String> expectedMinusActual = setDiff(expectedResults, actualResults);
		Set<String> actualMinusExpected = setDiff(actualResults, expectedResults);

		if (expectedMinusActual.size() != 0 || actualMinusExpected.size() != 0) {
			logUris("Expected results:   ", expectedResults);
			logUris("Actual results:     ", actualResults);
			logUris("Missing results:    ", expectedMinusActual);
			logUris("Unexpected results: ", actualMinusExpected);
			fail("Expected and actual result sets differ");
		}
	}

	private static <E> Set<E> setDiff(Set<E> lhs, Set<E> rhs) {
		SortedSet<E> diff = new TreeSet<>(lhs);
		diff.removeAll(rhs);
		return diff;
	}

	private static void logUris(String msg, Set<String> set) {
		String uris = set.stream()
			.map(PFX_MAP::qnameFor)
			.collect(Collectors.joining(", "));
		LOG.error("{}{}", msg, uris);
	}

	public void runTest(String queryFile, String resultFile) {
		ResultSetRewindable expectedResultSet = QueryTestUtil.loadResultSet(resultFile);
		Query query = QueryFactory.read(queryFile, Syntax.syntaxARQ);

		long start = System.currentTimeMillis();
		try (var qexec = QueryExecutionFactory.create(query, getDataset())) {
			ResultSetRewindable actualResultSet = ResultSetFactory.makeRewindable(qexec.execSelect());
			LOG.debug("Query time to first result: {} ms", (System.currentTimeMillis() - start));

			StringBuilder message = new StringBuilder();
			message.append("%nResult sets are not equal:%n%n".formatted());
			boolean matches = QueryTestUtil.equals(expectedResultSet, actualResultSet, query, message);
			LOG.debug("Query time to last result: {} ms", (System.currentTimeMillis() - start));
			assertTrue(matches, message.toString());
		}
	}

	public SpatialIndex getIndex() {
		return index;
	}

	public Dataset getDataset() {
		return graphStore.toDataset();
	}

	protected static Resource createResource(String qName) {
		return ResourceFactory.createResource(PFX_MAP.expandPrefix(qName));
	}
}
