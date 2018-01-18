package com.bbn.parliament.jena.graph.index;

import java.util.ArrayList;
import java.util.List;

/**
 * A registry for {@link IndexFactory}s. This registry is used by the
 * {@link IndexManager} when creating indexes.
 *
 * @author rbattle
 *
 * @see IndexManager
 */
public class IndexFactoryRegistry {

   private static class IndexFactoryRegistryHolder {
      private static final IndexFactoryRegistry INSTANCE = new IndexFactoryRegistry();
   }

   /**
    * Get the instance of the registry.
    *
    * @return the instance.
    */
   public static IndexFactoryRegistry getInstance() {
      return IndexFactoryRegistryHolder.INSTANCE;
   }

   private List<IndexFactory<?, ?>> factories;
   private boolean indexingEnabledByDefault;

   private Object lock = new Object();

   private IndexFactoryRegistry() {
      this.factories = new ArrayList<>();
   }

   /**
    * Register a new factory.
    *
    * @param factory
    *           a factory.
    */
   public void register(IndexFactory<?, ?> factory) {
      synchronized (lock) {
         factories.add(factory);
      }

   }

   /**
    * Unregister a factory.
    *
    * @param factory
    *           a factory.
    */
   public void unregister(IndexFactory<?, ?> factory) {
      synchronized (lock) {
         factories.remove(factory);
      }
   }

   /**
    * Get the registered factories.
    *
    * @return the factories.
    */
   public List<IndexFactory<?, ?>> getFactories() {
      synchronized (lock) {
         return new ArrayList<>(factories);
      }
   }

   /**
    * Answer if indexing is enabled by default.
    * @return <code>true</code> if indexing is enabled by default; otherwise <code>false</code>.
    */
   public boolean isIndexingEnabledByDefault() {
      return indexingEnabledByDefault;
   }


   /**
    * Set whether indexing is enabled by default.
    * @param value the default status of indexes.
    */
   public void setIndexingEnabledByDefault(boolean value) {
      indexingEnabledByDefault = value;
   }
}
