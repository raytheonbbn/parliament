package com.bbn.parliament.jena.graph.index.spatial.geosparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bbn.parliament.jena.graph.index.spatial.AbstractSpatialTest;
import com.bbn.parliament.jena.graph.index.spatial.Constants;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;

public class GeoSPARQLGraphTest extends AbstractSpatialTest {
	@Override
	protected Properties getProperties() {
		Properties p = new Properties();
		p.setProperty(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_RTREE);
		p.setProperty(Constants.GEOSPARQL_ENABLED, Boolean.TRUE.toString());
		return p;
	}

	private static final String NAMED_GRAPH1_URI = "http://example.org/data1";
	private static final String NAMED_GRAPH2_URI = "http://example.org/data2";
	private static Model namedModel1;
	private static Model namedModel2;

	@BeforeClass
	public static void setupGraph() {
		namedModel1 = addNamedGraph(NAMED_GRAPH1_URI);
		namedModel2 = addNamedGraph(NAMED_GRAPH2_URI);
	}

	@SuppressWarnings("static-method")
	@Before
	public void load() {
		loadData("queries/geosparql/100points.nt", namedModel1);
		loadData("queries/geosparql/500points.nt", namedModel2);
	}

	//private ExecutionContext execCxt;
	//private KbOpExecutor opExecutor;

	@Before
	public void setUp() throws Exception {
		//Context params = ARQ.getContext();
		//execCxt = new ExecutionContext(params, gr, dataset, KbOpExecutor.KbOpExecutorFactory);
		//opExecutor = new KbOpExecutor(execCxt);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testThreads() {
		@SuppressWarnings("unused")
		Query query = QueryFactory.read("queries/geosparql/query-graphs.rq", Syntax.syntaxARQ);
		//runTest("queries/geosparql/query-graphs.rq", "queries/geosparql/result-1.ttl");
		//performQuery(query);
		//startThreadTest(query.toString(), 100, 10);
	}

	public void startThreadTest(String query, int amount, int poolSize) {
		LOG.debug("Starting test");
		LOG.debug("Index size {}", index.size());
		long maxTimeOut = 450000 * 1000;
		int corePoolSize = poolSize;
		int maximumSize = corePoolSize + 1;
		long keepAliveTime = 5000L;

		List<ThreadQuery> threads = new ArrayList<>(corePoolSize);

		ThreadPoolExecutor tpe = new ThreadPoolExecutor(
			corePoolSize + 1,
			maximumSize,
			keepAliveTime,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());

		Dataset ds = graphStore.toDataset();
		for (int i = 0; i < corePoolSize; ++i) {
			QueryExecution qexec = QueryExecutionFactory.create(query, Syntax.syntaxARQ, ds);
			ThreadQuery tq = new ThreadQuery(qexec);
			threads.add(tq);
			tpe.execute(tq);
		}

		final AtomicBoolean finished = new AtomicBoolean(false);

		long startTime = System.currentTimeMillis();
		while (!finished.get()) {
			if (System.currentTimeMillis() - startTime > maxTimeOut) {
				fail("Did not finish");
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			boolean isfinished = true;
			// LOG.info("Threads size: {}", threads.size());

			for (ThreadQuery tq : threads) {
				if (null == tq) {
					LOG.info("null..wtf");
					continue;
				}
				if (tq.isFinished()) {
					assertEquals("Thread #" + tq.id + " invalid count", amount,
						tq.getCount());
				}
				isfinished = (tq.isFinished()) && isfinished;

				if (!isfinished) {
					// LOG.info(tq.id + " not finished");
					continue;
				}
			}
			finished.set(isfinished);
		}
		tpe.shutdown();

		assertTrue(finished.get());
	}

	private static class ThreadQuery implements Runnable {
		private QueryExecution qexec;
		private long id;
		private boolean finished = false;
		private int count;
		private Object finishedLock = new Object();
		private Object countLock = new Object();

		public ThreadQuery(QueryExecution qexec) {
			this.qexec = qexec;
		}

		@Override
		public void run() {
			id = Thread.currentThread().getId();
			LOG.debug("{} Start", id);

			ResultSet rs = qexec.execSelect();
			synchronized (countLock) {
				count = 0;
			}
			try {
				assertTrue(rs.hasNext());
				synchronized (countLock) {
					while (rs.hasNext()) {
						//Thread.sleep(5000);
						QuerySolution qs = rs.next();
						LOG.debug("{} {}", id, qs);
						++count;
					}
				}
				synchronized (finishedLock) {
					finished = true;
				}
			} catch (Exception e) {
				LOG.error("wtf?", e);
				synchronized (finishedLock) {
					finished = true;
				}
			}
			synchronized (countLock) {
				LOG.debug("{} Read: {} results", id, count);
			}
		}

		public boolean isFinished() {
			synchronized (finishedLock) {
				return finished;
			}
		}

		public int getCount() {
			synchronized (countLock) {
				return count;
			}
		}
	}
}
