// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.queryoptimization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author dkolas */
public class TreeWidthEstimatorTest {
	@SuppressWarnings("static-method")
	@BeforeEach
	public void beforeEach() {
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@SuppressWarnings("static-method")
	@Test
	public void test1(){
		// Vx = 10
		// VxVy = 15
		// VyVz = 7
		// Vz = 2
		Constraint constraint1 = new Constraint();
		constraint1.addVariable(0);
		constraint1.setMaximumProduct(10l);
		Constraint constraint2 = new Constraint();
		constraint2.addVariable(0);
		constraint2.addVariable(1);
		constraint2.setMaximumProduct(15l);
		Constraint constraint3 = new Constraint();
		constraint3.addVariable(1);
		constraint3.addVariable(2);
		constraint3.setMaximumProduct(7l);
		Constraint constraint4 = new Constraint();
		constraint4.addVariable(2);
		constraint4.setMaximumProduct(2l);

		TreeWidthEstimator estimator = new TreeWidthEstimator(3);
		estimator.pushConstraint(constraint1);
		assertEquals(10, estimator.calculateWidth());

		estimator.pushConstraint(constraint2);
		assertEquals(15, estimator.calculateWidth());

		estimator.removeLastConstraint();
		assertEquals(10, estimator.calculateWidth());

		estimator.pushConstraint(constraint2);
		estimator.pushConstraint(constraint3);
		assertEquals(70, estimator.calculateWidth());

		estimator.removeLastConstraint();
		assertEquals(15, estimator.calculateWidth());

		estimator.pushConstraint(constraint3);
		estimator.pushConstraint(constraint4);
		assertEquals(30, estimator.calculateWidth());
	}
}
