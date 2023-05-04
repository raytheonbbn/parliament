package com.bbn.parliament.jena.graph.index.spatial;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.util.iterator.NiceIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.IntersectionMatrix;
import org.locationtech.jts.geom.Point;

import com.bbn.parliament.kb_graph.index.Record;

public abstract class Operation {
	public final boolean relate(Geometry a, Geometry b) {
		if (a.isValid() && b.isValid()) {
			return doRelate(a, b);
		}
		return false;
	}

	protected abstract boolean doRelate(Geometry a, Geometry b);
	public abstract IntersectionMatrix[] getIntersectionMatrices();

	public static class OperationIterator implements ClosableIterator<Record<Geometry>> {
		private final Iterator<Record<Geometry>> it;
		private final Operation op;
		private final Geometry geom;

		private Record<Geometry> current;

		private boolean hasBeenNexted = true;
		private boolean hasNextValue = false;

		public OperationIterator(Iterator<Record<Geometry>> it, Geometry geom,
			Operation op) {
			this.it = it;
			this.op = op;
			this.geom = geom;
		}

		@Override
		public boolean hasNext() {
			if (!hasBeenNexted) {
				return hasNextValue;
			}

			hasNextValue = false;
			while (it.hasNext()) {
				Record<Geometry> r = it.next();
				if (op.relate(r.getValue(), geom)) {
					current = r;
					hasNextValue = true;
					break;
				}
			}
			hasBeenNexted = false;
			return hasNextValue;
		}

		@Override
		public Record<Geometry> next() {
			if (hasBeenNexted) {
				if (!hasNext()) {
					throw new RuntimeException("No more items");
				}
			}
			hasBeenNexted = true;
			return current;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			NiceIterator.close(it);
		}
	}

	public static class MatrixTester extends Operation {
		private IntersectionMatrix[] matrices;

		public MatrixTester(String... values) {
			this.matrices = Helper.create(values);
		}

		public MatrixTester(MatrixTester... values) {
			matrices = Arrays.stream(values)
				.flatMap(mt -> Arrays.stream(mt.getIntersectionMatrices()))
				.collect(Collectors.toList())
				.toArray(new IntersectionMatrix[] {});
		}

		@Override
		protected boolean doRelate(Geometry extent1, Geometry extent2) {
			return Helper.testRelation(extent1, extent2, matrices);
		}

		@Override
		public IntersectionMatrix[] getIntersectionMatrices() {
			return matrices;
		}
	}

	public static class Helper {
		public static final Operation ALWAYS_MATCH = new MatrixTester("*********");

		public static Operation invert(final Operation tester) {
			return new Operation() {
				@Override
				protected boolean doRelate(Geometry extent1, Geometry extent2) {
					return tester.relate(extent2, extent1);
				}

				@Override
				public IntersectionMatrix[] getIntersectionMatrices() {
					IntersectionMatrix[] matrix = tester.getIntersectionMatrices();
					IntersectionMatrix[] transpose = new IntersectionMatrix[matrix.length];
					for (int i = 0; i < matrix.length; i++) {
						transpose[i] = matrix[i].transpose();
					}
					return transpose;
				}
			};
		}

		public static boolean isIntersection(Operation op) {
			for (IntersectionMatrix matrix : op.getIntersectionMatrices()) {
				if (matrix.isIntersects()) {
					return true;
				}
			}
			return false;
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
		public static final Operation EQUALS = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				if (extent1 instanceof Point && extent2 instanceof Point) {
					return extent1.equals(extent2);
				} else {
					return Helper.testRelation(extent1, extent2, getIntersectionMatrices());
				}
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("TFFFTFFFT");
			}
		};

		public static final Operation DISJOINT = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.disjoint(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("FF*FF****");
			}
		};

		public static final Operation INTERSECTS = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.intersects(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T********", "*T*******", "***T*****",
					"****T****");
			}
		};

		public static final Operation TOUCHES = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.touches(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("FT*******", "F**T*****", "F***T****");
			}
		};

		public static final Operation WITHIN = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.within(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T*F**F***");
			}
		};

		public static final Operation CONTAINS = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.contains(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T*****FF*");
			}
		};

		public static final Operation OVERLAPS = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.overlaps(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T*T***T**");
			}
		};

		public static final Operation CROSSES = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				extent1.covers(extent2);
				return extent1.crosses(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T*T******", "T*****T**", "0********");
			}
		};
	}

	public static class Egenhofer {
		public static final Operation EQUALS = SimpleFeatures.EQUALS;
		public static final Operation DISJOINT = SimpleFeatures.DISJOINT;
		public static final Operation MEET = SimpleFeatures.TOUCHES;
		public static final Operation OVERLAP = SimpleFeatures.OVERLAPS;

		public static final Operation COVERS = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.covers(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T*****FF*", "*T****FF*", "***T**FF*", "****T*FF*");
			}
		};

		public static final Operation COVEREDBY = new Operation() {
			@Override
			protected boolean doRelate(Geometry extent1, Geometry extent2) {
				return extent1.coveredBy(extent2);
			}

			@Override
			public IntersectionMatrix[] getIntersectionMatrices() {
				return Helper.create("T*F**F***", "*TF**F***", "**FT*F***", "**F*TF***");
			}
		};

		public static final Operation INSIDE = new MatrixTester("TFF*FFT**");
		public static final Operation CONTAINS = new MatrixTester("T*TFF*FF*");
	}

	public static class RCC8 {
		public static final Operation EQ = SimpleFeatures.EQUALS;
		public static final Operation DC = SimpleFeatures.DISJOINT;
		public static final Operation EC = new MatrixTester("FFTFTTTTT");
		public static final Operation PO = new MatrixTester("TTTTTTTTT");
		public static final Operation TPPI = new MatrixTester("TTTFTTFFT");
		public static final Operation TPP = new MatrixTester("TFFTTFTTT");
		public static final Operation NTPP = new MatrixTester("TFFTFFTTT");//"T*F**F***" //this works, but not really correct?
		public static final Operation NTPPI = new MatrixTester("TTTFFTFFT");
	}

	public static class RCC_EXT {
		public static final Operation CONNECTED = SimpleFeatures.INTERSECTS;
		public static final Operation PART = SimpleFeatures.INTERSECTS;
		public static final Operation INV_PART = SimpleFeatures.INTERSECTS;
		public static final Operation PROPER_PART = new MatrixTester((MatrixTester)RCC8.TPP, (MatrixTester)RCC8.NTPP);
		public static final Operation INV_PROPER_PART = new MatrixTester((MatrixTester)RCC8.TPPI, (MatrixTester)RCC8.NTPPI);
	}
}
