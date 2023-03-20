package com.bbn.parliament.odda;

import java.util.Map;
import java.util.TreeMap;

import com.bbn.parliament.sparql_query_assembly.QueryBuilder;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;

public class RdfTypeTools {
	private final Map<Resource, RdfTypeInfo> rdfTypeMap;

	public RdfTypeTools(SparqlEndpointSink sparqlEndpointSink, PrefixMapping prefixMapping) {
		rdfTypeMap = new TreeMap<>();
		sparqlEndpointSink.runSelectQuery(this::processTypeHierarchyQueryResult, QueryBuilder
			.fromRsrc("odda/TypeHierarchy.sparql", prefixMapping)
			.asQuery());
	}

	private void processTypeHierarchyQueryResult(QuerySolution qs) {
		RdfTypeInfo subType = getOrCreateTypeInfo(qs, "subType", "subTypeLbl");
		RdfTypeInfo superType = getOrCreateTypeInfo(qs, "superType", "superTypeLbl");
		if (subType != null && superType != null) {
			subType.addDeclaredSuperType(superType);
			superType.addDeclaredSubType(subType);
		}
	}

	private RdfTypeInfo getOrCreateTypeInfo(com.hp.hpl.jena.query.QuerySolution qs, String uriVarName, String labelVarName) {
		Resource uri = qs.getResource(uriVarName);
		String label = QSUtil.getString(qs, labelVarName);
		return (uri == null)
			? null
			: rdfTypeMap.computeIfAbsent(uri, key -> new RdfTypeInfo(key, label));
	}

	public RdfTypeInfo getTypeInfo(Resource typeUri) {
		return rdfTypeMap.get(typeUri);
	}
}
