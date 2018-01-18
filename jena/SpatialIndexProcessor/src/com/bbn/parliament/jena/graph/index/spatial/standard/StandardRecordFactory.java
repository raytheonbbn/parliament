package com.bbn.parliament.jena.graph.index.spatial.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.spatial.GeometryRecord;
import com.bbn.parliament.jena.graph.index.spatial.GeometryRecordFactory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.vividsolutions.jts.geom.Geometry;

public class StandardRecordFactory implements GeometryRecordFactory {

   private static List<Triple> MATCHES;
   static {
      MATCHES = new ArrayList<>(Constants.VALID_TYPES.length);
      for (String type : Constants.VALID_TYPES) {
         MATCHES.add(Triple.create(Node.ANY, RDF.Nodes.type, Node.createURI(type)));
      }
   }

   private Graph graph;
   private Map<Node, String> nodesToProcess;
   private List<String> validTypes = Arrays.asList(Constants.VALID_TYPES);

   public StandardRecordFactory(Graph graph) {
      this.graph = graph;
      this.nodesToProcess = new HashMap<>();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((graph == null) ? 0 : graph.hashCode());
      result = prime * result
            + ((validTypes == null) ? 0 : validTypes.hashCode());
      return result;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof StandardRecordFactory)) {
         return false;
      }
      StandardRecordFactory other = (StandardRecordFactory) obj;
      if (graph == null) {
         if (other.graph != null) {
            return false;
         }
      } else if (!graph.equals(other.graph)) {
         return false;
      }
      if (validTypes == null) {
         if (other.validTypes != null) {
            return false;
         }
      } else if (!validTypes.equals(other.validTypes)) {
         return false;
      }
      return true;
   }

   @Override
   public GeometryRecord createRecord(Triple t) {
      Node subject = t.getSubject();
      Node predicate = t.getPredicate();
      Node object = t.getObject();
      Geometry geom = null;

      Node key = subject;

      if (RDF.type.asNode().equals(predicate) && object.isURI()) {
         String objUri = object.getURI();
         if (!validTypes.contains(objUri)) {
            return null;
         }
         // check graph to see if geometry is complete
         geom = createGeometry(subject, objUri);
         if (null == geom) {
            nodesToProcess.put(subject, objUri);
         } else {
            if (nodesToProcess.containsKey(subject)) {
               nodesToProcess.remove(subject);
            }
         }
      } else {
         if (nodesToProcess.containsKey(subject)) {
            geom = processNode(subject);
            if (null != geom) {
               nodesToProcess.remove(subject);
               key = subject;
            }
         }
         if (nodesToProcess.containsKey(object)) {
            geom = processNode(object);
            if (null != geom) {
               key = object;
               nodesToProcess.remove(object);
            }

         }
      }

      if (null == geom) {
         return null;
      }
      GeometryRecord r = GeometryRecord.create(key, geom);
      return r;
   }

   private Geometry processNode(Node node) {
      ExtendedIterator<Triple> triples = graph.find(node, RDF.type.asNode(),
                                                    Node.ANY);
      Geometry ret = null;
      while (triples.hasNext()) {
         Triple triple = triples.next();
         if (!triple.getObject().isURI()) {
            continue;
         }
         Geometry geom = createGeometry(node, triple.getObject().getURI());
         if (null != geom) {
            nodesToProcess.remove(node);
            ret = geom;
         }
      }
      triples.close();
      return ret;
   }

   /**
    * @param graph
    * @param subject
    * @param objUri
    */
   private Geometry createGeometry(Node subject, String objUri) {
      if (Constants.GML_POINT.equals(objUri)
            || Constants.GML_POINT_H.equals(objUri)) {
         return SpatialGeometryFactory.createPoint(subject, graph);
      } else if (Constants.GML_POLYGON.equals(objUri)
            || Constants.GML_POLYGON_H.equals(objUri)) {
         return SpatialGeometryFactory.createPolygon(subject, graph);
      } else if (Constants.GML_LINE_STRING.equals(objUri)
            || Constants.GML_LINE_STRING_H.equals(objUri)) {
         return SpatialGeometryFactory.createLineString(subject, graph);
      }
      return null;
   }

   @Override
   public List<Triple> getTripleMatchers() {
      return MATCHES;
   }
}
