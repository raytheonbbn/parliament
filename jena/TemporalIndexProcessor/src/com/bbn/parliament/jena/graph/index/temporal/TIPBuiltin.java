// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal;

import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

/** @author dkolas */
public abstract class TIPBuiltin extends BaseBuiltin {
	private TemporalIndex index;

	public TIPBuiltin(TemporalIndex index) {
		this.index = index;
	}

	protected TemporalIndex getTemporalIndex(){
		return index;
	}
}
