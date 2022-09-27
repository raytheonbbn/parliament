// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;

/** @author dkolas */
public interface ExtentTester {
	/**
	 * Test two extents.
	 *
	 * @param x The first extent.
	 * @param y The second extent.
	 * @return <code>true</code> if the relationship between extent1 and extent2
	 *         is <code>true</code>, otherwise <code>false</code>.
	 */
	public boolean testExtents(TemporalExtent x, TemporalExtent y);

	// RCC operands
	public static final ExtentTester INTERVAL_OVERLAPS = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() < y.getStart().getInstant() &&
					x.getEnd().getInstant() > y.getStart().getInstant() &&
					x.getEnd().getInstant() < y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_OVERLAPPED_BY = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return y.getStart().getInstant() < x.getStart().getInstant() &&
					y.getEnd().getInstant() > x.getStart().getInstant() &&
					y.getEnd().getInstant() < x.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_BEFORE = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getEnd().getInstant() < y.getStart().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_AFTER = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() > y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_STARTS_BEFORE = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() < y.getStart().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_FINISHES_AFTER = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getEnd().getInstant() > y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_MEETS = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getEnd().getInstant() == y.getStart().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_MET_BY = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return y.getEnd().getInstant() == x.getStart().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_STARTS = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() == y.getStart().getInstant() &&
					x.getEnd().getInstant() < y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_STARTED_BY = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return y.getStart().getInstant() == x.getStart().getInstant() &&
					y.getEnd().getInstant() < x.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_DURING = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() > y.getStart().getInstant() &&
					x.getEnd().getInstant() < y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_CONTAINS = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return y.getStart().getInstant() > x.getStart().getInstant() &&
					y.getEnd().getInstant() < x.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_FINISHED_BY = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() < y.getStart().getInstant() &&
					x.getEnd().getInstant() == y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INTERVAL_FINISHES = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() > y.getStart().getInstant() &&
					x.getEnd().getInstant() == y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester EQUALS = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInterval) {
				return x.getStart().getInstant() == y.getStart().getInstant() &&
					x.getEnd().getInstant() == y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester BEFORE = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			return x.getStart().getInstant() < y.getStart().getInstant() &&
				x.getEnd().getInstant() < y.getEnd().getInstant();
		}
	};

	public static final ExtentTester AFTER = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			return x.getStart().getInstant() > y.getStart().getInstant() &&
				x.getEnd().getInstant() > y.getEnd().getInstant();
		}
	};

	public static final ExtentTester HAS_BEGINNING = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (y instanceof TemporalInstant) {
				return x.getStart().getInstant() == y.getStart().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester HAS_END = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (y instanceof TemporalInstant) {
				return x.getEnd().getInstant() == y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INSTANT_EQUALS = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInstant && y instanceof TemporalInstant) {
				return x.getStart().getInstant() == y.getStart().getInstant() &&
					x.getEnd().getInstant() == y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};

	public static final ExtentTester INSIDE = new ExtentTester() {
		@Override
		public boolean testExtents(TemporalExtent x, TemporalExtent y) {
			if (x instanceof TemporalInterval && y instanceof TemporalInstant) {
				return x.getStart().getInstant() < y.getStart().getInstant() &&
					x.getEnd().getInstant() > y.getEnd().getInstant();
			} else {
				return false;
			}
		}
	};
}
