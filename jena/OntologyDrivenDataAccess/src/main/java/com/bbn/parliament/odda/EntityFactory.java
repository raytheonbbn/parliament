package com.bbn.parliament.odda;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.sparql_query_assembly.QueryBuilder;
import com.google.gson.stream.JsonReader;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;

public class EntityFactory {
	private static final Logger LOG = LoggerFactory.getLogger(EntityFactory.class);

	private final SparqlEndpointSink kbSink;
	private final OntologyTools ontTools;

	public EntityFactory(Resource rootEntityType, PrefixMapping prefixMapping, SparqlEndpointSink sparqlEndpointSink) {
		kbSink = sparqlEndpointSink;
		ontTools = new OntologyTools(rootEntityType, prefixMapping, kbSink);
	}

	public Map<Resource, Entity> fetchEntities(Query entitySubQuery) {
		Map<Resource, EntityImpl> result = new TreeMap<>();
		Instant startTime = Instant.now();

		int rowCount = kbSink.runSelectQuery(qs -> processEntities(qs, result),
			QueryBuilder
				.fromRsrc("odda/GenericEntity.sparql", ontTools.getPrefixMapping())
				.prependSubQuery(entitySubQuery)
				.asQuery());
		if (LOG.isErrorEnabled()) {
			Duration duration = Duration.between(startTime, Instant.now());
			LOG.error(String.format(
				"Entity query retrieved %1$d entities (%2$d result set rows) in %3$.3f seconds",
				result.size(), rowCount, duration.toMillis() / 1000.0));
		}
		return new TreeMap<>(result); // Allocate a new map to up-cast from EntityImpl to Entity
	}

	private void processEntities(QuerySolution qs, Map<Resource, EntityImpl> result) {
		Resource entUri = qs.getResource("ent");
		EntityImpl ent = result.computeIfAbsent(entUri, key -> new EntityImpl(key, ontTools));
		Resource propUri = qs.getResource("prop");
		if (RDF.type.equals(propUri)) {
			ent.getType().addValue(qs.getResource("propValue"));
		} else {
			RDFNode node = qs.get("propValue");
			if (node == null) {
				LOG.warn("Null ?propValue in entity query (?ent = '{}', >prop = '{}')",
					ent.getUri(), propUri);
			} else if (node.isAnon()) {
				LOG.warn("Blank node ?propValue in entity query (?ent = '{}', ?prop = '{}')",
					ent.getUri(), propUri);
			} else if (node.isLiteral()) {
				ent.getDTProp(propUri).addValue(node.asLiteral());
			} else {
				EntityImpl targetEnt = result.computeIfAbsent(node.asResource(),
					key -> new EntityImpl(key, ontTools));
				ent.getObjProp(propUri).addValue(targetEnt);
			}
		}
	}

	public Entity read(JsonReader rdr) {
		return GsonUtil.create(ontTools).fromJson(rdr, Entity.class);
	}

	public OntologyTools getOntTools() {
		return ontTools;
	}
}
