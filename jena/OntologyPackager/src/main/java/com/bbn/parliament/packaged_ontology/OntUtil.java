package com.bbn.parliament.packaged_ontology;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;

public class OntUtil {
	private static final String ONT_RSRC = "Ontology-MachineReadable.ttl";
	private static final Pattern URI_PATTERN = Pattern.compile(
		"^(?:(?:http)|(?:https)|(?:tag)|(?:urn)):.*$");
	private static final Model MODEL;
	private static final InfModel INF_MODEL;
	private static final String SPARQL_PREFIXES;

	static {
		Model m = null;
		String sparqlPrefixes = null;
		Lang lang = RDFLanguages.filenameToLang(ONT_RSRC);
		if (lang != null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try (
				InputStream is = cl.getResourceAsStream(ONT_RSRC);
			) {
				if (is != null) {
					m = ModelFactory.createDefaultModel().read(is, null, lang.getName());
					sparqlPrefixes = new TreeMap<>(m.getNsPrefixMap()).entrySet().stream()
						.map(entry -> String.format("prefix %1$s: <%2$s>%n", entry.getKey(), entry.getValue()))
						.collect(Collectors.joining("", "", String.format("%n%n")));
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		MODEL = m;
		INF_MODEL = ModelFactory.createRDFSModel(m);
		SPARQL_PREFIXES = sparqlPrefixes;
	}

	private OntUtil() {}	// prevents instantiation

	public static PrefixMapping getPrefixMapping() {
		return MODEL;
	}

	public static Model getOntModel() {
		return MODEL;
	}

	public static InfModel getOntInfModel() {
		return INF_MODEL;
	}

	public static ParameterizedSparqlString getPss() {
		return new ParameterizedSparqlString(MODEL);
	}

	public static ParameterizedSparqlString getPss(String query) {
		return new ParameterizedSparqlString(query, MODEL);
	}

	public static ParameterizedSparqlString getPssFromRsrc(String queryRsrc, Class<?> rsrcCls) {
		return getPssFromRsrc(queryRsrc, rsrcCls.getResourceAsStream(queryRsrc));
	}

	public static ParameterizedSparqlString getPssFromRsrc(String queryRsrc) {
		return getPssFromRsrc(queryRsrc,
			Thread.currentThread().getContextClassLoader().getResourceAsStream(queryRsrc));
	}

	private static ParameterizedSparqlString getPssFromRsrc(String queryRsrc, InputStream rsrcIS) {
		try (InputStream is = rsrcIS) {
			if (is == null) {
				throw new FileNotFoundException(String.format(
					"Unable to locate resource '%1$s'", queryRsrc));
			}
			try (
				Reader isRdr = new InputStreamReader(is, StandardCharsets.UTF_8);
				BufferedReader bRdr = new BufferedReader(isRdr);
			) {
				String query = bRdr.lines().collect(Collectors.joining(System.lineSeparator()));
				return new ParameterizedSparqlString(query, MODEL);
			}
		} catch (IOException ex) {
			// Since this method is reading resources embedded in the war file, this
			// exception only happens as the result of a programming error, and will
			// presumably be detected during implementation and testing. Thus, we
			// convert it to a runtime exception under the assumption that it really
			// won't happen in practice in a real deployment.
			throw new UncheckedIOException(ex);
		}
	}

	public static String getSparqlPrefixes() {
		return SPARQL_PREFIXES;
	}

	public static Map<String, String> getPrefixMap() {
		return MODEL.getNsPrefixMap();
	}

	public static String expandPrefix(String qName) {
		return (qName == null || qName.isEmpty() || URI_PATTERN.matcher(qName).matches())
			? qName : MODEL.expandPrefix(qName);
	}

	public static String qnameFor(Resource uri) {
		return (uri == null)
			? null : qnameFor(uri.getURI());
	}

	public static String qnameFor(String uri) {
		return (uri == null || uri.isEmpty() || !URI_PATTERN.matcher(uri).matches())
			? uri : MODEL.shortForm(uri);
	}

	public static void addPrefixMappingsTo(PrefixMapping destinationPM) {
		MODEL.setNsPrefixes(destinationPM);
	}

	public static boolean areUrisEqual(String lhs, String rhs) {
		return Objects.equals(expandPrefix(lhs), expandPrefix(rhs));
	}

	public static long execOntSelect(String queryStr, InferenceOption infOpt, Consumer<QuerySolution> qsConsumer) {
		return execOntSelect(QueryFactory.create(queryStr), infOpt, qsConsumer);
	}

	public static long execOntSelect(ParameterizedSparqlString pss, InferenceOption infOpt, Consumer<QuerySolution> qsConsumer) {
		return execOntSelect(pss.asQuery(), infOpt, qsConsumer);
	}

	public static long execOntSelect(Query query, InferenceOption infOpt, Consumer<QuerySolution> qsConsumer) {
		long count = 0;
		Model model = (infOpt == InferenceOption.WITH) ? INF_MODEL : MODEL;
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet rs = qe.execSelect();
			while (rs.hasNext()) {
				qsConsumer.accept(rs.next());
				++count;
			}
		}
		return count;
	}

	public static Stream<QuerySolution> execOntSelect(String queryStr, InferenceOption infOpt) {
		return execOntSelect(QueryFactory.create(queryStr), infOpt);
	}

	public static Stream<QuerySolution> execOntSelect(ParameterizedSparqlString pss, InferenceOption infOpt) {
		return execOntSelect(pss.asQuery(), infOpt);
	}

	public static Stream<QuerySolution> execOntSelect(Query query, InferenceOption infOpt) {
		Model model = (infOpt == InferenceOption.WITH) ? INF_MODEL : MODEL;
		return new CloseableQuerySolutionStream(QueryExecutionFactory.create(query, model));
	}
}
