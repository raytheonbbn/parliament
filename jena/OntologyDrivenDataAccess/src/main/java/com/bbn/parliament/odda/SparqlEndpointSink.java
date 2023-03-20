package com.bbn.parliament.odda;

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;

public interface SparqlEndpointSink {
	public int runSelectQuery(Consumer<QuerySolution> querySolutionConsumer, Query query);
	public Stream<QuerySolution> runSelectQuery(Query query);
}
