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
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.misc_needing_refactor.QName;
import com.bbn.parliament.sparql_query_builder.QueryBuilder;

public class RdfPropertyTools {
	private static final Logger LOG = LoggerFactory.getLogger(RdfPropertyTools.class);

	private final Map<Resource, PropInfo> allProperties;
	private final Map<String, Resource> allPropertiesByLocalName;
	private final Map<String, Resource> jsonFieldToRdfPropMap;
	private final Map<ClassPropPair, String> rdfClassPropToJsonFieldMap;

	public RdfPropertyTools(SparqlEndpointSink sparqlEndpointSink, PrefixMapping prefixMapping) {
		Map<Resource, PropInfo> tmpAllPropMap = new TreeMap<>(Comparator.comparing(Resource::getURI));
		sparqlEndpointSink.runSelectQuery(qs -> processAllPropQueryResult(qs, tmpAllPropMap),
			QueryBuilder.fromRsrc("odda/AllProperties.sparql", prefixMapping).asQuery());
		allProperties = Collections.unmodifiableMap(tmpAllPropMap);
		LOG.info("Prefetched {} properties", allProperties.size());

		allPropertiesByLocalName = Collections.unmodifiableMap(
			allProperties.keySet().stream().collect(Collectors.toMap(
				Resource::getLocalName,	// key mapper
				Function.identity(),		// value mapper
				(v1, v2) -> {				// merge function
					LOG.error("Found multiple RDF properties ({}, {}) with local name '{}'",
						QName.asQName(v1), QName.asQName(v2), v1.getLocalName());
					return v1;
				},
				TreeMap::new)));			// map supplier

		jsonFieldToRdfPropMap = Collections.unmodifiableMap(
			allProperties.values().stream()
			.filter(pi -> StringUtils.isNotBlank(pi.getJsonFieldName()))
			.collect(Collectors.toMap(
				PropInfo::getJsonFieldName,	// key mapper
				pi -> pi.getProp(),			// value mapper
				(v1, v2) -> {						// merge function
					LOG.error("Found multiple RDF properties ({}, {}) with the same JSON field name",
						QName.asQName(v1), QName.asQName(v2));
					return v1;
				},
				TreeMap::new)));					// map supplier

		Map<ClassPropPair, String> tmpClsToJsonMap = new TreeMap<>();
		allProperties.values().stream()
			.filter(pi -> StringUtils.isNotBlank(pi.getJsonFieldName()))
			.forEach(pi -> pi.getDomainTypes().forEach(domainType -> tmpClsToJsonMap.put(
				new ClassPropPair(domainType, pi.getProp()),
				pi.getJsonFieldName())));
		rdfClassPropToJsonFieldMap = Collections.unmodifiableMap(tmpClsToJsonMap);
	}

	private static void processAllPropQueryResult(QuerySolution qs, Map<Resource, PropInfo> allProps) {
		Resource prop = qs.getResource("prop");
		Resource propFamily = qs.getResource("propFamily");
		Resource domainType = qs.getResource("domainType");
		if (propFamily == null) {
			LOG.error("Found property '{}' that has no family", QName.asQName(prop));
		} else {
			PropInfo propInfo = allProps.computeIfAbsent(prop, key -> new PropInfo(key, propFamily));
			if (!propFamily.equals(propInfo.getPropFamily())) {
				LOG.error("Found property '{}' that is in multiple families ({} and {})",
					QName.asQName(prop), QName.asQName(propInfo.getPropFamily()), QName.asQName(propFamily));
			}
			propInfo.addDomainType(domainType);

			String jsonFieldName = QSUtil.getString(qs, "jsonFieldName");
			if (StringUtils.isNotBlank(jsonFieldName)) {
				if (StringUtils.isBlank(propInfo.getJsonFieldName())) {
					propInfo.setJsonFieldName(jsonFieldName);
				} else if (!jsonFieldName.equals(propInfo.getJsonFieldName())) {
					LOG.error("Found property '{}' that has multiple JSON field names ({} and {})",
						QName.asQName(prop), propInfo.getJsonFieldName(), jsonFieldName);
				}
			}
		}
	}

	public Resource mapJsonFieldToPropUri(String fieldName) {
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

	public PropInfo getPropertyInfo(String propUri) {
		return getPropertyInfo(ResourceFactory.createResource(propUri));
	}

	public PropInfo getPropertyInfo(Resource propUri) {
		return allProperties.get(propUri);
	}

	public String mapPropUriToJsonField(Resource propUri, Set<RdfTypeInfo> entTypes) {
		String result;
		if (LOG.isTraceEnabled()) {
			String typesList = entTypes.stream()
				.map(RdfTypeInfo::getUri)
				.map(QName::asQName)
				.collect(Collectors.joining(", "));
			LOG.trace("propUri = {}, entTypes = {}", QName.asQName(propUri), typesList);
		}
		Optional<String> fieldName = entTypes.stream()
			.map(ci -> new ClassPropPair(ci.getUri(), propUri))
			.map(rdfClassPropToJsonFieldMap::get)
			.filter(Objects::nonNull)
			.findFirst();
		if (LOG.isTraceEnabled()) {
			LOG.trace("{} propUri in rdfClassPropToJsonFieldMap",
				fieldName.isPresent() ? "Found" : "Did not find");
		}
		result = fieldName.orElse(propUri.getLocalName());
		LOG.trace("JSON field name = '{}'", result);
		return result;
	}
}
