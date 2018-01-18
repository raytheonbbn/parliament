package com.bbn.parliament.jena.graph.index.spatial.jts;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractFloatingExtentsTest;

public class JTSFloatingExtentsTest extends AbstractFloatingExtentsTest {

   @Override
   protected Properties getProperties() {
      return JTSPropertyFactory.create();
   }

}
