// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.queryoptimization;

import java.util.ArrayList;
import java.util.List;

/** @author dkolas */
public class Constraint
{
	private List<Integer> variables = new ArrayList<>();
	private long          maximumProduct = 0;

	public void addVariable(int x)
	{
		if (!variables.contains(x))
		{
			variables.add(x);
		}
	}

	public boolean hasVariable(int x)
	{
		return variables.contains(x);
	}

	public List<Integer> getVariables()
	{
		return variables;
	}

	public long getMaximumProduct()
	{
		return maximumProduct;
	}

	public void setMaximumProduct(long maximumProduct)
	{
		this.maximumProduct = maximumProduct;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int i = 0; i < variables.size(); i++)
		{
			builder.append(" V" + variables.get(i) + " ");
			if (i != variables.size() - 1)
			{
				builder.append("*");
			}
		}
		builder.append(" <= " + maximumProduct + " ]");
		return builder.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Constraint))
		{
			return false;
		}
		else
		{
			Constraint otherConstraint = (Constraint) o;
			if (otherConstraint.maximumProduct != this.maximumProduct)
			{
				return false;
			}
			return checkLists(otherConstraint.variables);
		}
	}

	private boolean checkLists(List<Integer> otherVariables)
	{
		if (variables.size() != otherVariables.size())
		{
			return false;
		}
		for (Integer i : otherVariables)
		{
			if (!variables.contains(i))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = Long.valueOf(maximumProduct).hashCode();
		for (Integer i : variables)
		{
			result ^= i.hashCode();
		}
		return result;
	}
}
