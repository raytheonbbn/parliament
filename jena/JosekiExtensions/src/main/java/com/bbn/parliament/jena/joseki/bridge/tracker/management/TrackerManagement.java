package com.bbn.parliament.jena.joseki.bridge.tracker.management;

import java.lang.management.ManagementFactory;
import java.util.Optional;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.jena.sparql.ARQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrackerManagement {
	static private Logger LOG = LoggerFactory.getLogger(TrackerManagement.class);

	public static void unregister(String name) {
		Optional<MBeanServer> mbs = getMBeanServer();
		if (!mbs.isPresent()) {
			return;
		}

		ObjectName objName = createObjectName(name);
		if (mbs.get().isRegistered(objName)) {
			try {
				mbs.get().unregisterMBean(objName);
			} catch(InstanceNotFoundException | MBeanRegistrationException ex) {
				// Do nothing
			}
		}
	}

	public static void register(String name, Object bean) {
		Optional<MBeanServer> mbs = getMBeanServer();
		if (!mbs.isPresent()) {
			return;
		}

		// Unregister to avoid classloader problems.
		// A previous load of this class will have registered something
		// with the object name. Remove it - copes with reloading.
		// (Does not cope with multiple loads running in parallel.)
		unregister(name);

		ObjectName objName = createObjectName(name);
		try {
			LOG.debug("Registering MBean: " + objName);
			mbs.get().registerMBean(bean, objName);
		} catch (NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException ex) {
			String msg = "Failed to register '%1$s': %2$s"
				.formatted(objName.getCanonicalName(), ex.getMessage());
			LOG.warn(msg);
			throw new ARQException(msg, ex);
		}
	}

	// In some environments, JMX does not exist.
	private static Optional<MBeanServer> getMBeanServer() {
		try {
			return Optional.of(ManagementFactory.getPlatformMBeanServer());
		} catch (Throwable ex) {
			LOG.warn("Failed to initialize JMX", ex);
			return Optional.empty();
		}
	}

	private static ObjectName createObjectName(String name) {
		try {
			return new ObjectName(name);
		} catch (MalformedObjectNameException ex) {
			throw new ARQException("Failed to create name '%1$s': %2$s"
				.formatted(name, ex.getMessage()), ex);
		}
	}
}
