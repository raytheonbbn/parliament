package com.bbn.parliament.jena.graph.index;

import java.util.Collection;

/**
 * An <code>Index</code> that is composed of multiple sub-<code>Index</code>es.
 *
 * @author rbattle
 *
 * @param <T>
 *           the type of object to index
 */
public interface CompositeIndex<T> extends Index<T> {

   /**
    * Get the sub indexes.
    *
    * @return the sub indexes.
    */
   public Collection<Index<T>> getSubIndexes();

}
