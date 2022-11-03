// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;


/** @author dkolas */
public final class VariableBindingInfo {
	//Needs a map of triples to variables
	private int[][] tripleVariables;

	//Needs to know whether or not a variable is unbound, just bound, or bound
	private int[] isBound;

	//Keep track of counts for things in the current domain
	private long[][] variableCounts;

	public VariableBindingInfo(QueryTriple[] triples){
		tripleVariables = new int[triples.length][3];
		int varCount=0;

		initTripleVariables();

		for (int i=0; i<triples.length; i++){
			if (triples[i].S.index != QueryNode.NO_INDEX){
				tripleVariables[i][0] = triples[i].S.index;
				if (triples[i].S.index +1 > varCount){
					varCount = triples[i].S.index + 1;
				}
			}
			if (triples[i].P.index != QueryNode.NO_INDEX){
				tripleVariables[i][1] = triples[i].P.index;
				if (triples[i].P.index +1 > varCount){
					varCount = triples[i].P.index + 1;
				}
			}
			if (triples[i].O.index != QueryNode.NO_INDEX){
				tripleVariables[i][2] = triples[i].O.index;
				if (triples[i].O.index +1 > varCount){
					varCount = triples[i].O.index + 1;
				}
			}
		}
		isBound = new int[varCount];
		variableCounts = new long[varCount][3];
	}

	private void initTripleVariables() {
		for (int i=0; i<tripleVariables.length; i++){
			tripleVariables[i][0]=-1;
			tripleVariables[i][1]=-1;
			tripleVariables[i][2]=-1;
		}
	}

	public void setCounts(int var, long sCount, long pCount, long oCount){
		variableCounts[var][0] = sCount;
		variableCounts[var][1] = pCount;
		variableCounts[var][2] = oCount;
	}

	public void bind(int var){
		isBound[var] = 2;
	}

	public void unbind(int var){
		isBound[var] = 0;
	}

	public void justBind(int var){
		isBound[var] = 1;
	}

	public boolean isBound(int var){
		return isBound[var] == 2;
	}

	public boolean isJustBound(int var){
		return isBound[var] == 1;
	}

	public long[] getCounts(int var){
		return variableCounts[var];
	}

	public long[][] getVariableValues() {
		return variableCounts;
	}

	public int[] getIsBound() {
		return isBound;
	}

	public int[] getTripleVars(int triple) {
		return tripleVariables[triple];
	}

	public void finishBinds(Domain domain, KbGraph graph) {
		for (int i=0; i<isBound.length; i++){
			if (isBound[i]==1){
				variableCounts[i][0] = graph.getNodeCountInPosition(domain.getElement(i), 1);
				variableCounts[i][1] = graph.getNodeCountInPosition(domain.getElement(i), 2);
				variableCounts[i][2] = graph.getNodeCountInPosition(domain.getElement(i), 3);
				isBound[i]=2;
			}
		}
	}
}
