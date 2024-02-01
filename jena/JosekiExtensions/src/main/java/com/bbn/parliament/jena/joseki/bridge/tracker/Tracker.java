package com.bbn.parliament.jena.joseki.bridge.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.tracker.management.TrackableMXBean.Status;
import com.bbn.parliament.jena.joseki.bridge.tracker.management.TrackerManagement;
import com.bbn.parliament.jena.joseki.handler.Inserter;

/**
 * Keep track of {@link Trackable} items. The singleton instance provides
 * methods for creating trackable queries, inserts, and updates. The instance
 * also provides a method to cancel an individual tracked object.
 *
 * @author rbattle
 */
public class Tracker implements Observer {
	private static final long SHUTDOWN_TIMEOUT = 8 * 1000; // in milliseconds
	private static final Tracker INSTANCE = new Tracker();
	private static final AtomicLong LAST_TRACKER_ISSUED = new AtomicLong(0);
	private static final Logger LOG = LoggerFactory.getLogger(Tracker.class);

	/** Returns the singleton instance of the Tracker class. */
	public static Tracker getInstance() {
		return INSTANCE;
	}

	private Object _trackablesLock;
	private Map<Long, Trackable> _trackables;
	private boolean _shuttingDown;

	private Tracker() {
		_trackablesLock = new Object();
		_trackables = new HashMap<>();
		_shuttingDown = false;
	}

	public boolean isShuttingDown() {
		return _shuttingDown;
	}

	private static long getNextTrackerId() {
		return LAST_TRACKER_ISSUED.getAndIncrement();
	}

	/**
	 * Register the trackable object with this instance.
	 *
	 * @param t the trackable to register.
	 */
	private void registerTrackable(Trackable t) {
		synchronized (_trackablesLock) {
			_trackables.put(t.getId(), t);
			TrackerManagement.register(getName(t), t);
			t.addObserver(this);
		}
	}

	private static String getName(Trackable t) {
		return t.getClass().getPackage().getName() + ":type="
			+ t.getClass().getSimpleName() + ",id=" + t.getId();
	}

	/**
	 * Unregister the trackable object. This calls release on the trackable to
	 * release any held resources.
	 *
	 * @param t the trackable to unregister.
	 */
	private void unregisterTrackable(Trackable t) {
		synchronized (_trackablesLock) {
			Trackable other = _trackables.get(t.getId());
			if (null == other) {
				LOG.error("Could not find trackable with id {}", t.getId());
				return;
			}
			if (other != t) {
				LOG.error("Found Trackable in whose key is not its own id.");
			}
			_trackables.remove(t.getId());
			TrackerManagement.unregister(getName(t));
			try {
				t.release();
			} catch (Throwable e) {
				LOG.error("Error while releasing query # {}", t.getId(), e);
			}
			LOG.debug("Closed query execution # {}", t.getId());
		}
	}

	public List<Long> getTrackableIDs() {
		List<Long> ids;
		synchronized (_trackablesLock) {
			ids = new ArrayList<>(_trackables.keySet());
		}
		Collections.sort(ids);
		return ids;
	}

	/**
	 * Get a trackable given its ID. This can return null if the trackable has
	 * been unregistered in between calls to getTrackabledIDs and getTrackable.
	 *
	 * @param id the ID.
	 * @return a Trackable
	 */
	public Trackable getTrackable(long id) {
		synchronized (_trackablesLock) {
			return _trackables.get(id);
		}
	}

	public TrackableInsert createInsert(Inserter inserter, String creator) {
		TrackableInsert ti = new TrackableInsert(getNextTrackerId(), inserter, creator);
		registerTrackable(ti);
		return ti;
	}

	public TrackableUpdate createUpdate(String query, String creator) {
		TrackableUpdate tu = new TrackableUpdate(getNextTrackerId(), query, creator);
		registerTrackable(tu);
		return tu;
	}

	public TrackableQuery createQuery(String query, String creator) {
		TrackableQuery tq = new TrackableQuery(getNextTrackerId(), query, creator);
		registerTrackable(tq);
		return tq;
	}

	public void cancel(long id) throws TrackableException {
		synchronized (_trackables) {
			Trackable t = _trackables.get(id);
			if (null == t) {
				LOG.error("Could not find trackable with id # {}", id);
				return;
			}
			if (!t.isCancellable()) {
				LOG.error("Trackable " + id + " is not cancellable");
				return;
			}
			t.cancel();
		}
	}

	public void shutdown() {
		synchronized (_trackablesLock) {
			LOG.info("Aborting {} in-flight executions", _trackables.size());

			_shuttingDown = true;

			List<TrackableException> exceptions = new ArrayList<>();
			List<Trackable> toCancel = new ArrayList<>(_trackables.values());
			Collections.sort(toCancel);
			for (Trackable t : toCancel) {
				try {
					if (t.isCancellable()) {
						t.cancel();
					}
				} catch (TrackableException e) {
					exceptions.add(e);
				} finally {
					unregisterTrackable(t);
				}
			}

			LOG.info("Executions aborted, waiting for execution threads to exit");
			try {
				long now = System.currentTimeMillis();
				long waitEnd = now + SHUTDOWN_TIMEOUT;
				for (; now <= waitEnd && !_trackables.isEmpty(); now = System
					.currentTimeMillis()) {
					_trackables.wait(waitEnd - now);
				}
			} catch (InterruptedException ex) {
				// Do nothing, i.e. act as if the wait timed out
			}

			if (exceptions.size() > 0) {
				LOG.warn("Enountered the following exceptions while cancelling: ");
				for (TrackableException te : exceptions) {
					LOG.error("Exception", te);
				}
			}

			if (LOG.isDebugEnabled()) {
				int size = _trackables.size();
				if (size > 0) {
					LOG.debug("Timeout while aborting query executions, {} remain",
						size);
				} else {
					LOG.debug("Query executions aborted, execution threads finished");
				}
			}
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof Trackable t && arg instanceof Status s) {
			if (s == Status.FINISHED || s == Status.CANCELLED || s == Status.ERROR) {
				unregisterTrackable(t);
			}
		}
	}
}
