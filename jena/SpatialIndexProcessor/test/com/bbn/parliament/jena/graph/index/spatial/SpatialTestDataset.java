package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geo;
import com.bbn.parliament.jena.graph.index.spatial.standard.StdConstants;
import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jena.joseki.client.StreamUtil;
import com.bbn.parliament.jena.query.QueryTestUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

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
		Arrays.stream(COMMON_PREFIXES)
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
			.filter(graphName -> !KbGraphStore.MASTER_GRAPH.equals(graphName.getURI()))
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

	public void loadData(String fileName) {
		RDFFormat dataFormat = RDFFormat.parseFilename(fileName);
		FileManager.get().readModel(model, fileName, dataFormat.toString());
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

	public CloseableQueryExec performQuery(String queryWithoutPrefixes) {
		ParameterizedSparqlString pss = new ParameterizedSparqlString(queryWithoutPrefixes, PFX_MAP);
		return new CloseableQueryExec(graphStore.toDataset(), pss.asQuery());
	}

	public static void checkResults(CloseableQueryExec qexec, String... expectedResultQNames) {
		Set<String> expectedResultSet = Arrays.stream(expectedResultQNames)
			.map(qname -> PFX_MAP.expandPrefix(qname))
			.collect(Collectors.toSet());
		Set<String> actualResultSet = StreamUtil.asStream(qexec.execSelect())
			.map(qs -> qs.get("a"))
			.filter(node -> node != null && node.isURIResource())
			.map(node -> node.asResource().getURI())
			.collect(Collectors.toSet());

		SortedSet<String> expectedMinusActual = new TreeSet<>(expectedResultSet);
		expectedMinusActual.removeAll(actualResultSet);
		if (expectedMinusActual.size() > 0) {
			LOG.warn("Expected results that were not found:  {}", expectedMinusActual);
		}

		SortedSet<String> actualMinusExpected = new TreeSet<>(actualResultSet);
		actualMinusExpected.removeAll(expectedResultSet);
		if (actualMinusExpected.size() > 0) {
			LOG.warn("Actual results that were not expected:  {}", actualMinusExpected);
		}

		assertEquals(expectedResultSet.size(), actualResultSet.size());
		assertEquals(0, expectedMinusActual.size());
		assertEquals(0, actualMinusExpected.size());
	}

	public void runTest(String queryFile, String resultFile) {
		ResultSet expectedResultSet = QueryTestUtil.loadResultSet(resultFile);
		Query query = QueryFactory.read(queryFile, Syntax.syntaxARQ);

		long start = System.currentTimeMillis();
		try (CloseableQueryExec qexec = new CloseableQueryExec(graphStore.toDataset(), query)) {
			ResultSet actualResultSet = qexec.execSelect();
			LOG.debug("Query time to first result: {} ms", (System.currentTimeMillis() - start));

			StringBuilder message = new StringBuilder();
			message.append(String.format("%nResult sets are not equal:%n%n"));
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
}
