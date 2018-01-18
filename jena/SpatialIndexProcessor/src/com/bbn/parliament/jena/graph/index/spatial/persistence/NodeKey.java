package com.bbn.parliament.jena.graph.index.spatial.persistence;

import java.io.Serializable;

/**
 * Representation of the key for stored map.
 *
 * @author rbattle
 *
 */
public class NodeKey implements Serializable {

   private static final long serialVersionUID = -8015836109240995962L;

   private String node;

   /**
    * Create a new instance.
    *
    * @param node
    *           the node.
    */
   public NodeKey(String node) {
      this.node = node;
   }

   /**
    * Get the name.
    *
    * @return the node.
    */
   public String getNode() {
      return node;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return "NodeKey [node=" + node + "]";
   }
}
