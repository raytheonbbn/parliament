package com.bbn.parliament.odda_examples;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.odda.Entity;
import com.bbn.parliament.odda.EntityFactory;
import com.bbn.parliament.sparql_query_builder.QueryBuilder;

public class ProfilesAndAssociations {
	private static final String NS = "http://bbn.com/tbox/ix/example_domain#";
	private static final Resource Association = ResourceFactory.createResource(
		NS + "Association");
	private static final Resource EntityOfInterest = ResourceFactory.createResource(
		NS + "EntityOfInterest");
	private static final Logger LOG = LoggerFactory.getLogger(ProfilesAndAssociations.class);

	public final Set<Entity> profiles;
	public final Set<Entity> associations;

	private ProfilesAndAssociations(Map<Resource, Entity> entMap) {
		profiles = Collections.unmodifiableSet(entMap.values().stream()
			.filter(ent -> ent.isOfType(EntityOfInterest))
			.collect(Collectors.toCollection(() -> new TreeSet<>(Entity.COMPARATOR))));
		associations = Collections.unmodifiableSet(entMap.values().stream()
			.filter(ent -> ent.isOfType(Association))
			.collect(Collectors.toCollection(() -> new TreeSet<>(Entity.COMPARATOR))));
	}

	public static ProfilesAndAssociations fetchProfileAndAssociatedProfilesByTypeAndIri(
		Resource iri, Resource typeIri, EntityFactory entFact) {

		Map<Resource, Entity> entMap = entFact.fetchEntities(QueryBuilder
			.fromRsrc("odda_examples/EntitiesRootedAtViaPAndA.sparql", entFact.prefixMapping())
			.setArg("_rootType", typeIri)
			.setArg("_root", iri)
			.asQuery());
		ProfilesAndAssociations panda = new ProfilesAndAssociations(entMap);

		LOG.error("Profile query retrieved {} profiles and {} associations",
			panda.profiles.size(), panda.associations.size());
		return panda;
	}
}
