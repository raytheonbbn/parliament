package org.semwebcentral.parliament.odda_examples;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.semwebcentral.parliament.misc_needing_refactor.Dmn;
import org.semwebcentral.parliament.odda.Entity;
import org.semwebcentral.parliament.odda.EntityFactory;
import org.semwebcentral.parliament.sparql_query_builder.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfilesAndAssociations {
	private static final Logger LOG = LoggerFactory.getLogger(ProfilesAndAssociations.class);

	public final Set<Entity> profiles;
	public final Set<Entity> associations;

	private ProfilesAndAssociations(Map<Resource, Entity> entMap) {
		profiles = Collections.unmodifiableSet(entMap.values().stream()
			.filter(ent -> ent.isOfType(Dmn.EntityOfInterest))
			.collect(Collectors.toCollection(() -> new TreeSet<>(Entity.COMPARATOR))));
		associations = Collections.unmodifiableSet(entMap.values().stream()
			.filter(ent -> ent.isOfType(Dmn.Association))
			.collect(Collectors.toCollection(() -> new TreeSet<>(Entity.COMPARATOR))));
	}

	public static ProfilesAndAssociations fetchProfileAndAssociatedProfilesByTypeAndUri(
		Resource uri, Resource typeUri, EntityFactory entFact) {

		Map<Resource, Entity> entMap = entFact.fetchEntities(QueryBuilder
			.fromRsrc("odda_examples/EntitiesRootedAtViaPAndA.sparql", entFact.getOntTools().getPrefixMapping())
			.setArg("_rootType", typeUri)
			.setArg("_root", uri)
			.asQuery());
		ProfilesAndAssociations panda = new ProfilesAndAssociations(entMap);

		LOG.error("Profile query retrieved {} profiles and {} associations",
			panda.profiles.size(), panda.associations.size());
		return panda;
	}
}
