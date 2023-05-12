package com.bbn.parliament.kb_graph.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.resultset.RDFInput;
import org.apache.jena.sparql.resultset.ResultSetCompare;

public class QueryTestUtil {
	private QueryTestUtil() {}	// prevents instantiation

	private static ResultSetRewindable makeUnique(ResultSet results) {
		// VERY crude.  Utilizes the fact that bindings have value equality.
		Set<Binding> seen = new LinkedHashSet<>();
		while (results.hasNext()) {
			seen.add(results.nextBinding());
		}
		QueryIterator qIter = new QueryIterPlainWrapper(seen.iterator());
		Model m = ModelFactory.createDefaultModel();
		ResultSet rs = new ResultSetStream(results.getResultVars(), m, qIter);
		return ResultSetFactory.makeRewindable(rs);
	}

	public static boolean equals(ResultSetRewindable expected, ResultSetRewindable actual,
			Query query, StringBuilder message) {
		if (query.isReduced()) {
			expected = makeUnique(expected);
			actual = makeUnique(actual);
		}
		boolean matches = query.isOrdered()
			? ResultSetCompare.equalsByValueAndOrder(expected, actual)
			: ResultSetCompare.equalsByValue(expected, actual);
		if (!matches) {
			expected.reset();
			actual.reset();
			message.append("Expected:%n%1$s%n".formatted(ResultSetFormatter.asText(expected)));
			message.append("Actual:%n%1$s".formatted(ResultSetFormatter.asText(actual)));
		}
		return matches;
	}

	public static ResultSetRewindable loadResultSet(String resultSet) {
		if (resultSet.toLowerCase().endsWith("srx")) {
			try (InputStream in = getResource(resultSet)) {
				return ResultSetFactory.makeRewindable(ResultSetFactory.fromXML(in));
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		} else {
			return ResultSetFactory.makeRewindable(RDFInput.fromRDF(loadModel(resultSet, null)));
		}
	}

	public static Model loadModel(String resource, String base) {
		Model model = ModelFactory.createDefaultModel();
		var lang = RDFLanguages.resourceNameToLang(resource);
		if (lang == null && resource.toLowerCase().endsWith(".rq")) {
			lang = Lang.TURTLE;
		} else if (lang == null) {
			throw new IllegalArgumentException("Unrecognized file extension: %1$s".formatted(resource));
		}
		try (InputStream in = getResource(resource)) {
			model.read(in, base, lang.getName());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return model;
	}

	public static void loadResource(String resource, Graph graph) {
		var lang = RDFLanguages.resourceNameToLang(resource);
		Model model = ModelFactory.createModelForGraph(graph);
		try (InputStream in = getResource(resource)) {
			model.read(in, null, lang.getName());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static void loadResource(String resource, Class<?> cls, Graph graph) {
		var lang = RDFLanguages.resourceNameToLang(resource);
		Model model = ModelFactory.createModelForGraph(graph);
		try (InputStream in = getResource(resource, cls)) {
			model.read(in, null, lang.getName());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static InputStream getResource(String resource) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream strm = cl.getResourceAsStream(resource);
		if (strm == null) {
			throw new MissingResourceException("Resource not found", null, resource);
		}
		return strm;
	}

	public static InputStream getResource(String resource, Class<?> cls) {
		InputStream strm = cls.getResourceAsStream(resource);
		if (strm == null) {
			throw new MissingResourceException("Resource not found", cls.getName(), resource);
		}
		return strm;
	}
}
