// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;

/** @author dkolas */
public class InstantAdder extends TIPBuiltin {
	public InstantAdder(TemporalIndex index) {
		super(index);
	}

	/** @see com.hp.hpl.jena.reasoner.rulesys.Builtin#getName() */
	@Override
	public String getName() {
		return "instant";
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {
		Node instant = getArg(0, args, context);
		Node time = getArg(1, args, context);
		if (!(instant.isURI() || instant.isBlank())) {
			throw new BuiltinException(this, context,
				"Builtin %1$s must have a resource as its 1st argument."
				.formatted(getName()));
		}

		if (!time.isLiteral()) {
			throw new BuiltinException(this, context,
				"Builtin %1$s must have a literal xsd:dateTime as its 2nd and 3rd arguments."
				.formatted(getName()));
		}
		// TemporalInstant temporalInstant = new TemporalInstant(time);
		// getTemporalIndexProcessor().addExtent(instant, temporalInstant);
	}

	@Override
	public int getArgLength() {
		return 2;
	}
}
