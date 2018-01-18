package com.bbn.parliament.jena.joseki.bridge.tracker.management;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.sparql.ARQException;

public class TrackerManagement {


   private static class MBeanServerHelper {
      private MBeanServer _mbs;
      private boolean _hasJMX = true;
      public MBeanServerHelper() {
         try {
            _mbs = ManagementFactory.getPlatformMBeanServer();
         } catch (Throwable ex) {
            log.warn("Failed to initialize JMX", ex);
            _hasJMX = false;
            _mbs = null;
         }
      }

      public boolean isJMXAvailable() {
         return _hasJMX;
      }

      public MBeanServer getMBeanServer() {
         return _mbs;
      }
   }

   // In some environments, JMX does not exist.
   static private Logger log = LoggerFactory.getLogger(TrackerManagement.class);


   private static MBeanServerHelper _instance = null;
   private static MBeanServerHelper getMBeanServerHelper() {
     if (null == _instance) {
         synchronized(MBeanServerHelper.class) {
            if (null == _instance) {
               _instance = new MBeanServerHelper();
            }
         }
      }
     return _instance;
   }

   public static void unregister(String name) {
      if (!getMBeanServerHelper().isJMXAvailable()) {
         return;
      }

      ObjectName objName = null;
      try {
         objName = new ObjectName(name);
      }
      catch(MalformedObjectNameException ex) {
         throw new ARQException("Failed to create name '" + name + "': "
            + ex.getMessage(), ex);
      }
      MBeanServer mbs = getMBeanServerHelper().getMBeanServer();

      if (mbs.isRegistered(objName)) {
         try {
            mbs.unregisterMBean(objName);
         }
         catch(InstanceNotFoundException e) {

         }
         catch(MBeanRegistrationException e) {

         }
      }
   }

   public static void register(String name, Object bean) {
      if (!getMBeanServerHelper().isJMXAvailable()) {
         return;
      }
      MBeanServer mbs = getMBeanServerHelper().getMBeanServer();

      ObjectName objName = null;
      try {
         objName = new ObjectName(name);
      }
      catch(MalformedObjectNameException ex) {
         throw new ARQException("Failed to create name '" + name + "': "
            + ex.getMessage(), ex);
      }

      try {
         // Unregister to avoid classloader problems.
         // A previous load of this class will have registered something
         // with the object name. Remove it - copes with reloading.
         // (Does not cope with multiple loads running in parallel.)
         if(mbs.isRegistered(objName)) {
            try {
               mbs.unregisterMBean(objName);
            }
            catch(InstanceNotFoundException ex) {}
         }
         log.debug("Register MBean: " + objName);
         mbs.registerMBean(bean, objName);
//         // remember ...
//         mgtObjects.put(objName, bean);

      }
      catch(NotCompliantMBeanException ex) {
         log.warn("Failed to register '" + objName.getCanonicalName() + "': "
            + ex.getMessage());
         throw new ARQException("Failed to register '"
            + objName.getCanonicalName() + "': " + ex.getMessage(), ex);
      }
      catch(InstanceAlreadyExistsException ex) {
         log.warn("Failed to register '" + objName.getCanonicalName() + "': "
            + ex.getMessage());
         throw new ARQException("Failed to register '"
            + objName.getCanonicalName() + "': " + ex.getMessage(), ex);
      }
      catch(MBeanRegistrationException ex) {
         log.warn("Failed to register '" + objName.getCanonicalName() + "': "
            + ex.getMessage());
         throw new ARQException("Failed to register '"
            + objName.getCanonicalName() + "': " + ex.getMessage(), ex);
      }
   }
}
