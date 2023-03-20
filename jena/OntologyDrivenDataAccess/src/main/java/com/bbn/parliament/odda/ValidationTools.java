package com.bbn.parliament.odda;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bbn.parliament.misc_needing_refactor.QName;
import com.bbn.parliament.sparql_query_assembly.QueryBuilder;
import com.bbn.parliament.util.CloseableQueryExecution;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;

public class ValidationTools {
	private final SparqlEndpointSink kbSink;
	private final PrefixMapping pm;
	private final Map<CardinalityKey, Cardinality> cardinalityCache;

	public ValidationTools(SparqlEndpointSink sparqlEndpointSink, PrefixMapping prefixMapping) {
		kbSink = sparqlEndpointSink;
		pm = prefixMapping;
		cardinalityCache = new ConcurrentSkipListMap<>();
	}

	public Cardinality getCardinality(Model model, Entity owner, Resource propUri) {
		Cardinality tmpCard = Cardinality.defaultCard();
		Set<RDFNode> uncachedTypes = new TreeSet<>();
		for (RdfTypeInfo ownerType : owner.getType().getValues()) {
			Cardinality card = getCardinalityFromCache(ownerType.getUri(), propUri);
			if (card == null) {
				uncachedTypes.add(ownerType.getUri());
			} else {
				tmpCard = Cardinality.intersection(tmpCard, card);
			}
		}

		if (!uncachedTypes.isEmpty()) {
			execQuery(qs -> processCardinalityQueryResult(qs, propUri), model, QueryBuilder
				.fromRsrc("odda/cardinality.sparql", pm)
				.setValues("subjectType", "subjectTypeMarker", uncachedTypes)
				.setArg("_property", propUri)
				.asQuery());
			for (RDFNode ownerType : uncachedTypes) {
				Cardinality card = getCardinalityFromCache(ownerType.asResource(), propUri);
				if (card != null) {
					tmpCard = Cardinality.intersection(tmpCard, card);
				}
			}
		}
		return tmpCard;
	}

	private Cardinality getCardinalityFromCache(Resource typeUri, Resource propUri) {
		return cardinalityCache.get(new CardinalityKey(typeUri, propUri));
	}

	private void processCardinalityQueryResult(QuerySolution qs, Resource propUri) {
		Resource type = qs.getResource("subjectType");
		Long min = QSUtil.getInteger(qs, "min");
		Long max = QSUtil.getInteger(qs, "max");
		Long exact = QSUtil.getInteger(qs, "exact");
		CardinalityKey cardKey = new CardinalityKey(type, propUri);
		Cardinality card = cardinalityCache.getOrDefault(cardKey, Cardinality.defaultCard());
		if (min != null) {
			card = Cardinality.intersection(card, min, Long.MAX_VALUE);
		}
		if (max != null) {
			card = Cardinality.intersection(card, 8, max);
		}
		if (exact != null) {
			card = Cardinality.intersection(card, exact, exact);
		}
		cardinalityCache.put(cardKey, card);
	}

	public boolean isDomainValid(List<String> validationErrors, Model model, Resource entUri, Resource propUri) {
		boolean result = true;
		if (propUri != null) {
			int rowCount = execQuery(
				qs -> processDomainQueryResult(qs, validationErrors, entUri, propUri),
				model, QueryBuilder
					.fromRsrc("odda/domain.sparql", pm)
					.setArg("_property", propUri)
					.setArg("_entity", entUri)
					.asQuery());
			if (rowCount > 0) {
				result = false;
			}
		}
		return result;
	}

	private static void processDomainQueryResult(QuerySolution qs, List<String> validationErrors, Resource entUri, Resource propUri) {
		Resource violatedDomainClass = qs.getResource("violatedDomain");
		if (validationErrors != null) {
			validationErrors.add(String.format("Domain violation on property %1$s: %2$s is not of type %3$s",
				QName.asQName(propUri), QName.asQName(entUri), QName.asQName(violatedDomainClass)));
		}
	}

	public boolean isRangeValid(Resource objectUri, List<String> validationErrors, Model model, Entity entity, Resource propUri) {
		boolean result = true;
		if (propUri != null) {
			List<RDFNode> ownerTypes = entity.getType().stream()
				.map(RdfTypeInfo::getUri)
				.collect(Collectors.toList());
			int rowCount = execQuery(
				qs -> processRangeQueryResult(qs, validationErrors, objectUri, entity.getUri(), propUri),
				model, QueryBuilder
					.fromRsrc("odda/range.sparql", pm)
					.setArg("_property", propUri)
					.setArg("_object", objectUri)
					.setValues("subjectType", "subjectTypeMarker", ownerTypes)
					.asQuery());
			if (rowCount > 0) {
				result = false;
			}
		}
		return result;
	}

	private static void processRangeQueryResult(QuerySolution qs, List<String> validationErrors,
		Resource objectUri, Resource entUri, Resource propUri) {
		Resource violatedRangeClass = qs.getResource("violatedRange");
		if (validationErrors != null) {
			validationErrors.add(String.format(
				"Range violation on property %1$s with subject %2$s: Object %3$s is not of type %4$s",
				QName.asQName(propUri), QName.asQName(entUri), QName.asQName(objectUri), QName.asQName(violatedRangeClass)));
		}
	}

	private int execQuery(Consumer<QuerySolution> consumer, Model model, Query query) {
		int rowCount = 0;
		if (model == null) {
			return kbSink.runSelectQuery(consumer, query);
		} else {
			try (CloseableQueryExecution queryExec = new CloseableQueryExecution(query, model)) {
				ResultSet results = queryExec.execSelect();
				while (results.hasNext()) {
					consumer.accept(results.next());
					++rowCount;
				}
			}
			return rowCount;
		}
	}
}
