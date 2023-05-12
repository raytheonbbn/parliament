// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.configuration;

import org.apache.jena.reasoner.Reasoner;

import com.bbn.parliament.server.exception.ConfigurationException;

/** @author rbattle */
public interface ReasonerConfigurationHandler extends ConfigurationHandler {
	public void configure(Reasoner reasoner) throws ConfigurationException;
}