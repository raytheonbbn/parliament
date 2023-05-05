// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.core.queryoptimization;

import java.util.ArrayList;
import java.util.List;

/** @author dkolas */
public class TreeWidthQueryOptimizer
{
	private List<Constraint>   fixedConstraints;
	private List<Constraint>   constraints;
	private TreeWidthEstimator treeWidthEstimator;

	public TreeWidthQueryOptimizer(List<Constraint> fixedConstraints,
		List<Constraint> constraintsToOptimize, int numVariables)
	{
		this.fixedConstraints = fixedConstraints;
		this.constraints = constraintsToOptimize;
		this.treeWidthEstimator = new TreeWidthEstimator(numVariables);

		for (Constraint constraint : this.fixedConstraints)
		{
			treeWidthEstimator.pushConstraint(constraint);
		}
	}

	public List<Integer> optimizeConstraints()
	{
		List<Integer> result = new ArrayList<>();
		optimizeConstraints(result);
		return result;
	}

	private void optimizeConstraints(List<Integer> result)
	{
		if (result.size() == constraints.size())
		{
			return;
		}
		long min = Long.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < constraints.size(); i++)
		{
			if (!result.contains(i))
			{
				treeWidthEstimator.pushConstraint(constraints.get(i));
				long thisValue = treeWidthEstimator.calculateWidth();
				if (thisValue < min)
				{
					min = thisValue;
					minIndex = i;
				}
				treeWidthEstimator.removeLastConstraint();
			}
		}
		result.add(minIndex);
		treeWidthEstimator.pushConstraint(constraints.get(minIndex));
		optimizeConstraints(result);
	}
}
