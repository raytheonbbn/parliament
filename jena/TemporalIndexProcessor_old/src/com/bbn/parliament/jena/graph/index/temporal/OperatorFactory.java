// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

/** @author dkolas */
public interface OperatorFactory {
	public TemporalPropertyFunction<?> getOperator(Operand operand);
}
