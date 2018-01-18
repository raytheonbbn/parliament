package com.bbn.parliament.jena.graph.index.spatial.persistence;

import java.io.Serializable;

/**
 * Representation of the geometry data to store.
 *
 * @author rbattle
 *
 */
public class NodeData implements Serializable {

   private static final long serialVersionUID = -9036265713376562086L;
   private String node;
   private byte[] extent;
   private int id;
   private String crsCode;

   /**
    * Create a new instance.
    *
    * @param id
    *           the id.
    * @param node
    *           the node.
    * @param extent
    *           the extent.
    * @param crsCode
    *           the coordinate reference system code.
    */
   public NodeData(int id, String node, byte[] extent, String crsCode) {
      this.id = id;
      this.node = node;
      this.extent = extent;
      this.crsCode = crsCode;
   }

   /**
    * Get the coordinate reference system code.
    *
    * @return the code.
    */
   public String getCRSCode() {
      return crsCode;
   }

   /**
    * Get the id.
    *
    * @return the id.
    */
   public int getId() {
      return id;
   }

   /**
    * Get the node.
    *
    * @return the node.
    */
   public String getNode() {
      return node;
   }

   /**
    * Get the extent.
    *
    * @return the extent.
    */
   public byte[] getExtent() {
      return extent;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return "NodeData [node=" + node + ", id=" + id + ", crsCode=" + crsCode + "]";
   }
}