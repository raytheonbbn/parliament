package com.bbn.parliament.spring_boot.joseki_extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.bridge.tracker.TrackableUpdate;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.handler.Inserter;
import com.bbn.parliament.jena.handler.VerifyOption;

public class TrackerTestCase {
	private static final Logger LOG = LoggerFactory.getLogger(TrackerTestCase.class);

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
	@Disabled
	public void testTrackerQuery() {
		String query = "SELECT ?a WHERE { ?a ?b ?c }";

		TrackableQuery tq = Tracker.getInstance().createQuery(query, "TEST");
		try {
			tq.run();
		}
		catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		// should be 1 since the result set isn't processed yet
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());

		TrackableQuery tq1 = Tracker.getInstance().createQuery(query, "TEST");
		try {
			tq1.run();
		}
		catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
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

		query = "CONSTRUCT { ?a a ?c }  WHERE {?a a ?c }";
		tq = Tracker.getInstance().createQuery(query, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			tq.run();
		}
		catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}

//	@SuppressWarnings("static-method")
//	@Test
//	public void testCancel() {
//		PropertyFunctionRegistry.get().put("http://example.org/suspend", Suspend.class);
//		String query = "SELECT * WHERE { ?a <http://example.org/suspend> ?b . }";
//		final TrackableQuery tq = Tracker.getInstance().createQuery(query, "TEST");
//		Runnable r = new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					tq.run();
//					ResultSet rs = tq.getResultSet();
//					while (rs.hasNext()) {
//						rs.next();
//					}
//				} catch (Throwable e) {
//					e.printStackTrace();
//				}
//			}
//		};
//
//		Thread t = new Thread(r);
//		t.start();
//
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//
//		}
//		try {
//			System.out.println("cancel");
//			tq.cancel();
//		} catch (TrackableException e) {
//			e.printStackTrace();
//		}
//		assertEquals(Status.CANCELLED, tq.getStatus());
//		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
//	}

	@SuppressWarnings("static-method")
	@Test
	@Disabled
	public void testTrackerUpdate() {
		TrackableUpdate tu;

		String update = "INSERT DATA { <http://example.org/test> a <http://example.org/data> }";
		tu = Tracker.getInstance().createUpdate(update, "TEST");

		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			tu.run();
		}
		catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		String delete = "DELETE DATA { <http://example.org/test> a <http://example.org/data> }";
		tu = Tracker.getInstance().createUpdate(delete, "TEST");
		assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
		try {
			tu.run();
		}
		catch(TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			fail(ex.getMessage());
		}
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}

//	private static class TestInserter extends Inserter {
//		public TestInserter() {
//			super("", new InputStreamProvider() {
//				@Override
//				public InputStream getInputStream() throws IOException {
//					ClassLoader cl = Thread.currentThread().getContextClassLoader();
//					InputStream is = cl.getResourceAsStream("University15_20.owl.zip");
//					ZipInputStream zis = new ZipInputStream(is);
//					zis.getNextEntry();
//					return zis;
//				}
//			}, "RDF/XML", null, "yes", "no", "University15_20.owl");
//		}
//	}

	@SuppressWarnings("static-method")
	@Test
	public void testTrackerInsert() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream("University15_20.owl-mini.zip");
		try (ZipInputStream zin = new ZipInputStream(is)) {
			Inserter inserter = new Inserter(false, "",  "application/rdf+xml", "University15_20.owl", VerifyOption.VERIFY, null, Inserter.getZipStrmProvider(zin));
			TrackableInsert ti = Tracker.getInstance().createInsert(inserter, "TEST");
			assertEquals(1, Tracker.getInstance().getTrackableIDs().size());
			try {
				LOG.debug("ti.run()...");
				ti.run();
			} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
				fail(ex.getMessage());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
	}
}
