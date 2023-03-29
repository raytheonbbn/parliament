package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.jena.graph.Node;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropertyFunctionBase;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.sparql.util.IterLib;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.bridge.tracker.TrackableUpdate;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.bridge.tracker.management.TrackableMXBean.Status;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.handler.Inserter;
import com.bbn.parliament.jena.handler.VerifyOption;

public class TrackerTestCase {
	private static class Suspend extends PropertyFunctionBase {
		@Override
		public QueryIterator exec(Binding binding, PropFuncArg argSubject,
			Node predicate, PropFuncArg argObject, ExecutionContext execCxt) {
			System.out.println("sleeping");
			try {
				Thread.sleep(100000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			System.out.println("awake");
			return IterLib.noResults(execCxt);
		}
	}

	private static final String TEST_RDF_FILE = "University15_20.owl";

	@BeforeAll
	public static void initialize() {
		ModelManager.inst().initialize();
	}

	@AfterAll
	public static void cleanUp() {
		ModelManager.inst().clearKb();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTrackerQuery() throws TrackableException, DataFormatException,
			MissingGraphException, IOException {
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String query = "select * where { ?a ?b ?c }";
		TrackableQuery tq1 = Tracker.getInstance().createQuery(query, "TEST");
		tq1.run();

		// should be 1 since the result set isn't processed yet
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());

		TrackableQuery tq2 = Tracker.getInstance().createQuery(query, "TEST");
		tq2.run();

		assertEquals(2, Tracker.getInstance().getTrackableIDs().size());

		ResultSet rs = tq1.getResultSet();
		while (rs.hasNext()) {
			rs.next();
		}
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());

		rs = tq2.getResultSet();
		while (rs.hasNext()) {
			rs.next();
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		query = "construct where {?a a ?c }";
		TrackableQuery tq3 = Tracker.getInstance().createQuery(query, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		tq3.run();
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCancel() {
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		PropertyFunctionRegistry.get().put("http://example.org/suspend", Suspend.class);
		String query = "select * where { ?a <http://example.org/suspend> ?b . }";
		final TrackableQuery tq = Tracker.getInstance().createQuery(query, "TEST");
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					tq.run();
					ResultSet rs = tq.getResultSet();
					while (rs.hasNext()) {
						rs.next();
					}
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
		};

		Thread t = new Thread(r);
		t.start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			fail(ex.getMessage());
		}

		try {
			System.out.println("cancel");
			tq.cancel();
		} catch (TrackableException ex) {
			ex.printStackTrace();
		}

		assertEquals(Status.CANCELLED, tq.getStatus());
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTrackerUpdate() throws TrackableException, DataFormatException,
			MissingGraphException, IOException {
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String update = "insert data { <http://example.org/test> a <http://example.org/data> }";
		TrackableUpdate tu = Tracker.getInstance().createUpdate(update, "TEST");

		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		tu.run();
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String delete = "delete data { <http://example.org/test> a <http://example.org/data> }";
		tu = Tracker.getInstance().createUpdate(delete, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		tu.run();
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTrackerInsert() throws TrackableException, DataFormatException,
			MissingGraphException, IOException {
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		Inserter inserter = Inserter.newGraphInserter(
			null, "RDF/XML", TEST_RDF_FILE, VerifyOption.VERIFY, null,
			() -> {
				try {
					File dataDir = new File(System.getProperty("test.data.path"));
					File file = new File(dataDir, TEST_RDF_FILE);
					return new FileInputStream(file);
				} catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			});

		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		ti.run();
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}
}
