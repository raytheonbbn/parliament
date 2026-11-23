//FILE ADDED BY CODEX CODING AGENT
package com.bbn.parliament.server.controller;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.parliament.kb_graph.Constants;
import com.bbn.parliament.kb_graph.KbGraphStore;
import com.bbn.parliament.kb_graph.index.Index;
import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.IndexFactory;
import com.bbn.parliament.kb_graph.index.IndexFactoryRegistry;
import com.bbn.parliament.kb_graph.index.IndexManager;
import com.bbn.parliament.kb_graph.query.PrefixRegistry;
import com.bbn.parliament.server.graph.ModelManager;
import com.bbn.parliament.server.util.ConcurrentRequestController;
import com.bbn.parliament.server.util.ConcurrentRequestLock;
import com.bbn.parliament.server.util.ExplorerUtil;
import com.bbn.parliament.server.util.NTriplesUtil;

@RestController
@RequestMapping("/api")
public class AppDataController {
	private static final int EXPLORER_INDEX_LIMIT = 20;

	@GetMapping("/metadata")
	public Map<String, Object> getMetadata() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("prefixes", PrefixRegistry.getInstance().getPrefixes());
		result.put("graphs", getGraphs());
		result.put("memory", getMemory());
		result.put("propertyFunctionNamespace", Constants.PFUNCTION_NS);
		return result;
	}

	@GetMapping("/memory")
	public Map<String, Object> getMemoryEndpoint() {
		return getMemory();
	}

	@GetMapping("/indexes")
	public Map<String, Object> getIndexesEndpoint() {
		return getIndexes();
	}

	@GetMapping("/explorer-index")
	public Map<String, Object> getExplorerIndexEndpoint() {
		return getExplorerIndex();
	}

	@GetMapping("/explorer")
	public Map<String, Object> getExplorerEndpoint(
			@RequestParam(value = "value", required = false) String valueStr,
			@RequestParam(value = "useLabels", required = false) String useLabelsStr) {
		return getExplorer(valueStr, "yes".equalsIgnoreCase(useLabelsStr));
	}

	private static Map<String, Object> getMemory() {
		MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("heapUsedKb", memBean.getHeapMemoryUsage().getUsed() / 1024);
		result.put("heapMaxKb", memBean.getHeapMemoryUsage().getMax() / 1024);
		result.put("nonHeapUsedKb", memBean.getNonHeapMemoryUsage().getUsed() / 1024);
		result.put("nonHeapMaxKb", memBean.getNonHeapMemoryUsage().getMax() / 1024);
		return result;
	}

	private static List<Map<String, Object>> getGraphs() {
		Model defaultModel = ModelManager.inst().getDefaultModel();
		List<String> graphNames = new ArrayList<>(ModelManager.inst().getSortedModelNames());
		graphNames.add(0, null);

		List<Map<String, Object>> result = new ArrayList<>(graphNames.size());
		for (String graphName : graphNames) {
			boolean isDefault = graphName == null || graphName.isEmpty();
			String uri = isDefault ? KbGraphStore.DEFAULT_GRAPH_NODE.getURI() : graphName;
			Resource graphResource = ResourceFactory.createResource(uri);
			String label = ExplorerUtil.getLabelForResource(graphResource, defaultModel);

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("uri", uri);
			item.put("name", isDefault ? "" : graphName);
			item.put("label", label);
			item.put("display", isDefault ? label : graphName);
			item.put("defaultGraph", isDefault);
			item.put("value", NTriplesUtil.toNTriplesString(graphResource));
			result.add(item);
		}
		return result;
	}

	private static Map<String, Object> getIndexes() {
		List<IndexFactory<?, ?>> factories = IndexFactoryRegistry.getInstance().getFactories();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("factories", factories.stream().map(IndexFactory::getLabel).toList());
		result.put("graphs", getIndexGraphs(factories));
		result.put("propertyFunctionNamespace", Constants.PFUNCTION_NS);
		return result;
	}

	private static List<Map<String, Object>> getIndexGraphs(List<IndexFactory<?, ?>> factories) {
		ModelManager modelManager = ModelManager.inst();
		Model defaultModel = modelManager.getDefaultModel();
		List<String> graphNames = new ArrayList<>(modelManager.getSortedModelNames());
		graphNames.remove(KbGraphStore.MASTER_GRAPH.getURI());
		graphNames.add(0, null);

		List<Map<String, Object>> rows = new ArrayList<>(graphNames.size());
		for (String graphName : graphNames) {
			boolean isDefault = graphName == null || graphName.isEmpty();
			Model model = isDefault ? modelManager.getDefaultModel() : modelManager.getModel(graphName);
			String graphUri = isDefault ? KbGraphStore.DEFAULT_GRAPH_NODE.getURI() : graphName;
			Graph graph = model.getGraph();
			List<Index<?>> indexes = IndexManager.getInstance().getIndexes(graph);

			Map<String, Long> counts = new LinkedHashMap<>();
			for (IndexFactory<?, ?> factory : factories) {
				counts.put(factory.getLabel(), getIndexSize(factory, indexes));
			}

			Resource graphResource = ResourceFactory.createResource(graphUri);
			String display = isDefault
				? ExplorerUtil.getLabelForResource(graphResource, defaultModel)
				: graphName;

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("uri", graphUri);
			row.put("display", display);
			row.put("value", NTriplesUtil.toNTriplesString(graphResource));
			row.put("defaultGraph", isDefault);
			row.put("enabled", !indexes.isEmpty());
			row.put("counts", counts);
			rows.add(row);
		}
		return rows;
	}

	private static Long getIndexSize(IndexFactory<?, ?> factory, List<Index<?>> indexes) {
		for (Index<?> index : indexes) {
			if (factory.equals(IndexManager.getInstance().getIndexFactory(index))) {
				try {
					return index.size();
				} catch (IndexException ex) {
					return null;
				}
			}
		}
		return null;
	}

	private static Map<String, Object> getExplorerIndex() {
		Model model = ModelManager.inst().getDefaultModel();
		ExplorerUtil.BlankNodeLabeler labeler = new ExplorerUtil.BlankNodeLabeler();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("classes", runResourceListQuery(model, """
			PREFIX owl: <http://www.w3.org/2002/07/owl#>
			SELECT DISTINCT ?value WHERE {
				?value a owl:Class .
				FILTER (!isblank(?value))
			} LIMIT %d
			""".formatted(EXPLORER_INDEX_LIMIT), labeler));
		result.put("properties", runResourceListQuery(model, """
			PREFIX owl: <http://www.w3.org/2002/07/owl#>
			SELECT DISTINCT ?value WHERE {
				{ ?value a owl:ObjectProperty }
				UNION
				{ ?value a owl:DatatypeProperty }
			} LIMIT %d
			""".formatted(EXPLORER_INDEX_LIMIT), labeler));
		result.put("graphs", getGraphs());
		return result;
	}

	private static List<Map<String, String>> runResourceListQuery(Model model,
			String query, ExplorerUtil.BlankNodeLabeler labeler) {
		try (QueryExecution execution = QueryExecutionFactory.create(query, model)) {
			List<Map<String, String>> values = new ArrayList<>();
			var results = execution.execSelect();
			while (results.hasNext()) {
				RDFNode node = results.nextSolution().get("value");
				if (node != null && node.isResource()) {
					values.add(toValueDto(node, labeler));
				}
			}
			return values;
		}
	}

	private static Map<String, Object> getExplorer(String valueStr, boolean useLabels) {
		Model defaultGraph = ModelManager.inst().getDefaultModel();
		ExplorerUtil.BlankNodeLabeler labeler = new ExplorerUtil.BlankNodeLabeler();

		RDFNode value = null;
		if (valueStr != null && !valueStr.isBlank()) {
			value = NTriplesUtil.parseValue(valueStr, defaultGraph);
		}

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("value", value == null ? null : toValueDto(value, labeler));
			result.put("useLabels", useLabels);
			result.put("graphs", getGraphs());
			result.put("asSubject", getStatements(value, useLabels, StatementRole.SUBJECT, labeler));
			result.put("asPredicate", getStatements(value, useLabels, StatementRole.PREDICATE, labeler));
			result.put("asObject", getStatements(value, useLabels, StatementRole.OBJECT, labeler));
			result.put("inGraph", getStatementsInGraph(value, useLabels, labeler));
			return result;
		}
	}

	private static List<Map<String, Object>> getStatements(RDFNode value,
			boolean useLabels, StatementRole role, ExplorerUtil.BlankNodeLabeler labeler) {
		if (value == null) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> statements = new ArrayList<>();
		for (String graphName : graphNamesWithDefault()) {
			Model model = graphName.isEmpty()
				? ModelManager.inst().getDefaultModel()
				: ModelManager.inst().getModel(graphName);
			StmtIterator iter = switch (role) {
				case SUBJECT -> value.isResource()
					? model.listStatements((Resource) value, null, (RDFNode) null)
					: ExplorerUtil.getEmptyStmtIterator();
				case PREDICATE -> value.isResource()
					? model.listStatements(null, value.as(Property.class), (RDFNode) null)
					: ExplorerUtil.getEmptyStmtIterator();
				case OBJECT -> model.listStatements(null, null, value);
			};
			try {
				while (iter.hasNext()) {
					statements.add(toStatementDto(graphName, model, iter.nextStatement(),
						useLabels, labeler, true));
				}
			} finally {
				iter.close();
			}
		}
		return statements;
	}

	private static List<Map<String, Object>> getStatementsInGraph(RDFNode value,
			boolean useLabels, ExplorerUtil.BlankNodeLabeler labeler) {
		if (value == null || !value.isURIResource()) {
			return Collections.emptyList();
		}

		Resource resource = value.asResource();
		Model model = null;
		if (KbGraphStore.DEFAULT_GRAPH_NODE.getURI().equals(resource.getURI())) {
			model = ModelManager.inst().getDefaultModel();
		} else if (ModelManager.inst().containsModel(resource.getURI())) {
			model = ModelManager.inst().getModel(resource.getURI());
		}
		if (model == null) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> statements = new ArrayList<>();
		StmtIterator iter = model.listStatements(null, null, (RDFNode) null);
		try {
			while (iter.hasNext()) {
				statements.add(toStatementDto(null, model, iter.nextStatement(),
					useLabels, labeler, false));
			}
		} finally {
			iter.close();
		}
		return statements;
	}

	private static List<String> graphNamesWithDefault() {
		List<String> graphNames = new ArrayList<>(ModelManager.inst().getSortedModelNames());
		graphNames.add(0, "");
		return graphNames;
	}

	private static Map<String, Object> toStatementDto(String graphName, Model model,
			Statement statement, boolean useLabels, ExplorerUtil.BlankNodeLabeler labeler,
			boolean includeGraph) {
		Map<String, Object> result = new LinkedHashMap<>();
		if (includeGraph) {
			result.put("graph", graphDto(graphName, useLabels));
		}
		result.put("subject", toValueDto(statement.getSubject(), model, useLabels, labeler));
		result.put("predicate", toValueDto(statement.getPredicate(), model, useLabels, labeler));
		result.put("object", toValueDto(statement.getObject(), model, useLabels, labeler));
		return result;
	}

	private static Map<String, String> graphDto(String graphName, boolean useLabels) {
		boolean isDefault = graphName == null || graphName.isEmpty();
		String uri = isDefault ? KbGraphStore.DEFAULT_GRAPH_NODE.getURI() : graphName;
		Resource resource = ResourceFactory.createResource(uri);
		String display = isDefault ? ExplorerUtil.getLabelForResource(resource,
			ModelManager.inst().getDefaultModel()) : graphName;
		if (!isDefault && useLabels) {
			String label = ExplorerUtil.getLabelForResource(resource,
				ModelManager.inst().getDefaultModel());
			if (label != null) {
				display = label;
			}
		}
		return toValueDto(resource, display);
	}

	private static Map<String, String> toValueDto(RDFNode node,
			ExplorerUtil.BlankNodeLabeler labeler) {
		return toValueDto(node, null, false, labeler);
	}

	private static Map<String, String> toValueDto(RDFNode node, Model labelModel,
			boolean useLabels, ExplorerUtil.BlankNodeLabeler labeler) {
		String display = ExplorerUtil.getDisplayString(node, labeler);
		if (useLabels && node.isResource() && labelModel != null) {
			String label = ExplorerUtil.getLabelForResource(node.asResource(), labelModel);
			if (label != null) {
				display = label;
			}
		}
		return toValueDto(node, display);
	}

	private static Map<String, String> toValueDto(RDFNode node, String display) {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("display", display);
		result.put("value", NTriplesUtil.toNTriplesString(node));
		result.put("type", node.isLiteral() ? "literal" : node.isAnon() ? "blank" : "uri");
		return result;
	}

	private enum StatementRole {
		SUBJECT,
		PREDICATE,
		OBJECT
	}
}
