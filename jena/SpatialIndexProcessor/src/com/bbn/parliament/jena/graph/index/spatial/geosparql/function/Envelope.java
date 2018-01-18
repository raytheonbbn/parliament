/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author rbattle
 *
 */
public class Envelope extends SingleGeometrySpatialFunction {

   /**
    * {@inheritDoc}
    */
   @Override
   protected NodeValue exec(Geometry g, GeoSPARQLLiteral datatype, Binding binding,
         List<NodeValue> evalArgs, String uri, FunctionEnv env) {
      Geometry toReturn = g.getEnvelope();
      toReturn.setUserData(g.getUserData());
      return makeNodeValue(toReturn, datatype);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String[] getRestOfArgumentTypes() {
      return new String[] {};
   }

}
