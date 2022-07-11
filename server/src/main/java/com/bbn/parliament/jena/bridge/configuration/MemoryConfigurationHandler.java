package com.bbn.parliament.jena.bridge.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.configuration.vocab.ConfigOnt;
import com.bbn.parliament.jena.bridge.ParliamentBridge;
import com.bbn.parliament.jena.bridge.tracker.Trackable;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.graph.ModelManager;
import com.hp.hpl.jena.rdf.model.Resource;

public class MemoryConfigurationHandler implements ConfigurationHandler {
	private static final MemoryPoolMXBean tenuredGenPool = findTenuredGenPool();
	private static Logger _log = LoggerFactory.getLogger(MemoryConfigurationHandler.class);

	public static void setPercentageUsageThreshold(double percentage) {
		if (percentage <= 0.0 || percentage > 1.0) {
			throw new IllegalArgumentException("Percentage not in range");
		}
		long maxMemory = tenuredGenPool.getUsage().getMax();
		long warningThreshold = (long) (maxMemory * percentage);
		tenuredGenPool.setUsageThreshold(warningThreshold);
	}

	/**
	 * Tenured Space Pool can be determined by it being of type
	 * HEAP and by it being possible to set the usage threshold.
	 */
	private static MemoryPoolMXBean findTenuredGenPool() {
		for (MemoryPoolMXBean pool :
			ManagementFactory.getMemoryPoolMXBeans()) {
			// I don't know whether this approach is better, or whether
			// we should rather check for the pool name "Tenured Gen"?
			if (pool.getType() == MemoryType.HEAP &&
				pool.isUsageThresholdSupported()) {
				return pool;
			}
		}
		throw new AssertionError("Could not find tenured space");
	}

	@Override
	public void initialize(Resource handle) throws ConfigurationException {
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter) mbean;

		if (handle.hasProperty(ConfigOnt.usageThreshold)) {
			double threshold = handle.getProperty(ConfigOnt.usageThreshold).getDouble();
			if (threshold < 0 || threshold > 1) {
				_log.error(String.format("Threshold must be between 0 and 1. %f is invalid.", threshold));
			}
			setPercentageUsageThreshold(threshold);
		} else {
			_log.error("No usage threshold specified");
			return;
		}
		emitter.addNotificationListener(new NotificationListener() {
			@Override
			public void handleNotification(Notification n, Object hb) {
				if (n.getType().equals(
					MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {

					List<Long> ids = Tracker.getInstance().getTrackableIDs();
					if (ids.size() > 0) {
						int index = 0;
						long id = ids.get(index);
						Trackable t = Tracker.getInstance().getTrackable(id);
						while (index < ids.size() && (null == t || !t.isCancellable())) {
							index++;
							id = ids.get(index);
							t = Tracker.getInstance().getTrackable(id);
						}
						if (t.isCancellable()) {
							try {
								t.cancel();
							}
							catch(TrackableException e) {
								_log.error("Error while cancelling " + t.getId(), e);
							}
						}
					}
				}
			}
		}, null, null);
	}

	@Override
	public void postModelInitialization(ParliamentBridge server,
		ModelManager manager) {
	}

	@Override
	public void preModelInitialization(ParliamentBridge server,
		ModelManager manager) {
	}
}
