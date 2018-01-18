// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import org.junit.Ignore;

/**
 * @author Robert Battle
 */
public class EstimateSelectivityTest extends AbstractPostgresTest {

   //
   // private static Node[] getVariables(List<Triple> triples) {
   // Set<Node> vars = new HashSet<Node>();
   //
   // for (Triple t : triples) {
   // if (t.getSubject().isVariable()) {
   // vars.add(t.getSubject());
   // }
   // if (t.getObject().isVariable()) {
   // vars.add(t.getObject());
   // }
   // }
   //
   // return vars.toArray(new Node[] {});
   // }
//
//   private static List<Node> createVars(String... vars) {
//      List<Node> nodes = new ArrayList<Node>(vars.length);
//      for (String v : vars) {
//         nodes.add(Node.createVariable(v));
//      }
//      return nodes;
//   }
//
//   private static List<Node> createResources(String... uris) {
//      List<Node> nodes = new ArrayList<Node>(uris.length);
//      for (String n : uris) {
//         nodes.add(Node.createURI(n));
//      }
//      return nodes;
//   }
//
//   private void estimateSelectivityTest(List<Node> subjects,
//         List<Node> objects, Operation op, List<Triple> triples, long expected) {
//      if (!(index instanceof PostgresIndex)) {
//         LOG.warn("Spatial Index is " + index.getClass());
//         return;
//      }
//      BasicPattern pattern = BasicPattern.wrap(triples);
//      OperandFactory<Geometry> of = new SpatialOperandFactory();
//      of.setIndex(index);
//
//      PostgresPropertyFunctionFactory pff = new PostgresPropertyFunctionFactory(new SpatialOperandFactory());
//      PostgresPropertyFunction pf = pff.doCreate(op.getUri());
//      ExecutionContext context = new ExecutionContext(
//                                                      ARQ.getContext(),
//                                                      graph,
//                                                      graphStore,
//                                                      KbOpExecutor.KbOpExecutorFactory);
//
//      PropFuncArg argSubject = new PropFuncArg(subjects);
//      PropFuncArg argObject = new PropFuncArg(objects);
//      Node predicate = Node.createURI(op.getUri());
//      pf.setPattern(pattern);
//      pf.build(argSubject, predicate, argObject, context);
//
//      Map<Node, Operand<Geometry>> operands = new HashMap<Node, Operand<Geometry>>();
//
//      List<Node> allNodes = new ArrayList<Node>(subjects);
//      allNodes.addAll(objects);
//      for (Node n : allNodes) {
//         Operand<Geometry> o = of.createOperand(n, pattern,
//                                                BindingRoot.create());
//         if (null != o) {
//            operands.put(n, o);
//         }
//      }
//      EstimablePropertyFunctionPattern<Geometry> p;
//      p = new EstimablePropertyFunctionPattern<Geometry>(pf, predicate,
//                                                         subjects, objects,
//                                                         operands);
//
//      long selectivity = p.estimate();
//      assertEquals(expected, selectivity);
//   }

   @Ignore
   public void testBuffer() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?place " + Constants.GML_NS +
//      // "representativeExtent ?placeExtent"));
//      // triples.add(createTriple("?placeExtent " + Constants.RCC_NS +
//      // "part ?reg"));
//      triples.add(createTriple("?reg " + RDF.type.getURI() + " "
//            + Constants.BUFFER_NS + "Buffer"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS
//            + "distance \"580\"xsd:double"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS + "extent ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(Node.createVariable("ext"),
//                                Node.createURI(Constants.GML_NS + "pos"),
//                                Node.createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("placeExtent"), createVars("reg"),
//                              Operation.RCC_PART, triples, 3);
   }

   @Ignore
   public void testBufferProperPart() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?place " + Constants.GML_NS +
//      // "representativeExtent ?placeExtent"));
//      // triples.add(createTriple("?placeExtent " + Constants.RCC_NS +
//      // "properPart ?reg"));
//      triples.add(createTriple("?reg " + RDF.type.getURI() + " "
//            + Constants.BUFFER_NS + "Buffer"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS
//            + "distance \"580\"xsd:double"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS + "extent ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"), NodeCreateUtils
//            .create(Constants.GML_NS + "pos"), Node.createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("placeExtent"), createVars("reg"),
//                              Operation.RCC_PROPER_PART, triples, 2);
   }

   @Ignore
   public void testBufferInvProperPart() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?place " + Constants.GML_NS +
//      // "representativeExtent ?placeExtent"));
//      // triples.add(createTriple("?reg " + Constants.RCC_NS +
//      // "invProperPart ?placeExtent"));
//      triples.add(createTriple("?reg " + RDF.type.getURI() + " "
//            + Constants.BUFFER_NS + "Buffer"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS
//            + "distance \"580\"xsd:double"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS + "extent ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"), NodeCreateUtils
//            .create(Constants.GML_NS + "pos"), Node.createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg"), createVars("placeExtent"),
//                              Operation.RCC_INV_PROPER_PART, triples, 2);
   }

   @Ignore
   public void testUnboundBuffer() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//      // triples.add(createTriple("?placeExtent " + Constants.RCC_NS +
//      // "part ?reg"));
//      // triples.add(createTriple("?place " + Constants.GML_NS +
//      // "representativeExtent ?placeExtent"));
//
//      triples.add(createTriple("?x " + "http://someproperty#distance ?y"));
//      triples.add(createTriple("?reg " + RDF.type.getURI() + " "
//            + Constants.BUFFER_NS + "Buffer"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS + "distance ?y"));
//      triples.add(createTriple("?reg " + Constants.BUFFER_NS + "extent ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"), NodeCreateUtils
//            .create(Constants.GML_NS + "pos"), Node.createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("placeExtent"), createVars("reg"),
//                              Operation.RCC_PART, triples, Long.MAX_VALUE);
   }

   @Ignore
   public void testFloating() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg3 " + Constants.RCC_NS +
//      // "part ?circle"));
//      // triples.add(createTriple("?reg2 " + Constants.RCC_NS +
//      // "part ?circle"));
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "part ?circle"));
//      triples.add(createTriple("?circle " + Constants.GML_NS
//            + "radius \"0.1\"http://www.w3.org/2001/XMLSchema#double"));
//      triples.add(createTriple("?circle rdf:type " + Constants.GML_NS
//            + "Circle"));
//      estimateSelectivityTest(createVars("reg1", "reg2", "reg3"),
//                              createVars("circle"), Operation.RCC_PART,
//                              triples, index.size());
//
   }

   @Ignore
   public void testBoundExtentToCheckBoundExtentToCheckAgainst() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple(EXAMPLE1_NS + "point1 " + Constants.RCC_NS +
//      // "disconnected ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples
//            .add(Triple.create(NodeCreateUtils.create("?ext"),
//                               NodeCreateUtils.create(Constants.GML_NS
//                                     + "posList"),
//                               Node.createLiteral("-1 -1 -1 1 1 1 1 -1 -1 -1")));
//
//      estimateSelectivityTest(createResources(EXAMPLE1_NS + "point1"),
//                              createVars("polygon"),
//                              Operation.RCC_DISCONNECTED, triples, 1);
   }

   @Ignore
   public void testBoundExtentToCheckUnboundExtentToCheckAgainst() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple(EXAMPLE1_NS + "point1 " + Constants.RCC_NS +
//      // "disconnected ?polygon"));
//
//      estimateSelectivityTest(createResources(EXAMPLE1_NS + "point1"),
//                              createVars("polygon"),
//                              Operation.RCC_DISCONNECTED, triples, 11);
//      // estimateSelectivityTest(triples, 11);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple(EXAMPLE1_NS + "point1 " + Constants.RCC_NS +
//      // "properPart ?polygon"));
//      estimateSelectivityTest(createResources(EXAMPLE1_NS + "point1"),
//                              createVars("polygon"), Operation.RCC_PROPER_PART,
//                              triples, 2);
//      // estimateSelectivityTest(triples, 2);
   }

   @Ignore
   public void testUnboundExtentToCheckUnboundExtentToCheckAgainst() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "disconnected ?polygon"));
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_DISCONNECTED, triples,
//                              Long.MAX_VALUE);
//      // estimateSelectivityTest(triples, Long.MAX_VALUE);
   }

   @Ignore
   public void testDisconnectedPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "disconnected ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples
//            .add(Triple.create(NodeCreateUtils.create("?ext"),
//                               NodeCreateUtils.create(Constants.GML_NS
//                                     + "posList"),
//                               Node.createLiteral("-1 -1 -1 1 1 1 1 -1 -1 -1")));
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_DISCONNECTED, triples, 6);
//      // estimateSelectivityTest(triples, 6);
   }

   @Ignore
   public void testDisconnectedPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 "
//      // + Constants.RCC_NS
//      // + "disconnected ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_DISCONNECTED, triples, 11);
//      // estimateSelectivityTest(triples, 11);
   }

   @Ignore
   public void testConnectedPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "connected ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples
//            .add(Triple.create(NodeCreateUtils.create("?ext"),
//                               NodeCreateUtils.create(Constants.GML_NS
//                                     + "posList"),
//                               Node.createLiteral("-1 -1 -1 1 1 1 1 -1 -1 -1")));
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_CONNECTED, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testConnectedPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "connected ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_CONNECTED, triples, 2);
//      // estimateSelectivityTest(triples, 2);
   }

   @Ignore
   public void testExternallyConnectedPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "externallyConnected ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_EXT_CONNECTED, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testExternallyConnectedPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "externallyConnected ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_EXT_CONNECTED, triples, 2);
//      // estimateSelectivityTest(triples, 2);
   }

   @Ignore
   public void testPartiallyOverlapsPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "partiallyOverlaps ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      // estimateSelectivityTest(triples, 1);
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_PARTIALLY_OVERLAPS, triples, 1);
   }

   @Ignore
   public void testPartiallyOverlapsPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "partiallyOverlaps ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      // estimateSelectivityTest(triples, 2);
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_PARTIALLY_OVERLAPS, triples, 2);
   }

   @Ignore
   public void testTangentialProperPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "tangentialProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testTangentialProperPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "tangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("34.845206 35.915989")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "tangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testNonTangentialProperPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "nonTangentialProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_NON_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   /**
    * Test whether any geometries are non tangential proper parts of a point. As
    * a query, this should always be 0 as nothing can be a non tangential proper
    * part of a point, but the estimate will return a count of any points that
    * match it.
    */
   @Ignore
   public void testNonTangentialProperPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "nonTangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      // estimateSelectivityTest(triples, 1);
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_NON_TAN_PROPER_PART, triples, 1);
   }

   @Ignore
   public void testPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "part ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "part ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      // estimateSelectivityTest(triples, 2);
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_PART, triples, 2);
   }

   @Ignore
   public void testProperPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "properPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      // estimateSelectivityTest(triples, 1);
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_PROPER_PART, triples, 1);
   }

   /**
    * This should be one (when testing with an existing point in the db) and 0
    * otherwise.
    */
   @Ignore
   public void testProperPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "properPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "properPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("-0.5 -0.5")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_PROPER_PART, triples, 0);
//      // estimateSelectivityTest(triples, 0);
   }

   @Ignore
   public void testInverseTangentialProperPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // polygon that isn't contained by any polygon in the DB
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invTangentialProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_TAN_PROPER_PART, triples, 0);
//      // estimateSelectivityTest(triples, 0);
//
//      triples.clear();
//      // polygon that is contained in only one polygon in the DB
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invTangentialProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 0.5 1 0.5 0 0 0")));
//
//      // estimateSelectivityTest(triples, 1);
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_TAN_PROPER_PART, triples, 1);
   }

   @Ignore
   public void testInverseTangentialProperPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invTangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_TAN_PROPER_PART, triples, 2);
//      // estimateSelectivityTest(triples, 2);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invTangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("-0.5 -0.5")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testInverseNonTangentialProperPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // polygon that isn't contained by any polygon in the DB
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invNonTangentialProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_NON_TAN_PROPER_PART, triples, 0);
//      // estimateSelectivityTest(triples, 0);
//
//      triples.clear();
//      // polygon that is contained in only one polygon in the DB
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invNonTangentialProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 0.5 1 0.5 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_NON_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testInverseNonTangentialProperPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invTangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_TAN_PROPER_PART, triples, 2);
//      // estimateSelectivityTest(triples, 2);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invTangentialProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("-0.5 -0.5")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_TAN_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testInversePartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS +
//      // "invPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 6 0 6 1 0 1 0 0")));
//
//      // estimateSelectivityTest(triples, 3);
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_PART, triples, 3);
   }

   @Ignore
   public void testInversePartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_PART, triples, 2);
//      // estimateSelectivityTest(triples, 2);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("-0.5 -0.5")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testInverseProperPartPolygon() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 1 1 1 1 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_PROPER_PART, triples, 0);
//      // estimateSelectivityTest(triples, 0);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invProperPart ?polygon"));
//      triples.add(createTriple("?polygon " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Polygon"));
//      triples
//            .add(createTriple("?polygon " + Constants.GML_NS + "exterior ?ext"));
//      triples.add(createTriple("?ext " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "LinearRing"));
//      triples.add(Triple.create(NodeCreateUtils.create("?ext"),
//                                NodeCreateUtils.create(Constants.GML_NS
//                                      + "posList"),
//                                Node.createLiteral("0 0 0 5 0.5 5 0.5 0 0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("polygon"),
//                              Operation.RCC_INV_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

   @Ignore
   public void testInverseProperPartPoint() {
//      if (!(index instanceof PostgresIndex)) {
//         return;
//      }
//      loadData("example/BuildingExample1.ttl");
//      loadData("example/BuildingExample2.ttl");
//      loadData("example/BuildingExample3.ttl");
//
//      List<Triple> triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("0 0")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_PROPER_PART, triples, 2);
//      // estimateSelectivityTest(triples, 2);
//
//      triples = new ArrayList<Triple>();
//
//      // triples.add(createTriple("?reg1 " + Constants.RCC_NS
//      // + "invProperPart ?point"));
//      triples.add(createTriple("?point " + RDF.type.getURI() + " "
//            + Constants.GML_NS + "Point"));
//      triples.add(Triple.create(NodeCreateUtils.create("?point"),
//                                NodeCreateUtils
//                                      .create(Constants.GML_NS + "pos"), Node
//                                      .createLiteral("-0.5 -0.5")));
//
//      estimateSelectivityTest(createVars("reg1"), createVars("point"),
//                              Operation.RCC_INV_PROPER_PART, triples, 1);
//      // estimateSelectivityTest(triples, 1);
   }

}
