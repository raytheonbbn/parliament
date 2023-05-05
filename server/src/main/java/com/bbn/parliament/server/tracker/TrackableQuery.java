package com.bbn.parliament.server.tracker;

import java.beans.ConstructorProperties;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.core.jni.KbConfig;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.graph.ModelManager;

/**
 * A trackable query. Currently the only Trackable object that can be canceled.
 * Canceling aborts the query execution.
 *
 * @author rbattle
 */
public class TrackableQuery extends Trackable {
	private static final Logger LOG = LoggerFactory.getLogger(TrackableQuery.class);

	private final Query query;
	private QueryExecution qExec;
	private Object queryResult;

	@ConstructorProperties({ "id", "query", "creator" })
	TrackableQuery(long id, String query, String creator) {
		this(id, QueryFactory.create(query, Syntax.syntaxARQ), creator);
	}

	TrackableQuery(long id, Query query, String creator) {
		super(id, creator);
		this.query = query;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Create query: {}\n{}", _id, query.toString());
		}
	}

	@Override
	public void release() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("release query: {}", _id);
		}
		if (qExec != null) {
			if (isRunning()) {
				try {
					cancel();
				} catch (TrackableException e) {
					LOG.error("While releasing, error while canceling query.", e);
				}
			}
			if (!isCancelled()) {
				qExec.close();
			}
		}
	}

	private void createQueryExecution() {
		if (query.hasDatasetDescription()) {
			qExec = QueryExecutionFactory.create(query);
		} else {
			qExec = QueryExecutionFactory.create(query, ModelManager.inst().getDataset());
		}
		KbConfig cfg = ModelManager.inst().getDefaultGraphConfig();
		qExec.setTimeout(cfg.m_timeoutDuration, cfg.m_timeoutUnit);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Created query execution # {} of type {}", getId(), qExec
				.getClass().getName());
		}
	}

	@Override
	protected void doCancel() {
		if (qExec != null) {
			LOG.info("Cancel query: {}", _id);
			qExec.abort();
		}
	}

	@Override
	protected void doRun() {
		createQueryExecution();

		if (query.isAskType()) {
			queryResult = qExec.execAsk();
		} else if (query.isConstructType()) {
			queryResult = qExec.execConstruct();
		} else if (query.isSelectType()) {
			// set finished on run to false as the query returns when the first
			// match is found. This will cause the website to display a status of
			// finished even though the query could be iterating over a large
			// result set
			_setFinishedOnRun = false;

			queryResult = new TrackableResultSet(qExec.execSelect());

		} else if (query.isDescribeType()) {
			queryResult = qExec.execDescribe();
		}
	}

	public Object getQueryResult() {
		return queryResult;
	}

	public ResultSet getResultSet() {
		return ResultSet.class.cast(queryResult);
	}

	public Model getModel() {
		return Model.class.cast(queryResult);
	}

	public boolean getBoolean() {
		return Boolean.class.cast(queryResult);
	}

	public Query getQuery() {
		return query;
	}

	@Override
	public boolean isCancellable() {
		return true;
	}

	@Override
	public String getDisplay() {
		return query.toString();
	}

	/**
	 * A result set that sets the status of the TrackableQuery to finished when
	 * there are no more results.
	 *
	 * @author rbattle
	 */
	private class TrackableResultSet implements ResultSet {
		private ResultSet base;

		public TrackableResultSet(ResultSet rs) {
			base = rs;
		}

		@Override
		public Model getResourceModel() {
			return base.getResourceModel();
		}

		@Override
		public List<String> getResultVars() {
			return base.getResultVars();
		}

		@Override
		public int getRowNumber() {
			return base.getRowNumber();
		}

		@Override
		public boolean hasNext() {
			boolean ret = false;
			try {
				ret = base.hasNext();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}
			// if no results, update the status for the website
			if (!ret) {
				setFinished();
			}
			return ret;
		}

		@Override
		public QuerySolution next() {
			QuerySolution next = null;
			try {
				next = base.next();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}

			return next;
		}

		@Override
		public Binding nextBinding() {
			Binding next = null;
			try {
				next = base.nextBinding();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}
			return next;
		}

		@Override
		public QuerySolution nextSolution() {
			QuerySolution next = null;
			try {
				next = base.nextSolution();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}
			return next;
		}

		@Override
		public void remove() {
			try {
				base.remove();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}
		}
	}
}
