// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/** @author rbattle */
public abstract class AbstractThreadTest extends AbstractSpatialTest {
	@SuppressWarnings("static-method")
	@Before
	public void addData() {
		loadData("example/BuildingExample1.ttl");
		loadData("example/BuildingExample2.ttl");
		loadData("example/BuildingExample3.ttl");
	}

	@Test
	public void testSimpleQuery() {
		String query = PREFIXES
			+ "SELECT ?a "
			+ "WHERE { "
			+ "?polygon a gml:Polygon . "
			+ "?polygon gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "?ext gml:posList \"34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33\" . "
			+ "?a rcc:nonTangentialProperPart ?polygon . "
			+ "}";
		startThreadTest(query, 4, 10);
	}

	@Test
	public void testCircle3Extents() {
		String query = ""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 ?building3 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"0.1\"^^xsd:double ."
			+ "  (?sreg1 ?sreg2 ?sreg3) rcc:part ?circle .   "
			+ "  ?building1 a example:SpatialThing ;"
			+ "     georss:where ?sreg1 ."
			+ "  ?building2 a example:SpatialThing ;"
			+ "     georss:where ?sreg2 ."
			+ "  ?building3 a example:SpatialThing ;"
			+ "     georss:where ?sreg3 ."
			+ "  FILTER (?sreg1 != ?sreg2 &&"
			+ "     ?sreg1 != ?sreg3 &&"
			+ "     ?sreg2 != ?sreg3"
			+ "  )"
			+ "}";
		startThreadTest(query, 6, 20);
	}

	@Test
	public void testOptionalPart() {
		String query = ""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?x ?y "
			+ "WHERE { "
			+ "  ?poly1 a gml:Polygon ;"
			+ "    gml:exterior ["
			+ "      a gml:LinearRing ;"
			+ "      gml:posList \"34.90 36.0 34.845 36.0 34.845 35.8 34.9 35.8 34.9 36.0\""
			+ "    ] ."
			+ "  ?x rcc:part ?poly1 . "
			+ ""
			+ "  OPTIONAL {"
			+ "    ?poly2 a gml:Polygon ;"
			+ "       gml:exterior ?poly2ext . "
			+ "    ?poly2ext a gml:LinearRing ; "
			+ "       gml:posList \"34.90 36.0 34.845 36.0 34.845 35.8 34.9 35.8 34.9 36.0\" . "
			+ "    ?y rcc:part ?poly2 ."
			+ "  }"
			+ "}";
		startThreadTest(query, 9, 50);
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
		for (int i = 0; i < corePoolSize; i++) {
			QueryExecution qexec = QueryExecutionFactory.create(query, ds);
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

		/** {@inheritDoc} */
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
						QuerySolution qs = rs.next();
						LOG.debug("{} {}", id, qs);
						count++;
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
