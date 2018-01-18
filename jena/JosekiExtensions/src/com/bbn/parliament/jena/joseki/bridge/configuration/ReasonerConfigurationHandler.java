// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.joseki.bridge.configuration;

import com.hp.hpl.jena.reasoner.Reasoner;

/**
 * @author rbattle
 *
 */
public interface ReasonerConfigurationHandler extends ConfigurationHandler {
   public void configure(Reasoner reasoner) throws ConfigurationException;
}
