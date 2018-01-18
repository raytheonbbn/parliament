// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.queryoptimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author dkolas */
public class TreeWidthEstimator
{
	private Stack<Constraint>      constraints;
	private List<List<Constraint>> bucketedConstraints;
	private int                    numVariables;
	private static int             iterationCounter;
	private static Logger          logger = LoggerFactory.getLogger(TreeWidthEstimator.class);

	public TreeWidthEstimator(int numVariables)
	{
		constraints = new Stack<>();
		this.numVariables = numVariables;
	}

	public void pushConstraint(Constraint constraint)
	{
		constraints.push(constraint);
	}

	public void removeLastConstraint()
	{
		constraints.pop();
	}

	public long calculateWidth()
	{
		bucketConstraints();
		return calculateEstimate();
	}

	private void bucketConstraints()
	{
		bucketedConstraints = new ArrayList<>();
		for (int i = 0; i < numVariables; i++)
		{
			bucketedConstraints.add(new ArrayList<Constraint>());
		}
		for (Constraint constraint : constraints)
		{
			for (Integer integer : constraint.getVariables())
			{
				bucketedConstraints.get(integer).add(constraint);
			}
		}
	}

	private long calculateEstimate()
	{
		iterationCounter = 0;
		long result = calculateEstimate(0, 1, new ArrayList<Constraint>());
		logger.debug("Size: {}, Iteration count: {}", constraints.size(), iterationCounter);
		return result;
	}

	private long calculateEstimate(int variableStart, long precedingEstimate,
		List<Constraint> usedConstraints)
	{
		iterationCounter++;
		if (variableStart == numVariables)
		{
			return precedingEstimate;
		}
		if (bucketedConstraints.get(variableStart).size() != 0)
		{
			long min = Long.MAX_VALUE;
			for (Constraint constraint : bucketedConstraints.get(variableStart))
			{
				long thisEstimate = Long.MAX_VALUE;
				if (usedConstraints.contains(constraint))
				{
					thisEstimate = calculateEstimate(variableStart + 1, precedingEstimate,
						usedConstraints);
				}
				else
				{
					usedConstraints.add(constraint);
					thisEstimate = calculateEstimate(variableStart + 1, precedingEstimate
						* constraint.getMaximumProduct(), usedConstraints);
					usedConstraints.remove(constraint);
				}
				if (thisEstimate < min)
				{
					min = thisEstimate;
				}
			}
			return min;
		}
		else
		{
			// There are no constraints for this variable; thus, this
			// variable has not yet been bound
			return calculateEstimate(variableStart + 1, precedingEstimate, usedConstraints);
		}
	}
}
