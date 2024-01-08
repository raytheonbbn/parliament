package com.bbn.parliament.odda;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.sparql_query_builder.QueryBuilder;
import com.bbn.parliament.util.QName;
import com.google.gson.stream.JsonReader;

public class EntityFactory {
	private static final Logger LOG = LoggerFactory.getLogger(EntityFactory.class);

	private final Resource rootEntType;
	private final Resource orderIndex;
	private final PrefixMapping pm;
	private final QName qName;
	private final SparqlEndpointSink kbSink;
	private final RdfTypeTools rdfTypeTools;
	private final RdfPropertyTools rdfPropTools;
	private final ValidationTools valTools;

	public EntityFactory(Resource rootEntityType, Resource orderIndexPredicate,
			PrefixMapping prefixMapping, SparqlEndpointSink sparqlEndpointSink) {
		rootEntType = ArgCheck.throwIfNull(rootEntityType, "rootEntityType");
		orderIndex = ArgCheck.throwIfNull(orderIndexPredicate, "orderIndexPredicate");
		pm = ArgCheck.throwIfNull(prefixMapping, "prefixMapping");
		qName = new QName(pm);
		kbSink = ArgCheck.throwIfNull(sparqlEndpointSink, "sparqlEndpointSink");
		rdfTypeTools = new RdfTypeTools(this);
		rdfPropTools = new RdfPropertyTools(this);
		valTools = new ValidationTools(this);
	}

	public Map<Resource, Entity> fetchEntities(Query entitySubQuery) {
		Map<Resource, EntityImpl> result = new TreeMap<>();
		Instant startTime = Instant.now();

		int rowCount = kbSink.runSelectQuery(qs -> processEntities(qs, result),
			QueryBuilder
				.fromRsrc("odda/GenericEntity.sparql", prefixMapping())
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
		Resource entIri = qs.getResource("ent");
		EntityImpl ent = result.computeIfAbsent(entIri, key -> new EntityImpl(key, this));
		Resource propIri = qs.getResource("prop");
		if (RDF.type.equals(propIri)) {
			ent.type().addValue(qs.getResource("propValue"));
		} else {
			RDFNode node = qs.get("propValue");
			if (node == null) {
				LOG.warn("Null ?propValue in entity query (?ent = '{}', >prop = '{}')",
					ent.iri(), propIri);
			} else if (node.isAnon()) {
				LOG.warn("Blank node ?propValue in entity query (?ent = '{}', ?prop = '{}')",
					ent.iri(), propIri);
			} else if (node.isLiteral()) {
				ent.dtProp(propIri).addValue(node.asLiteral());
			} else {
				EntityImpl targetEnt = result.computeIfAbsent(node.asResource(),
					key -> new EntityImpl(key, this));
				ent.objProp(propIri).addValue(targetEnt);
			}
		}
	}

	public Entity read(JsonReader rdr) {
		return GsonUtil.create(this).fromJson(rdr, Entity.class);
	}

	public Resource rootEntityType() {
		return rootEntType;
	}

	public Resource orderIndexPredicate() {
		return orderIndex;
	}

	public PrefixMapping prefixMapping() {
		return pm;
	}

	public QName qName() {
		return qName;
	}

	public SparqlEndpointSink kbSink() {
		return kbSink;
	}

	public RdfTypeTools rdfTypeTools() {
		return rdfTypeTools;
	}

	public RdfPropertyTools rdfPropertyTools() {
		return rdfPropTools;
	}

	public ValidationTools validationTools() {
		return valTools;
	}
}
