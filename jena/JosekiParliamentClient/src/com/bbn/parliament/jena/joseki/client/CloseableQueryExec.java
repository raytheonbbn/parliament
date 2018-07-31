package com.bbn.parliament.jena.joseki.client;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class CloseableQueryExec implements AutoCloseable {
	private QueryExecution qe;

	public CloseableQueryExec(String sparqlService, String query) {
		qe = QueryExecutionFactory.sparqlService(sparqlService, query);
	}

	public CloseableQueryExec(String sparqlService, Query query) {
		qe = QueryExecutionFactory.sparqlService(sparqlService, query);
	}

	public CloseableQueryExec(Dataset dataset, String query) {
		qe = QueryExecutionFactory.create(query, dataset);
	}

	public CloseableQueryExec(Dataset dataset, Query query) {
		qe = QueryExecutionFactory.create(query, dataset);
	}

	public CloseableQueryExec(Model model, String query) {
		qe = QueryExecutionFactory.create(query, model);
	}

	public CloseableQueryExec(Model model, Query query) {
		qe = QueryExecutionFactory.create(query, model);
	}

	public CloseableQueryExec(QueryExecution qExec) {
		qe = qExec;
	}

	public ResultSet execSelect() {
		return qe.execSelect();
	}

	@Override
	public void close() {
		if (qe != null) {
			qe.close();
			qe = null;
		}
	}
}
