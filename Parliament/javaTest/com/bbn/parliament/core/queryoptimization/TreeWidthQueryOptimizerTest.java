// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.core.queryoptimization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** @author dkolas */
public class TreeWidthQueryOptimizerTest {
	@SuppressWarnings("static-method")
	@Test
	public void test1() {
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

		List<Constraint> constraints = new ArrayList<>();
		constraints.add(constraint1);
		constraints.add(constraint2);
		constraints.add(constraint3);
		constraints.add(constraint4);

		TreeWidthQueryOptimizer optimizer = new TreeWidthQueryOptimizer(new ArrayList<Constraint>(), constraints, 3);

		checkOptimizerResult(new int[]{3,2,1,0}, optimizer.optimizeConstraints());
	}

	private static void checkOptimizerResult(int[] expected, List<Integer> actual) {
		assertEquals(expected.length, actual.size());
		for (int i=0; i<expected.length; i++) {
			assertEquals(expected[i], actual.get(i).intValue());
		}
	}
}
