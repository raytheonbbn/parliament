package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipInputStream;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
	@SuppressWarnings("static-method")
	@BeforeEach
	public void initialize() {
		ModelManager.inst().initialize();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void clearKB() {
		ModelManager.inst().clearKb();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTrackerQuery() {
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String query = "select * where { ?a ?b ?c }";
		TrackableQuery tq = Tracker.getInstance().createQuery(query, "TEST");
		try {
			tq.run();
		} catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		// should be 1 since the result set isn't processed yet
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());

		TrackableQuery tq1 = Tracker.getInstance().createQuery(query, "TEST");
		try {
			tq1.run();
		} catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}

		assertEquals(2, Tracker.getInstance().getTrackableIDs().size());

		ResultSet rs = tq.getResultSet();
		while (rs.hasNext()) {
			rs.next();
		}
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());

		rs = tq1.getResultSet();
		while (rs.hasNext()) {
			rs.next();
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		query = "construct where {?a a ?c }";
		tq = Tracker.getInstance().createQuery(query, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			tq.run();
		} catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
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
	public void testTrackerUpdate() {
		TrackableUpdate tu;

		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String update = "insert data { <http://example.org/test> a <http://example.org/data> }";
		tu = Tracker.getInstance().createUpdate(update, "TEST");

		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			tu.run();
		} catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String delete = "delete data { <http://example.org/test> a <http://example.org/data> }";
		tu = Tracker.getInstance().createUpdate(delete, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			tu.run();
		} catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTrackerInsert() {
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		Inserter inserter = Inserter.newGraphInserter(
			null, "RDF/XML", "University15_20.owl", VerifyOption.VERIFY, null,
			() -> {
				try {
					File dataDir = new File(System.getProperty("test.data.path"));
					File file = new File(dataDir, "University15_20.owl.zip");
					InputStream is = new FileInputStream(file);
					ZipInputStream zis = new ZipInputStream(is);
					zis.getNextEntry();
					return zis;
				} catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
		});

		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			ti.run();
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}

		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}
}