package com.bbn.parliament.jena.graph.index.spatial.rtree;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractFloatingExtentsTest;

public class RTreeFloatingExtentsTest extends AbstractFloatingExtentsTest {

   @Override
   protected Properties getProperties() {
      return RTreePropertyFactory.create();
   }

}
