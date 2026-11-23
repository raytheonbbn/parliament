package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.sparql_query_builder.QueryBuilder;

public class RdfPropertyTools {
	private static final Logger LOG = LoggerFactory.getLogger(RdfPropertyTools.class);

	private final EntityFactory entFact;
	private final Map<Resource, PropInfo> allProperties;
	private final Map<String, Resource> allPropertiesByLocalName;
	private final Map<String, Resource> jsonFieldToRdfPropMap;
	private final Map<ClassPropPair, String> rdfClassPropToJsonFieldMap;

	public RdfPropertyTools(EntityFactory entityFactory) {
		entFact = ArgCheck.throwIfNull(entityFactory, "entityFactory");
		Map<Resource, PropInfo> tmpAllPropMap = new TreeMap<>(Comparator.comparing(Resource::getURI));
		entFact.kbSink().runSelectQuery(qs -> processAllPropQueryResult(qs, tmpAllPropMap),
			QueryBuilder.fromRsrc("odda/AllProperties.sparql", entFact.prefixMapping()).asQuery());
		allProperties = Collections.unmodifiableMap(tmpAllPropMap);
		LOG.info("Prefetched {} properties", allProperties.size());

		allPropertiesByLocalName = Collections.unmodifiableMap(
			allProperties.keySet().stream().collect(Collectors.toMap(
				Resource::getLocalName,	// key mapper
				Function.identity(),		// value mapper
				(v1, v2) -> {				// merge function
					LOG.error("Found multiple RDF properties ({}, {}) with local name '{}'",
						qnameFor(v1), qnameFor(v2), v1.getLocalName());
					return v1;
				},
				TreeMap::new)));			// map supplier

		jsonFieldToRdfPropMap = Collections.unmodifiableMap(
			allProperties.values().stream()
			.filter(pi -> StringUtils.isNotBlank(pi.jsonFieldName()))
			.collect(Collectors.toMap(
				PropInfo::jsonFieldName,	// key mapper
				pi -> pi.prop(),			// value mapper
				(v1, v2) -> {						// merge function
					LOG.error("Found multiple RDF properties ({}, {}) with the same JSON field name",
						qnameFor(v1), qnameFor(v2));
					return v1;
				},
				TreeMap::new)));					// map supplier

		Map<ClassPropPair, String> tmpClsToJsonMap = new TreeMap<>();
		allProperties.values().stream()
			.filter(pi -> StringUtils.isNotBlank(pi.jsonFieldName()))
			.forEach(pi -> pi.domainTypes().forEach(domainType -> tmpClsToJsonMap.put(
				new ClassPropPair(domainType, pi.prop()),
				pi.jsonFieldName())));
		rdfClassPropToJsonFieldMap = Collections.unmodifiableMap(tmpClsToJsonMap);
	}

	private void processAllPropQueryResult(QuerySolution qs, Map<Resource, PropInfo> allProps) {
		Resource prop = qs.getResource("prop");
		Resource propFamily = qs.getResource("propFamily");
		Resource domainType = qs.getResource("domainType");
		if (propFamily == null) {
			LOG.error("Found property '{}' that has no family", qnameFor(prop));
		} else {
			PropInfo propInfo = allProps.computeIfAbsent(prop, key -> new PropInfo(key, propFamily));
			if (!propFamily.equals(propInfo.propFamily())) {
				LOG.error("Found property '{}' that is in multiple families ({} and {})",
					qnameFor(prop), qnameFor(propInfo.propFamily()), qnameFor(propFamily));
			}
			propInfo.addDomainType(domainType);

			String jsonFieldName = QSUtil.getString(qs, "jsonFieldName");
			if (StringUtils.isNotBlank(jsonFieldName)) {
				if (StringUtils.isBlank(propInfo.jsonFieldName())) {
					propInfo.jsonFieldName(jsonFieldName);
				} else if (!jsonFieldName.equals(propInfo.jsonFieldName())) {
					LOG.error("Found property '{}' that has multiple JSON field names ({} and {})",
						qnameFor(prop), propInfo.jsonFieldName(), jsonFieldName);
				}
			}
		}
	}

	public Resource mapJsonFieldToPropIri(String fieldName) {
		Resource prop = jsonFieldToRdfPropMap.get(fieldName);
		return (prop == null)
			? allPropertiesByLocalName.get(fieldName)
			: prop;
	}

	public boolean isDTProp(Resource prop) {
		PropInfo propInfo = allProperties.get(prop);
		return propInfo != null && propInfo.isDTProp();
	}

	public boolean isObjProp(Resource prop) {
		PropInfo propInfo = allProperties.get(prop);
		return propInfo != null && propInfo.isObjProp();
	}

	public PropInfo propertyInfo(String propIri) {
		return propertyInfo(ResourceFactory.createResource(propIri));
	}

	public PropInfo propertyInfo(Resource propIri) {
		return allProperties.get(propIri);
	}

	public String mapPropIriToJsonField(Resource propIri, Set<RdfTypeInfo> entTypes) {
		String result;
		if (LOG.isTraceEnabled()) {
			String typesList = entTypes.stream()
				.map(RdfTypeInfo::iri)
				.map(this::qnameFor)
				.collect(Collectors.joining(", "));
			LOG.trace("propIri = {}, entTypes = {}", qnameFor(propIri), typesList);
		}
		Optional<String> fieldName = entTypes.stream()
			.map(ci -> new ClassPropPair(ci.iri(), propIri))
			.map(rdfClassPropToJsonFieldMap::get)
			.filter(Objects::nonNull)
			.findFirst();
		if (LOG.isTraceEnabled()) {
			LOG.trace("{} propIri in rdfClassPropToJsonFieldMap",
				fieldName.isPresent() ? "Found" : "Did not find");
		}
		result = fieldName.orElse(propIri.getLocalName());
		LOG.trace("JSON field name = '{}'", result);
		return result;
	}

	private String qnameFor(Resource iri) {
		return entFact.qName().qnameFor(iri);
	}
}
