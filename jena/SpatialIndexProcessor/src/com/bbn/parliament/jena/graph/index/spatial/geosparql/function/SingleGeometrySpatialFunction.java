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
public abstract class SingleGeometrySpatialFunction extends SpatialFunctionBase {

   /**
    * {@inheritDoc}
    */
   @Override
   protected NodeValue exec(Binding binding, List<NodeValue> evalArgs,
         String uri, FunctionEnv env) {
      NodeValue geom = evalArgs.get(0);
      checkGeometryLiteral(geom);

      Geometry g = (Geometry) geom.getNode().getLiteralValue();
      GeoSPARQLLiteral datatype = (GeoSPARQLLiteral)geom.getNode().getLiteralDatatype();
      return exec(g, datatype, binding, evalArgs, uri, env);
   }

   protected abstract NodeValue exec(Geometry g, GeoSPARQLLiteral datatype, Binding binding,
         List<NodeValue> evalArgs, String uri, FunctionEnv env);

   protected abstract String[] getRestOfArgumentTypes();

   /**
    * {@inheritDoc}
    */
   @Override
   protected String[] getArgumentTypes() {
      String[] rest = getRestOfArgumentTypes();
      String[] args = new String[rest.length + 1];
      args[0] = "ogc:GeomLiteral";
      int i = 1;
      for (String s : rest) {
         args[i++] = s;
      }
      return args;
   }

}
