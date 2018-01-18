// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.sesame.sail;

import org.openrdf.sesame.config.SailConfig;

public class KbRdfRepositoryConfig extends SailConfig
{
   private static final String PARAM_NAME = "dir";

   public KbRdfRepositoryConfig(boolean useSchemaRepository, String kbDir)
   {
      super(useSchemaRepository
         ? "com.bbn.parliament.sesame.sail.KbRdfSchemaRepository"
         : "com.bbn.parliament.sesame.sail.KbRdfRepository");
      setKbDir(kbDir);
   }

   public String getKbDir()
   {
      return getParameter(PARAM_NAME);
   }

   public void setKbDir(String kbDir)
   {
      setParameter(PARAM_NAME, kbDir);
   }
}
