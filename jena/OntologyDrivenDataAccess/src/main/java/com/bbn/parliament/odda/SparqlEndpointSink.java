package com.bbn.parliament.odda;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;

public interface SparqlEndpointSink {
	public int runSelectQuery(Consumer<QuerySolution> querySolutionConsumer, Query query);
	public Stream<QuerySolution> runSelectQuery(Query query);
}
