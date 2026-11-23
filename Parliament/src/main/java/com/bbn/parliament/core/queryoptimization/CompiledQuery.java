// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.core.queryoptimization;

/** @author dkolas */
public final class CompiledQuery
{
	private long[][]         triples;
	private boolean[][]      isVariable;
	public static final long VARIABLE_NOT_BOUND = -1;
	private boolean[]        used;
	private double           kbSize;

	public CompiledQuery(long[][] triples, boolean[][] isVariable, long kbSize)
	{
		this.triples = triples;
		this.isVariable = isVariable;
		assert (triples.length == isVariable.length);
		assert (triples[0].length == 3);
		assert (isVariable[0].length == 3);
		used = new boolean[triples.length];
		this.kbSize = kbSize;
	}

	public int findMinQuery(long[][] variableValues, int[] isBound)
	{
		double minValue = Double.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < triples.length; i++)
		{
			if (!used[i])
			{
				double eval = estimateTriple(i, variableValues, isBound);
				if (eval < minValue)
				{
					minValue = eval;
					minIndex = i;
				}
			}
		}
		return minIndex;
	}

	private double estimateTriple(int i, long[][] variableValues, int[] isBound)
	{
		double minValue = Double.MAX_VALUE;
		double[] values = new double[3];
		for (int j = 0; j < 3; j++)
		{
			values[j] = getValue(i, j, variableValues, isBound);
			if (values[j] < minValue)
			{
				minValue = values[j];
			}
		}
		return values[0] * (values[1] / kbSize) * (values[2] / kbSize);
	}

	private double getValue(int i, int j, long[][] variableValues, int[] isBound)
	{
		if (isVariable[i][j])
		{
			long var = triples[i][j];
			if (isBound[(int) var] == 2)
			{
				return variableValues[(int) var][j];
			}
			else
			{
				return kbSize;
			}
		}
		else
		{
			return triples[i][j];
		}
	}

	public void markUsed(int triple)
	{
		used[triple] = true;
	}

	public void markUnused(int triple)
	{
		used[triple] = false;
	}

	public int findAnyQuery()
	{
		for (int i = 0; i < used.length; i++)
		{
			if (!used[i])
			{
				return i;
			}
		}
		return 0;
	}

	public int countUsed()
	{
		int i = 0;
		for (int j = 0; j < used.length; j++)
		{
			if (used[j])
			{
				i++;
			}
		}
		return i;
	}
}
