package com.bbn.parliament.odda;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.bbn.parliament.sparql_query_builder.QueryBuilder;

public class ValidationTools {
	private final EntityFactory entFact;
	private final Map<CardinalityKey, Cardinality> cardinalityCache;

	public ValidationTools(EntityFactory entityFactory) {
		entFact = ArgCheck.throwIfNull(entityFactory, "entityFactory");
		cardinalityCache = new ConcurrentSkipListMap<>();
	}

	public Cardinality cardinality(Model model, Entity owner, Resource propIri) {
		Cardinality tmpCard = Cardinality.defaultCard();
		Set<RDFNode> uncachedTypes = new TreeSet<>();
		for (RdfTypeInfo ownerType : owner.type().values()) {
			Cardinality card = cardinalityFromCache(ownerType.iri(), propIri);
			if (card == null) {
				uncachedTypes.add(ownerType.iri());
			} else {
				tmpCard = Cardinality.intersection(tmpCard, card);
			}
		}

		if (!uncachedTypes.isEmpty()) {
			execQuery(qs -> processCardinalityQueryResult(qs, propIri), model, QueryBuilder
				.fromRsrc("odda/cardinality.sparql", entFact.prefixMapping())
				.setValues("subjectType", "subjectTypeMarker", uncachedTypes)
				.setArg("_property", propIri)
				.asQuery());
			for (RDFNode ownerType : uncachedTypes) {
				Cardinality card = cardinalityFromCache(ownerType.asResource(), propIri);
				if (card != null) {
					tmpCard = Cardinality.intersection(tmpCard, card);
				}
			}
		}
		return tmpCard;
	}

	private Cardinality cardinalityFromCache(Resource typeIri, Resource propIri) {
		return cardinalityCache.get(new CardinalityKey(typeIri, propIri));
	}

	private void processCardinalityQueryResult(QuerySolution qs, Resource propIri) {
		Resource type = qs.getResource("subjectType");
		Long min = QSUtil.getInteger(qs, "min");
		Long max = QSUtil.getInteger(qs, "max");
		Long exact = QSUtil.getInteger(qs, "exact");
		CardinalityKey cardKey = new CardinalityKey(type, propIri);
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

	public boolean isDomainValid(List<String> validationErrors, Model model, Resource entIri, Resource propIri) {
		boolean result = true;
		if (propIri != null) {
			int rowCount = execQuery(
				qs -> processDomainQueryResult(qs, validationErrors, entIri, propIri),
				model, QueryBuilder
					.fromRsrc("odda/domain.sparql", entFact.prefixMapping())
					.setArg("_property", propIri)
					.setArg("_entity", entIri)
					.asQuery());
			if (rowCount > 0) {
				result = false;
			}
		}
		return result;
	}

	private void processDomainQueryResult(QuerySolution qs, List<String> validationErrors, Resource entIri, Resource propIri) {
		Resource violatedDomainClass = qs.getResource("violatedDomain");
		if (validationErrors != null) {
			validationErrors.add(String.format("Domain violation on property %1$s: %2$s is not of type %3$s",
				qnameFor(propIri), qnameFor(entIri), qnameFor(violatedDomainClass)));
		}
	}

	public boolean isRangeValid(Resource objectIri, List<String> validationErrors, Model model, Entity entity, Resource propIri) {
		boolean result = true;
		if (propIri != null) {
			List<RDFNode> ownerTypes = entity.type().stream()
				.map(RdfTypeInfo::iri)
				.collect(Collectors.toList());
			int rowCount = execQuery(
				qs -> processRangeQueryResult(qs, validationErrors, objectIri, entity.iri(), propIri),
				model, QueryBuilder
					.fromRsrc("odda/range.sparql", entFact.prefixMapping())
					.setArg("_property", propIri)
					.setArg("_object", objectIri)
					.setValues("subjectType", "subjectTypeMarker", ownerTypes)
					.asQuery());
			if (rowCount > 0) {
				result = false;
			}
		}
		return result;
	}

	private void processRangeQueryResult(QuerySolution qs, List<String> validationErrors,
		Resource objectIri, Resource entIri, Resource propIri) {
		Resource violatedRangeClass = qs.getResource("violatedRange");
		if (validationErrors != null) {
			validationErrors.add(String.format(
				"Range violation on property %1$s with subject %2$s: Object %3$s is not of type %4$s",
				qnameFor(propIri), qnameFor(entIri), qnameFor(objectIri), qnameFor(violatedRangeClass)));
		}
	}

	private int execQuery(Consumer<QuerySolution> consumer, Model model, Query query) {
		int rowCount = 0;
		if (model == null) {
			return entFact.kbSink().runSelectQuery(consumer, query);
		} else {
			try (QueryExecution queryExec = QueryExecutionFactory.create(query, model)) {
				ResultSet results = queryExec.execSelect();
				while (results.hasNext()) {
					consumer.accept(results.next());
					++rowCount;
				}
			}
			return rowCount;
		}
	}

	private String qnameFor(Resource iri) {
		return entFact.qName().qnameFor(iri);
	}
}
