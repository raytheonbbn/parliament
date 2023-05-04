package com.bbn.parliament.client;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

/**
 * Prior to Jena 3.0.0, the QueryExecution class was not AutoCloseable, and so
 * this wrapper class made QueryExecution compatible with Java's try-finally
 * statement.
 *
 * @deprecated Starting with Jena 3.0.0, simply use QueryExecution instead,
 *             obtaining it directly from QueryExecutionFactory.
 */
@Deprecated(forRemoval = true, since = "2.8.2")
public class CloseableQueryExec implements QueryExecution {
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

	@Override
	public void close() {
		if (qe != null) {
			qe.close();
			qe = null;
		}
	}

//	@Override
//	public void setFileManager(FileManager fm) {
//		qe.setFileManager(fm);
//	}

	@Override
	public void setInitialBinding(QuerySolution binding) {
		qe.setInitialBinding(binding);
	}

	@Override
	public Dataset getDataset() {
		return qe.getDataset();
	}

	@Override
	public Context getContext() {
		return qe.getContext();
	}

	@Override
	public Query getQuery() {
		return qe.getQuery();
	}

	@Override
	public ResultSet execSelect() {
		return qe.execSelect();
	}

	@Override
	public Model execConstruct() {
		return qe.execConstruct();
	}

	@Override
	public Model execConstruct(Model model) {
		return qe.execConstruct(model);
	}

	@Override
	public Iterator<Triple> execConstructTriples() {
		return qe.execConstructTriples();
	}

	@Override
	public Model execDescribe() {
		return qe.execDescribe();
	}

	@Override
	public Model execDescribe(Model model) {
		return qe.execDescribe(model);
	}

	@Override
	public Iterator<Triple> execDescribeTriples() {
		return qe.execDescribeTriples();
	}

	@Override
	public boolean execAsk() {
		return qe.execAsk();
	}

	@Override
	public void abort() {
		qe.abort();
	}

	@Override
	public void setTimeout(long timeout, TimeUnit timeoutUnits) {
		qe.setTimeout(timeout, timeoutUnits);
	}

	@Override
	public void setTimeout(long timeout) {
		qe.setTimeout(timeout);
	}

	@Override
	public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
		qe.setTimeout(timeout1, timeUnit1, timeout2, timeUnit2);
	}

	@Override
	public void setTimeout(long timeout1, long timeout2) {
		qe.setTimeout(timeout1, timeout2);
	}

	@Override
	public boolean isClosed() {
		return (qe == null)
			? true
			: qe.isClosed();
	}

	@Override
	public long getTimeout1() {
		return qe.getTimeout1();
	}

	@Override
	public long getTimeout2() {
		return qe.getTimeout2();
	}

	@Override
	public Iterator<Quad> execConstructQuads() {
		return qe.execConstructQuads();
	}

	@Override
	public Dataset execConstructDataset() {
		return qe.execConstructDataset();
	}

	@Override
	public Dataset execConstructDataset(Dataset dataset) {
		return qe.execConstructDataset(dataset);
	}
}
