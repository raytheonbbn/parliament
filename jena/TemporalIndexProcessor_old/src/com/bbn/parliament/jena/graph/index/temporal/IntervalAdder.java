// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;

/**
 * @author dkolas
 */
public class IntervalAdder extends TIPBuiltin {
	public IntervalAdder(TemporalIndex index) {
		super(index);
	}

	@Override
	public String getName() {
		return "timeInterval";
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {
		Node interval = getArg(0, args, context);
		Node startTime = getArg(1, args, context);
		Node endTime = getArg(2, args, context);
		if (!(interval.isURI() || interval.isBlank())){
			throw new BuiltinException(this, context,"Builtin "+getName()+" must have a resource as its first argument.");
		}

		if (!(startTime.isLiteral() && endTime.isLiteral())){
			throw new BuiltinException(this, context,"Builtin "+getName()+" must have a literal xsdDateTime as its second and third arguments.");
		}

       if (!(startTime.getLiteralValue() instanceof XSDDateTime && endTime.getLiteralValue() instanceof XSDDateTime)) {
          throw new BuiltinException(this, context,"Builtin "+getName()+" must have a literal xsdDateTime as its second and third arguments.");
       }
       long startValue = ((XSDDateTime)startTime.getLiteralValue()).asCalendar().getTimeInMillis();
       long endValue = ((XSDDateTime)endTime.getLiteralValue()).asCalendar().getTimeInMillis();
		TemporalInterval extent = new TemporalInterval();
		TemporalInstant start = new TemporalInstant(startValue, extent, true);
		TemporalInstant end = new TemporalInstant(endValue, extent, false);
		extent.setStart(start);
		extent.setEnd(end);
//		getTemporalIndexProcessor().addExtent(interval, extent);
	}

	@Override
	public int getArgLength() {
		return 3;
	}
}
