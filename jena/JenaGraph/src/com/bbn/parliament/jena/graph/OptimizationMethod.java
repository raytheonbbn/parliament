// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

/** @author dkolas */
public enum OptimizationMethod {

	DefaultOptimization,

	TreeWidthEstimation,

	/** Changes order on fly at every possible juncture (we suspect reads counts too often) */
	DynamicOptimization,

	UpdatedStaticOptimization
}
