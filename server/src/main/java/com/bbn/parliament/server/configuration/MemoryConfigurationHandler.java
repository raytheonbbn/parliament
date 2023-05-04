package com.bbn.parliament.server.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.server.ParliamentBridge;
import com.bbn.parliament.server.configuration.vocab.ConfigOnt;
import com.bbn.parliament.server.exception.ConfigurationException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.graph.ModelManager;
import com.bbn.parliament.server.tracker.Trackable;
import com.bbn.parliament.server.tracker.Tracker;

public class MemoryConfigurationHandler implements ConfigurationHandler {
	private static final MemoryPoolMXBean TENURED_GEN_POOL = findTenuredGenPool();
	private static final Logger LOG = LoggerFactory.getLogger(MemoryConfigurationHandler.class);

	public static void setPercentageUsageThreshold(double percentage) {
		if (percentage <= 0.0 || percentage > 1.0) {
			throw new IllegalArgumentException("Percentage not in range");
		}
		long maxMemory = TENURED_GEN_POOL.getUsage().getMax();
		long warningThreshold = (long) (maxMemory * percentage);
		TENURED_GEN_POOL.setUsageThreshold(warningThreshold);
	}

	/**
	 * Tenured Space Pool can be determined by it being of type HEAP and by it being
	 * possible to set the usage threshold. Not sure whether this approach is
	 * better, or whether we should rather check for the pool name "Tenured Gen".
	 */
	private static MemoryPoolMXBean findTenuredGenPool() {
		return ManagementFactory.getMemoryPoolMXBeans().stream()
			.filter(pool -> pool.getType() == MemoryType.HEAP)
			.filter(MemoryPoolMXBean::isUsageThresholdSupported)
			.findFirst()
			.orElseThrow(() -> new AssertionError("Could not find tenured space"));
	}

	@Override
	public void initialize(Resource handle) throws ConfigurationException {
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter) mbean;

		if (handle.hasProperty(ConfigOnt.usageThreshold)) {
			double threshold = handle.getProperty(ConfigOnt.usageThreshold).getDouble();
			if (threshold < 0 || threshold > 1) {
				LOG.error("Threshold must be between 0 and 1. {} is invalid.", threshold);
			}
			setPercentageUsageThreshold(threshold);
		} else {
			LOG.error("No usage threshold specified");
			return;
		}
		emitter.addNotificationListener(new NotificationListener() {
			@Override
			public void handleNotification(Notification n, Object hb) {
				if (n.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
					List<Long> ids = Tracker.getInstance().getTrackableIDs();
					if (ids.size() > 0) {
						int index = 0;
						long id = ids.get(index);
						Trackable t = Tracker.getInstance().getTrackable(id);
						while (index < ids.size() && (null == t || !t.isCancellable())) {
							++index;
							id = ids.get(index);
							t = Tracker.getInstance().getTrackable(id);
						}
						if (t.isCancellable()) {
							try {
								t.cancel();
							} catch(TrackableException ex) {
								LOG.error("Error while cancelling " + t.getId(), ex);
							}
						}
					}
				}
			}
		}, null, null);
	}

	@Override
	public void postModelInitialization(ParliamentBridge server, ModelManager manager) {
	}

	@Override
	public void preModelInitialization(ParliamentBridge server, ModelManager manager) {
	}
}
