package com.bbn.parliament.odda;

import java.util.Map;
import java.util.TreeMap;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;

import com.bbn.parliament.sparql_query_builder.QueryBuilder;

public class RdfTypeTools {
	private final Map<Resource, RdfTypeInfo> rdfTypeMap;

	public RdfTypeTools(EntityFactory entityFactory) {
		rdfTypeMap = new TreeMap<>();
		entityFactory.kbSink().runSelectQuery(this::processTypeHierarchyQueryResult, QueryBuilder
			.fromRsrc("odda/TypeHierarchy.sparql", entityFactory.prefixMapping())
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

	private RdfTypeInfo getOrCreateTypeInfo(QuerySolution qs, String iriVarName, String labelVarName) {
		Resource iri = qs.getResource(iriVarName);
		String label = QSUtil.getString(qs, labelVarName);
		return (iri == null)
			? null
			: rdfTypeMap.computeIfAbsent(iri, key -> new RdfTypeInfo(key, label));
	}

	public RdfTypeInfo typeInfo(Resource typeIri) {
		return rdfTypeMap.get(typeIri);
	}
}
