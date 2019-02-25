package com.bbn.parliament.jena.joseki.bridge.tracker;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.graph.ModelManager;
import com.bbn.parliament.jni.Config;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 * A trackable query. Currently the only cancellable Trackable object.
 * Cancelling aborts the query execution.
 *
 * @author rbattle
 */
public class TrackableQuery extends Trackable {
	private static Logger _log = LoggerFactory.getLogger(TrackableQuery.class);

	private static final Long TIMEOUT_DURATION;
	private static final TimeUnit TIMEOUT_UNIT;

	static {
		Config config = Config.readFromFile();
		TIMEOUT_DURATION = config.m_timeoutDuration;
		TIMEOUT_UNIT = config.m_timeoutUnit;
	}

	private final Query _query;
	// private final AtomicBoolean _cancelled;
	private QueryExecution _qExec;
	private Object _queryResult;

	@ConstructorProperties({ "id", "query", "creator" })
	TrackableQuery(long id, String query, String creator) {
		this(id, QueryFactory.create(query, Syntax.syntaxARQ), creator);
	}

	TrackableQuery(long id, Query query, String creator) {
		super(id, creator);
		_query = query;
		if (_log.isDebugEnabled()) {
			_log.debug("Create query: {}\n{}", _id, query.toString());
		}
		// _cancelled = new AtomicBoolean(false);
	}

	@Override
	public void release() {
		if (_log.isDebugEnabled()) {
			_log.debug("release query: {}", _id);
		}
		if (_qExec != null) {
			if (isRunning()) {
				try {
					cancel();
				} catch (TrackableException e) {
					_log.error("While releasing, error while cancelling query.", e);
				}
			}
			if (!isCancelled()) {
				_qExec.close();
			}
		}
	}

	private void createQueryExecution() {
		if (!_query.hasDatasetDescription()) {
			_qExec = QueryExecutionFactory.create(_query, ModelManager.inst()
				.getDataset());
		} else {
			_qExec = QueryExecutionFactory.create(_query);
		}
		_qExec.setTimeout(TIMEOUT_DURATION, TIMEOUT_UNIT);

		// add a cancel flag to the query execution context. The context is a
		// copy of the ARQ global context (The constructor for
		// QueryExecutionBase calls ARQ.getContext().copy()) so this should be
		// unique for each query execution
		// _qExec.getContext().set(Constants.CANCEL_QUERY_FLAG_SYMBOL,
		// _cancelled);

		if (_log.isDebugEnabled()) {
			_log.debug("Created query execution # {} of type {}", getId(), _qExec
				.getClass().getName());
		}
	}

	@Override
	protected void doCancel() {
		if (_qExec != null) { // && !_cancelled.get()) {
			_log.info("Cancel query: {}", _id);
			_qExec.abort();
		}
	}

	@Override
	protected void doRun() {
		createQueryExecution();

		if (_query.isAskType()) {
			_queryResult = _qExec.execAsk();
		} else if (_query.isConstructType()) {
			_queryResult = _qExec.execConstruct();
		} else if (_query.isSelectType()) {
			// set finished on run to false as the query returns when the first
			// match is found. This will cause the website to display a status of
			// finished even though the query could be iterating over a large
			// result set
			_setFinishedOnRun = false;

			_queryResult = new TrackableResultSet(_qExec.execSelect());
		} else if (_query.isDescribeType()) {
			_queryResult = _qExec.execDescribe();
		}

	}

	public Object getQueryResult() {
		return _queryResult;
	}

	public ResultSet getResultSet() {
		return ResultSet.class.cast(_queryResult);
	}

	public Model getModel() {
		return Model.class.cast(_queryResult);
	}

	public boolean getBoolean() {
		return Boolean.class.cast(_queryResult);
	}

	public Query getQuery() {
		return _query;
	}

	@Override
	public boolean isCancellable() {
		return true;
	}

	@Override
	public String getDisplay() {
		return _query.toString();
	}

	/**
	 * A result set that sets the status of the TrackableQuery to finished when
	 * there are no more results.
	 *
	 * @author rbattle
	 */
	private class TrackableResultSet implements ResultSet {
		private ResultSet _base;

		public TrackableResultSet(ResultSet rs) {
			_base = rs;
		}

		@Override
		public Model getResourceModel() {
			return _base.getResourceModel();
		}

		@Override
		public List<String> getResultVars() {
			return _base.getResultVars();
		}

		@Override
		public int getRowNumber() {
			return _base.getRowNumber();
		}

		@Override
		public boolean hasNext() {
			boolean ret = false;
			try {
				ret = _base.hasNext();
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
				next = _base.next();
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
				next = _base.nextBinding();
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
				next = _base.nextSolution();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}
			return next;
		}

		@Override
		public void remove() {
			try {
				_base.remove();
			} catch (RuntimeException e) {
				setError();
				throw e;
			}
		}
	}
}
