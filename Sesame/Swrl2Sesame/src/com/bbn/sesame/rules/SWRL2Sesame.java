// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SWRL2Sesame.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.rules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.w3._2003._11.ruleml.Imp;
import org.w3._2003._11.swrlx.Ontology;

public class SWRL2Sesame
{
   @SuppressWarnings({ "rawtypes", "deprecation" })
   public static List<SWRLRule> getRules(String uri) throws Exception
   {
      List<SWRLRule> rules = new ArrayList<>();

      JAXBContext jc = JAXBContext.newInstance("org.w3._2003._11.swrlx");
      Unmarshaller u = jc.createUnmarshaller();

      u.setValidating(true);

      Ontology ont = (Ontology) u.unmarshal(new File(uri));
      List list = ont.getVersionInfoOrPriorVersionOrBackwardCompatibleWith();
      for (Object o : list)
      {
         if (o instanceof Imp)
         {
            rules.add(new SWRLRule((Imp) o));
         }
      }

      return rules;
   }
}
