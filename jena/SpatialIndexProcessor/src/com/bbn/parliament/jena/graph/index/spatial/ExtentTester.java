// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.IntersectionMatrix;
import org.locationtech.jts.geom.Point;

/** @author Robert Battle */
public interface ExtentTester {
	/**
	 * Test two extents.
	 *
	 * @param extent1 The first extent.
	 * @param extent2 The second extent.
	 * @return <code>true</code> if the relationship between extent1 and extent2
	 *         is <code>true</code>, otherwise <code>false</code>.
	 */
	public boolean testExtents(Geometry extent1, Geometry extent2);

	public IntersectionMatrix[] getIntersectionMatrix();

	public static class MatrixTester implements ExtentTester {
		private IntersectionMatrix[] matrices;

		public MatrixTester(String... values) {
			this.matrices = Helper.create(values);
		}

		@Override
		public boolean testExtents(Geometry extent1, Geometry extent2) {
			return Helper.testRelation(extent1, extent2, matrices);
		}

		@Override
		public IntersectionMatrix[] getIntersectionMatrix() {
			return matrices;
		}
	}

	public static class Helper {
		public static final ExtentTester ALWAYS_MATCH = new MatrixTester("*********");

		public static ExtentTester invert(final ExtentTester tester) {
			return new ExtentTester() {

				@Override
				public boolean testExtents(Geometry extent1, Geometry extent2) {
					return tester.testExtents(extent2, extent1);
				}

				@Override
				public IntersectionMatrix[] getIntersectionMatrix() {
					IntersectionMatrix[] matrix = tester.getIntersectionMatrix();
					IntersectionMatrix[] transpose = new IntersectionMatrix[matrix.length];
					for (int i = 0; i < matrix.length; i++) {
						transpose[i] = matrix[i].transpose();
					}
					return transpose;
				}
			};
		}

		public static boolean testRelation(Geometry g1, Geometry g2,
			IntersectionMatrix[] matrices) {
			IntersectionMatrix relation = g1.relate(g2);
			String actual = relation.toString();
			for (IntersectionMatrix matrix : matrices) {
				String desired = matrix.toString();
				if (IntersectionMatrix.matches(actual, desired)) {
					return true;
				}
			}
			return false;
		}

		public static IntersectionMatrix[] create(String... values) {
			IntersectionMatrix[] matrices = new IntersectionMatrix[values.length];
			for (int i = 0; i < values.length; i++) {
				matrices[i] = new IntersectionMatrix(values[i]);
			}
			return matrices;
		}
	}

	public static class SimpleFeatures {
		public static final ExtentTester EQUALS = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				if (extent1 instanceof Point && extent2 instanceof Point) {
					return extent1.equals(extent2);
				} else {
					return Helper.testRelation(extent1, extent2,
						getIntersectionMatrix());
				}
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("TFFFTFFFT");
			}
		};

		public static final ExtentTester DISJOINT = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.disjoint(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("FF*FF****");
			}
		};

		public static final ExtentTester INTERSECTS = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.intersects(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T********", "*T*******", "***T*****",
					"****T****");
			}
		};

		public static final ExtentTester TOUCHES = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.touches(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("FT*******", "F**T*****", "F***T****");
			}
		};

		public static final ExtentTester WITHIN = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.within(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T*F**F***");
			}
		};

		public static final ExtentTester CONTAINS = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.contains(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T******FF*");
			}
		};

		public static final ExtentTester OVERLAPS = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.overlaps(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T*T***T**");
			}
		};

		public static final ExtentTester CROSSES = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				extent1.covers(extent2);
				return extent1.crosses(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T*T******", "T*****T**", "0********");
			}
		};
	}

	public static class Egenhofer {
		public static final ExtentTester EQUALS = SimpleFeatures.EQUALS;
		public static final ExtentTester DISJOINT = SimpleFeatures.DISJOINT;
		public static final ExtentTester MEET = SimpleFeatures.TOUCHES;
		public static final ExtentTester OVERLAP = SimpleFeatures.OVERLAPS;

		//public static final ExtentTester COVERS = new MatrixTester("T*TFT*FF*");

		public static final ExtentTester COVERS = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.covers(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T*****FF*", "*T****FF*", "***T**FF*", "****T*FF*");
			}
		};

		//public static final ExtentTester COVEREDBY = new MatrixTester("TFF*TFT**");

		public static final ExtentTester COVEREDBY = new ExtentTester() {
			@Override
			public boolean testExtents(Geometry extent1, Geometry extent2) {
				return extent1.coveredBy(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrix() {
				return Helper.create("T*F**F***", "*TF**F***", "**FT*F***", "**F*TF***");
			}
		};

		public static final ExtentTester INSIDE = new MatrixTester("TFF*FFT**");
		public static final ExtentTester CONTAINS = new MatrixTester("T*TFF*FF*");
	}

	public static class RCC8 {
		public static final ExtentTester EQ = SimpleFeatures.EQUALS;
		public static final ExtentTester DC = SimpleFeatures.DISJOINT;
		public static final ExtentTester EC = new MatrixTester("FFTFTTTTT");
		public static final ExtentTester PO = new MatrixTester("TTTTTTTTT");
		public static final ExtentTester TPPI = new MatrixTester("TTTFTTFFT");
		public static final ExtentTester TPP = new MatrixTester("TFFTTFTTT");
		public static final ExtentTester NTPP = new MatrixTester("TFFTFFTTT");
		public static final ExtentTester NTPPI = new MatrixTester("TTTFFTFFT");
	}
}
