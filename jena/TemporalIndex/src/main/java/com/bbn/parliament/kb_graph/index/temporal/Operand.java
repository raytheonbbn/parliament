// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal;

/** @author dkolas */
public enum Operand {
	INTERVAL_OVERLAPS(Constants.OT_NS, "intervalOverlaps", ExtentTester.INTERVAL_OVERLAPS),
	INTERVAL_BEFORE(Constants.OT_NS, "intervalBefore", ExtentTester.INTERVAL_BEFORE),
	INTERVAL_AFTER(Constants.OT_NS, "intervalAfter", ExtentTester.INTERVAL_AFTER),
	INTERVAL_STARTS_BEFORE(Constants.OT_NS, "intervalStartsBefore", ExtentTester.INTERVAL_STARTS_BEFORE),
	INTERVAL_FINISHES_AFTER(Constants.OT_NS, "intervalFinishesAfter", ExtentTester.INTERVAL_FINISHES_AFTER),
	INTERVAL_EQUALS(Constants.OT_NS, "intervalEquals", ExtentTester.EQUALS),
	INTERVAL_MEETS(Constants.OT_NS, "intervalMeets", ExtentTester.INTERVAL_MEETS),
	INTERVAL_STARTS(Constants.OT_NS, "intervalStarts", ExtentTester.INTERVAL_STARTS),
	INTERVAL_DURING(Constants.OT_NS, "intervalDuring", ExtentTester.INTERVAL_DURING),
	INTERVAL_FINISHES(Constants.OT_NS, "intervalFinishes", ExtentTester.INTERVAL_FINISHES),
	INTERVAL_MET_BY(Constants.OT_NS, "intervalMetBy", ExtentTester.INTERVAL_MET_BY),
	INTERVAL_OVERLAPPED_BY(Constants.OT_NS, "intervalOverlappedBy", ExtentTester.INTERVAL_OVERLAPPED_BY),
	INTERVAL_STARTED_BY(Constants.OT_NS, "intervalStartedBy", ExtentTester.INTERVAL_STARTED_BY),
	INTERVAL_CONTAINS(Constants.OT_NS, "intervalContains", ExtentTester.INTERVAL_CONTAINS),
	INTERVAL_FINISHED_BY(Constants.OT_NS, "intervalFinishedBy", ExtentTester.INTERVAL_FINISHED_BY),

	BEFORE(Constants.OT_NS, "before", ExtentTester.BEFORE),
	AFTER(Constants.OT_NS, "after", ExtentTester.AFTER),
	HAS_BEGINNING(Constants.OT_NS, "hasBeginning", ExtentTester.HAS_BEGINNING),
	HAS_END(Constants.OT_NS, "hasEnd", ExtentTester.HAS_END),
	INSIDE(Constants.OT_NS, "inside", ExtentTester.INSIDE),

	INSTANT_EQUALS(Constants.PT_NS, "instantEquals", ExtentTester.INSTANT_EQUALS);



	private String _uri;
	private ExtentTester _extentTester;

	private Operand(String namespace, String relativeUri, ExtentTester extentTester) {
		_uri = namespace + relativeUri;
		_extentTester = extentTester;
	}

	public String getUri() {
		return _uri;
	}

	public ExtentTester getExtentTester() {
		return _extentTester;
	}

	public static Operand valueOfUri(String uri) {
		for (Operand op : values()) {
			if (op.getUri().equals(uri)) {
				return op;
			}
		}
		throw new IllegalArgumentException("'%1$s' is not a valid Operand URI".formatted(uri));
	}
}

