package com.bbn.parliament.jena.graph.index.spatial.standard;

import java.util.ArrayList;
import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.standard.data.BufferedGeometry;
import com.bbn.parliament.jena.graph.index.spatial.standard.data.FloatingCircle;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class SpatialGeometryFactory {
   private static final PrecisionModel PRECISION_MODEL = new PrecisionModel(
      PrecisionModel.FLOATING);
   public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(
      PRECISION_MODEL, com.bbn.parliament.jena.graph.index.spatial.Constants.WGS84_SRID);

   public static Coordinate getCoordinateFromPos(String pos) {
      if (null == pos) {
         throw new RuntimeException("Null position for point");
      }
      String[] degrees = pos.split(" ");
      if (degrees.length != 2) {
         throw new RuntimeException("'" + pos + "' does not contain 2 values");
      }
      double lat = Double.parseDouble(makeDouble(degrees[0]));
      double lon = Double.parseDouble(makeDouble(degrees[1]));
      Coordinate c = new Coordinate(lon, lat);

      return c;
   }

   /**
    * Get coordinates from the position list.
    *
    * @param posList
    *           the position list
    * @return the coordinate representation of the string.
    */
   public static Coordinate[] getCoordinatesFromPosList(String posList) {
      String[] degrees = posList.split(" ");
      if (degrees.length % 2 != 0) {
         throw new RuntimeException("'" + posList
               + "' does not contain an even number of values");
      }

      List<Coordinate> coords = new ArrayList<>();
      for (int i = 0; i < degrees.length; i += 2) {
         double lat = Double.parseDouble(makeDouble(degrees[i]));
         double lon = Double.parseDouble(makeDouble(degrees[i + 1]));
         Coordinate c = new Coordinate(lon, lat);
         coords.add(c);
      }
      return coords.toArray(new Coordinate[] {});
   }

   private static String makeDouble(String number) {
      String n = number;
      if (!n.contains(".")) {
         n = n + ".0";
      }
      return n;
   }

   /**
    * Converts an array of coordinates into a ring by ensuring that the first
    * and last coordinate are the same.
    *
    * @param coords
    *           the coordinates
    * @return a ring of coordinates.
    */
   public static Coordinate[] makeRing(Coordinate[] coords) {
      Coordinate[] ret = coords;
      if (!(ret[0].equals(ret[ret.length - 1]))) {
         Coordinate[] tmp = new Coordinate[ret.length + 1];
         for (int i = 0; i < ret.length; i++) {
            tmp[i] = ret[i];
         }
         tmp[ret.length] = tmp[0];
         ret = tmp;
      }
      return ret;
   }

   public static Point createPoint(Node subject, Graph graph) {
      Node[] nodes = { Constants.GML_POS_NODE, Constants.GML_POS_NODE_H };
      Point geom = null;
      for (Node n : nodes) {
         ExtendedIterator<Triple> triples = graph.find(subject, n, Node.ANY);

         while (triples.hasNext()) {
            Triple t = triples.next();
            if (t.getObject().isLiteral()) {
               String pos = t.getObject().getLiteralValue().toString();
               Coordinate coordinate = SpatialGeometryFactory
                     .getCoordinateFromPos(pos);
               geom = GEOMETRY_FACTORY.createPoint(coordinate);
               break;
            }
         }
         triples.close();
         if (null != geom) {
            geom.setUserData("EPSG:4326");
            break;
         }
      }

      return geom;
   }

   public static LineString createLineString(Node subject, Graph graph) {
      String posList = null;
      Node[] nodes = { Constants.GML_POS_LIST_NODE,
            Constants.GML_POS_LIST_NODE_H };

      for (Node n : nodes) {
         ExtendedIterator<Triple> triples = graph.find(subject, n, Node.ANY);

         while (triples.hasNext()) {
            Triple triple = triples.next();
            if (triple.getObject().isLiteral()) {
               posList = triple.getObject().getLiteralValue().toString();
               break;
            }
         }
         triples.close();
      }
      if (posList == null) {
         return null;
      }

      Coordinate[] coordinates = getCoordinatesFromPosList(posList);
      LineString geom = GEOMETRY_FACTORY.createLineString(coordinates);
      geom.setUserData("EPSG:4326");
      return geom;
   }

   public static Polygon createPolygon(Node subject, Graph graph) {
      Node exterior = null;
      Node[] nodes = { Constants.GML_EXTERIOR_NODE,
            Constants.GML_EXTERIOR_NODE_H };
      for (Node n : nodes) {
         ExtendedIterator<Triple> triples = graph.find(subject, n, Node.ANY);
         while (triples.hasNext()) {
            Triple triple = triples.next();
            if (!triple.getObject().isLiteral()) {
               exterior = triple.getObject();
               break;
            }
         }
         triples.close();
      }

      if (exterior == null) {
         return null;
      }
      LineString line = SpatialGeometryFactory
            .createLineString(exterior, graph);
      if (line == null) {
         return null;
      }

      Coordinate[] coords = line.getCoordinates();
      coords = SpatialGeometryFactory.makeRing(coords);
      LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coords);

      Node[] intNodes = { Constants.GML_INTERIOR_NODE,
            Constants.GML_INTERIOR_NODE_H };
      List<LinearRing> holes = new ArrayList<>();
      for (Node n : intNodes) {
         ExtendedIterator<Triple> triples = graph.find(subject, n, Node.ANY);
         while (triples.hasNext()) {
            Triple triple = triples.next();
            Node object = triple.getObject();
            if (!object.isLiteral()) {
               LineString interior = SpatialGeometryFactory
                     .createLineString(object, graph);
               if (interior != null) {
                  Coordinate[] intCoords = SpatialGeometryFactory
                        .makeRing(interior.getCoordinates());
                  LinearRing intRing = GEOMETRY_FACTORY
                        .createLinearRing(intCoords);
                  holes.add(intRing);
               }
            }
         }
         triples.close();
      }
      Polygon geom = GEOMETRY_FACTORY.createPolygon(shell, holes
            .toArray(new LinearRing[] {}));
      geom.setUserData("EPSG:4326");
      return geom;
   }

   public static FloatingCircle createFloatingCircle(double radius) {
      FloatingCircle geom = new FloatingCircle(GEOMETRY_FACTORY, radius);
      geom.setUserData("EPSG:4326");
      return geom;
   }

   public static BufferedGeometry createBufferedGeometry(Geometry extent) {
      BufferedGeometry geom = new BufferedGeometry(GEOMETRY_FACTORY, extent);
      geom.setUserData(extent.getUserData());
      return geom;
   }

   public static BufferedGeometry createBufferedGeometry(Geometry extent,
         double distance) {
      BufferedGeometry geom = new BufferedGeometry(GEOMETRY_FACTORY, extent,
                                                   distance);
      geom.setUserData(extent.getUserData());
      return geom;
   }

   /**
    * Get the UTM Zone SRID for a given geometry
    *
    * @param geometry
    *           the geometry to lookup.
    * @return the geometry's UTM SRID
    */
   public static int UTMZoneSRID(Geometry geometry) {
      Point p = geometry.getCentroid();
      int srid = 0;
      double lat = p.getY();
      double lon = p.getX();

      if (lat > 0) {
         srid = 32600;
      } else {
         srid = 32700;
      }

      double zone = Math.floor(((lon + 186d)) / 6);

      // make sure longitude 180.00 is in zone 60
      if (((Double) lon).equals(180D)) {
         zone = 60;
      }

      // special zone for Norway
      if (lat >= 56D && lat < 64D && lon >= 3D && lon <= 12D) {
         zone = 32;
      }

      // special zones for Svalbard
      if (lat >= 72D && lat < 84D) {
         if (lon >= 0D && lon < 9D) {
            zone = 31;
         } else if (lon >= 9D && lon < 21D) {
            zone = 33;
         } else if (lon >= 21D && lon < 33D) {
            zone = 35;
         } else if (lon >= 33D && lon < 42D) {
            zone = 37;
         }
      }
      srid += zone;

      return srid;
   }
}
