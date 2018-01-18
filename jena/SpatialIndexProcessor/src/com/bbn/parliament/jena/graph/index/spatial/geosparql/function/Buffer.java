package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Units;
import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import com.hp.hpl.jena.vocabulary.XSD;
import com.vividsolutions.jts.geom.Geometry;

public class Buffer extends SingleGeometrySpatialFunction {

   /**
    * {@inheritDoc}
    */
   @Override
   protected NodeValue exec(Geometry g, GeoSPARQLLiteral datatype,
         Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {
      NodeValue radius = evalArgs.get(1);
      NodeValue units = evalArgs.get(2);

      checkUnits(units);
      Node rNode = radius.getNode();
      if (!rNode.isLiteral() || !(rNode.getLiteralValue() instanceof Number)) {
         throw new QueryExecException(String.format("%s is not a number",
                                                    rNode));
      }
      double distance = ((Number) rNode.getLiteralValue()).doubleValue();
      Node unitsNode = units.getNode();

      Geometry buffered = g;
      if (Units.Nodes.degree.equals(unitsNode)) {
         // UoM for WGS84 is degree
         buffered = g.buffer(distance);
      } else if (Units.Nodes.radian.equals(unitsNode)) {
         buffered = g.buffer(Math.toDegrees(distance));
      } else if (Units.Nodes.metre.equals(unitsNode)) {
         CoordinateReferenceSystem destination;
         MathTransform transform;
         MathTransform inverse;

         try {

            int srid = SpatialGeometryFactory.UTMZoneSRID(buffered
                  .getEnvelope());
            destination = CRS.decode("EPSG:" + srid);
            boolean valid = false;
            if (null != destination) {
               transform = CACHE.getTransform(DefaultGeographicCRS.WGS84,
                                              destination);
               inverse = transform.inverse();

               buffered = JTS.transform(buffered, transform);
               buffered = buffered.buffer(distance);

               buffered = JTS.transform(buffered, inverse);
               valid = buffered.isValid();
            }
            if (!valid) {
               throw new GeoSPARQLFunctionException(
                                                    String.format("Invalid geometry when buffering: %s by %f",
                                                                  g.toText(),
                                                                  distance));
            }
         } catch (MismatchedDimensionException e) {
            throw new GeoSPARQLFunctionException(
                                                 String.format("Error while buffering: %s by %f",
                                                               g.toText(),
                                                               distance), e);
         } catch (NoSuchAuthorityCodeException e) {
            throw new GeoSPARQLFunctionException(
                                                 String.format("Error while buffering: %s by %f",
                                                               g.toText(),
                                                               distance), e);
         } catch (NoninvertibleTransformException e) {
            throw new GeoSPARQLFunctionException(
                                                 String.format("Error while buffering: %s by %f",
                                                               g.toText(),
                                                               distance), e);
         } catch (FactoryException e) {
            throw new GeoSPARQLFunctionException(
                                                 String.format("Error while buffering: %s by %f",
                                                               g.toText(),
                                                               distance), e);
         } catch (TransformException e) {
            throw new GeoSPARQLFunctionException(
                                                 String.format("Error while buffering: %s by %f",
                                                               g.toText(),
                                                               distance), e);
         }

      } else {
         throw new UnsupportedUnitsException(unitsNode);
      }

      buffered.setUserData(g.getUserData());
      return makeNodeValue(buffered, datatype);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String[] getRestOfArgumentTypes() {
      return new String[] { XSD.xdouble.getURI(), XSD.anyURI.getURI() };
   }

}
