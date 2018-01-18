package com.bbn.parliament.jena.graph.index.spatial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.bbn.parliament.jena.graph.index.spatial.standard.data.BufferedGeometry;
import com.bbn.parliament.jena.graph.index.spatial.standard.data.FloatingCircle;
import com.bbn.parliament.jena.query.index.QueryCache;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactory;
import com.bbn.parliament.jena.query.index.pfunction.EstimableIndexPropertyFunction;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterCommonParent;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.util.IterLib;
import com.hp.hpl.jena.util.iterator.NiceIterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SpatialPropertyFunction extends EstimableIndexPropertyFunction<Geometry> {
	private static final Logger LOG = LoggerFactory.getLogger(SpatialPropertyFunction.class);

	protected Operation opToExecute;
	private String uri;

	/** Constructs a new property function for the specified index type and operation. */
	public SpatialPropertyFunction(String uri, Operation op,
		Class<? extends OperandFactory<Geometry>> operandFactoryClass) {
		super(SpatialIndex.class, operandFactoryClass);
		opToExecute = op;
		this.uri = uri;
	}

	/** {@inheritDoc} */
	@Override
	protected SpatialIndex getIndex() {
		return (SpatialIndex) index;
	}

	/** Get the URI of this instance. */
	public String getUri() {
		return uri;
	}

	/**
	 * Perform the binary operation.
	 *
	 * @param extent1 the subject of the operation (can be <code>null</code>).
	 * @param extent2 the object of the operation (can be <code>null</code>).
	 * @param rootNode1 the root node of extent1.
	 * @param rootNode2 the root node of extent2.
	 * @param context the execution context.
	 * @return an iterator of bindings that represent the outcome of the operation.
	 * @throws SpatialIndexException if both extents are null.
	 */
	public QueryIterator performOperation(Geometry extent1, Geometry extent2,
		final Node rootNode1, final Node rootNode2, ExecutionContext context)
			throws SpatialIndexException {
		if (extent1 != null) {
			LOG.debug("{} extent1 is not null", getUri());
			return bindVar(extent1, rootNode2, getIndex(), false, context);
		} else if (extent2 != null) {
			LOG.debug("{} extent2 is not null", getUri());
			return bindVar(extent2, rootNode1, getIndex(), true, context);
		} else {
			if (rootNode1.isVariable() && rootNode2.isVariable()) {
				// both extents are null. Iterate over index to bind node1 and
				// perform operation to bind node2
				final Var rootVar1 = Var.alloc(rootNode1);
				Iterator<Record<Geometry>> records = index.iterator();
				QueryIterator wrapper = new SingleGeometryIterator(
					rootNode1,
					records,
					index.getQueryCache(),
					context);
				QueryIterator ret = new QueryIterRepeatApply(wrapper, context) {

					@Override
					protected QueryIterator nextStage(Binding binding) {
						Node n = binding.get(rootVar1);
						Geometry g = index.getQueryCache().get(n);
						ExecutionContext c = getExecContext();
						QueryIterator input = bindVar(g, rootNode2, getIndex(),
							false, c);
						return new QueryIterCommonParent(input, binding, c);
					}
				};
				return ret;
			}

			// at least one of the root nodes is a URI that is not indexed so no
			// operation can occur
			LOG.debug("{} extent1: {} extent2: {}, URI not found in index so the operation cannot be performed.",
				new Object[] { getUri(), rootNode1, rootNode2 });
			return IterLib.noResults(context);
		}
	}

	protected QueryIterator bindExtentsForFloatingExtents(
		List<Geometry> extents, Node rootNode, FloatingCircle floater,
		boolean floatingSubject, SpatialIndex spIndex, Binding binding,
		ExecutionContext context) {
		if (floater.getRadius() <= 0.0) {
			LOG.info("no results for 0 radius");
			return IterLib.noResults(context);
		}

		if (extents.size() == 0) {
			// no extents to check against so return index
			return new SingleGeometryIterator(rootNode, spIndex.iterator(),
				spIndex.getQueryCache(), context);
		}
		Iterator<Record<Geometry>> baseIterator = null;
		Geometry floatingRegion = null;
		if (extents.size() > 1) {
			// compute a region around the extents
			floatingRegion = floater.computeFloatingRegion(extents);
			if (floatingRegion == null) {
				return IterLib.noResults(context);
			}
		} else {
			floatingRegion = extents.get(0);
		}

		Point center = floatingRegion.getCentroid();

		BufferedGeometry bg = new BufferedGeometry(SpatialGeometryFactory.GEOMETRY_FACTORY, center, floater.getRadius()*2);
		baseIterator = spIndex
			.iterator(bg.getBufferedGeometry(), Operation.SimpleFeatures.INTERSECTS);
		return new FloatingGeometryIterator(baseIterator, binding, rootNode,
			floater, extents, floatingSubject,
			spIndex.getQueryCache(), context);
	}

	public boolean testFloatingExtent(Geometry boundExtent,
		List<Geometry> extents, FloatingCircle floater,
		boolean floaterFirstArgument) {
		if (null == extents) {
			return true;
		}
		List<Geometry> floatingExtents = new ArrayList<>();
		if (extents.size() > 0) {
			floatingExtents.addAll(extents);
		}
		if (boundExtent != null && !floatingExtents.contains(boundExtent)) {
			floatingExtents.add(boundExtent);
		}

		Geometry floatingRegion = floater.computeFloatingRegion(floatingExtents);
		if (floatingRegion == null) {
			LOG.debug("No floating region");
			return false;
		}

		for (Geometry extent : extents) {
			Geometry arg1;
			Geometry arg2;
			if (floaterFirstArgument) {
				arg1 = floatingRegion;
				arg2 = extent;
			} else {
				arg1 = extent;
				arg2 = floatingRegion;
			}

			if (!opToExecute.relate(arg1, arg2)) {
				return false;
			}

		}

		return true;
	}

	protected QueryIterator bindVar(Geometry boundExtent, Node unboundVariable,
		SpatialIndex spIndex, boolean isFirstVarUnbound, ExecutionContext context) {

		Operation et = opToExecute;
		if (!isFirstVarUnbound) {
			et = Operation.Helper.invert(et);
		}
		Iterator<Record<Geometry>> it = spIndex.iterator(boundExtent, et);

		return new SingleGeometryIterator(unboundVariable, it,
			spIndex.getQueryCache(), context);
	}

	/** {@inheritDoc} */
	@Override
	public long estimate(List<Node> subjects, List<Node> objects,
		Map<Node, Operand<Geometry>> operands) {
		if (subjects.size() == 1 && objects.size() > 1) {
			return estimate(subjects.get(0), objects, operands, true);
		} else if (subjects.size() > 1 && objects.size() == 1) {
			return estimate(objects.get(0), subjects, operands, false);
		} else if (subjects.size() == 1 && objects.size() == 1) {
			return estimate(subjects.get(0), objects.get(0), operands);
		} else {
			throw new SpatialIndexException(getIndex(),
				"Cannot have multiple subjects and multiple arguments for: "
					+ getUri());
		}
	}

	protected long estimateSelectivity(Geometry geometry, boolean isSubject) {
		return getIndex().estimate(geometry, opToExecute);
	}

	protected long estimate(Node node, List<Node> args,
		Map<Node, Operand<Geometry>> operands, boolean isSubject) {
		long l = Long.MAX_VALUE;
		for (Node n : args) {
			long tmp;
			if (isSubject) {
				tmp = estimate(node, n, operands);
			} else {
				tmp = estimate(n, node, operands);
			}
			if (tmp < l) {
				l = tmp;
			}
		}
		return l;
	}

	protected long estimate(Node subject, Node object,
		Map<Node, Operand<Geometry>> operands) {
		Operand<Geometry> op1 = operands.get(subject);
		Operand<Geometry> op2 = operands.get(object);

		Geometry op1obj = op1.getRepresentation();
		Geometry op2obj = op2.getRepresentation();

		// handle case where a URI is specified but there is nothing in the index
		// that matches it
		if (subject.isURI() && null == op1obj) {
			return 0;
		}
		if (object.isURI() && null == op2obj) {
			return 0;
		}
		return estimateSelectivity(op1obj, op2obj);
	}

	private long estimateSelectivity(Geometry op1obj, Geometry op2obj) {
		if (op1obj instanceof FloatingCircle || op2obj instanceof FloatingCircle) {
			// there is no easy way to estimate the count for a floating
			// geometry without actually
			// executing the query
			try {
				return index.size();
			} catch (IndexException e) {

				return Long.MAX_VALUE;
			}
		} else if (op1obj instanceof BufferedGeometry
			|| op2obj instanceof BufferedGeometry) {
			// estimate for buffered geometries depends on whether the distance
			// is bound or not
			if (op1obj instanceof BufferedGeometry) {
				BufferedGeometry buffer = (BufferedGeometry) op1obj;
				if (buffer.getDistance() == null) {
					return Long.MAX_VALUE;
				} else if (buffer.getDistance() <= 0.0d) {
					return 0;
				} else {
					Geometry extent = buffer.getBufferedGeometry();
					return estimateSelectivity(extent, op2obj);
				}
			}
			if (op2obj instanceof BufferedGeometry) {
				BufferedGeometry buffer = (BufferedGeometry) op2obj;
				if (buffer.getDistance() == null) {
					return Long.MAX_VALUE;
				} else if (buffer.getDistance() <= 0.0d) {
					return 0;
				} else {
					Geometry extent = buffer.getBufferedGeometry();
					return estimateSelectivity(op1obj, extent);
				}
			}
		} else {
			if (null != op1obj && null != op2obj) {
				// No need to check. This predicate will return at most one
				// result (op1obj).
				return 1;
			} else if (null != op1obj) {
				return estimateSelectivity(op1obj, true);
			} else if (null != op2obj) {
				return estimateSelectivity(op2obj, false);
			}
		}
		return Long.MAX_VALUE;
	}

	/** {@inheritDoc} */
	@Override
	public QueryIterator execBinding(Binding binding, List<Node> subjects,
		List<Node> objects, Map<Node, Operand<Geometry>> operands,
		ExecutionContext context) {
		if (subjects.size() == 1 && objects.size() > 1) {
			return exec(binding, subjects.get(0), objects, operands, true, context);
		} else if (subjects.size() > 1 && objects.size() == 1) {
			return exec(binding, objects.get(0), subjects, operands, false,
				context);
		} else if (subjects.size() == 1 && objects.size() == 1) {
			return exec(binding, subjects.get(0), objects.get(0), operands,
				context);
		} else {
			throw new RuntimeException(
				"Cannot have multiple subjects and multiple arguments for: "
					+ getUri());
		}
	}

	protected QueryIterator exec(Binding binding, Node node, List<Node> args,
		Map<Node, Operand<Geometry>> operands, boolean isSubject,
		ExecutionContext context) {

		boolean allBound = true;
		for (Node subject : args) {
			Operand<Geometry> op = operands.get(subject);
			Geometry rep = null;
			if (null != op) {
				rep = op.getRepresentation();
			}
			if (rep instanceof FloatingCircle) {
				throw new RuntimeException(
					"Cannot have an instance of a floating circle in the list of subjects for: "
						+ getUri());
			}
			allBound = allBound && (null != rep);
		}

		Operand<Geometry> op = operands.get(node);
		Geometry extent = (null == op) ? null : op.getRepresentation();
		BindingMap b = BindingFactory.create(binding);
		if (null != extent) {
			updateBinding(b, node, extent);
		}
		updateBinding(b, operands);
		if (extent instanceof FloatingCircle) {
			return processFloatingCircle(node, args, b, operands, isSubject,
				context);
		} else if (null != extent && allBound) {
			return processGeometries(Arrays.asList(node), args, b, operands,
				isSubject, context);
		} else {
			return IterLib.noResults(context);
		}

	}

	protected void updateBinding(BindingMap binding, Node node, Geometry extent) {
		if (null == extent) {
			return;
		}
		if (node.isVariable()) {
			Var v = Var.alloc(node);
			if (binding.contains(v)) {
				return;
			}
			Node blank = index.getQueryCache().getBlankNodeMap().get(v);
			if (null == blank) {
				blank = Node.createAnon();
				index.getQueryCache().getBlankNodeMap().put(v, blank);
			}
			binding.add(v, blank);
			index.getQueryCache().put(blank, extent);
		}
	}

	protected void updateBinding(BindingMap binding,
		Map<Node, Operand<Geometry>> operands) {
		for (Map.Entry<Node, Operand<Geometry>> op : operands.entrySet()) {
			updateBinding(binding, op.getKey(), op.getValue().getRepresentation());
		}
	}

	protected QueryIterator exec(Binding binding, Node subject, Node object,
		Map<Node, Operand<Geometry>> operands, ExecutionContext context) {
		Operand<Geometry> op1 = operands.get(subject);
		Operand<Geometry> op2 = operands.get(object);

		// check null operands
		if (null == op1 || null == op2) {
			LOG.debug("Can not find a representation for {}",
				(null == op1 ? subject : object));
			return IterLib.noResults(context);
		}

		// check for valid operands
		boolean op1Invalid = (null == op1.getRepresentation()
			&& (op1.getTriples().size() > 0
				|| subject.isURI()));
		boolean op2Invalid = (null == op2.getRepresentation()
			&& (op2.getTriples().size() > 0
				|| object.isURI()));
		if (op1Invalid || op2Invalid) {
			LOG.debug("Can not find a representation for {}",
				(op1Invalid ? subject : object));
			return IterLib.noResults(context);
		}

		Geometry extent1 = op1.getRepresentation();
		Geometry extent2 = op2.getRepresentation();
		if (extent1 instanceof FloatingCircle
			&& extent2 instanceof FloatingCircle) {
			throw new RuntimeException(
				"Cannot have a floating circle as subject and object for: "
					+ getUri());
		}

		BindingMap b = BindingFactory.create(binding);
		if (extent1 instanceof BufferedGeometry) {
			extent1 = ((BufferedGeometry) extent1).getBufferedGeometry();
			if (null == extent1) {
				// no buffer
				return IterLib.noResults(context);
			}
		}
		if (extent2 instanceof BufferedGeometry) {
			extent2 = ((BufferedGeometry) extent2).getBufferedGeometry();
			if (null == extent2) {
				// no buffer
				return IterLib.noResults(context);
			}
		}
		updateBinding(b, op1.getRootNode(), extent1);
		updateBinding(b, op2.getRootNode(), extent2);
		updateBinding(b, operands);
		if (extent1 instanceof FloatingCircle) {
			return processFloatingCircle(subject, Arrays.asList(object), b,
				operands, true, context);
		} else if (extent2 instanceof FloatingCircle) {
			return processFloatingCircle(object, Arrays.asList(subject), b,
				operands, false, context);
		} else if (null != extent1 && null != extent2) {
			return processGeometries(Arrays.asList(subject),
				Arrays.asList(object), b, operands, true,
				context);
		}

		// either extent1, extent2, or both are unbound
		QueryIterator input = performOperation(extent1, extent2, subject, object,
			context);
		return new QueryIterCommonParent(input, b, context);

	}

	protected QueryIterator processGeometries(List<Node> subjects,
		List<Node> objects, Binding binding,
		Map<Node, Operand<Geometry>> operands, boolean isSubject,
		ExecutionContext context) {
		boolean valid = true;
		List<Node> first;
		List<Node> second;
		if (isSubject) {
			first = subjects;
			second = objects;
		} else {
			first = objects;
			second = subjects;
		}
		for (Node n1 : first) {
			Geometry extent1 = operands.get(n1).getRepresentation();
			if (extent1 instanceof BufferedGeometry) {
				extent1 = ((BufferedGeometry) extent1).getBufferedGeometry();
			}
			for (Node n2 : second) {
				Geometry extent2 = operands.get(n2).getRepresentation();
				if (extent2 instanceof BufferedGeometry) {
					extent2 = ((BufferedGeometry) extent2).getBufferedGeometry();
				}
				valid = valid && opToExecute.relate(extent1, extent2);
			}
		}
		if (valid) {
			Binding newResult = BindingFactory.create(binding);
			List<Node> allNodes = new ArrayList<>(subjects.size() + objects.size());
			allNodes.addAll(subjects);
			allNodes.addAll(objects);
			for (Node s : allNodes) {
				Operand<Geometry> op = operands.get(s);
				if (s.isVariable()) {
					Var var = Var.alloc(s);
					if (newResult.contains(var)) {
						Node n = newResult.get(var);
						index.getQueryCache().put(n, op.getRepresentation());
					}
				}
			}
			return IterLib.result(newResult, context);
		}
		return IterLib.noResults(context);
	}

	protected QueryIterator processFloatingCircle(Node node,
		List<Node> argNodes, Binding binding,
		Map<Node, Operand<Geometry>> operands, boolean floatingSubject,
		ExecutionContext context) {

		BindingMap results = BindingFactory.create(binding);

		Operand<Geometry> op = operands.get(node);
		Geometry extent = op.getRepresentation();
		FloatingCircle floater = (FloatingCircle) extent;
		if (node.isVariable()) {
			Var v = Var.alloc(node);
			Node b = results.get(v);
			if (null == b) {
				b = index.getQueryCache().getBlankNodeMap().get(v);
				if (null == b) {
					b = Node.createAnon();
					index.getQueryCache().getBlankNodeMap().put(v, b);
				}
				results.add(v, b);
				index.getQueryCache().put(b, floater);
			} else {
				floater = (FloatingCircle) index.getQueryCache().get(b);
			}
		}

		List<Operand<Geometry>> ops = new ArrayList<>();
		final List<Geometry> extents = new ArrayList<>();
		final List<Node> unbound = new ArrayList<>();
		for (Node n : argNodes) {
			Operand<Geometry> o = operands.get(n);
			Geometry e = o.getRepresentation();
			if (null == e) {
				unbound.add(n);
			} else {
				addExtent(extents, e);
				ops.add(o);
			}
		}
		QueryIterator result = null;

		if (unbound.size() > 0) {
			QueryIterator input = IterLib.result(results, context);
			boolean first = true;
			for (Node n : unbound) {
				if (first) {
					result = new ExtentRepeatApplyIterator(getIndex(), n, floater,
						extents, unbound,
						floatingSubject, input,
						context);
					first = false;
				} else {
					result = new ExtentRepeatApplyIterator(getIndex(), n, floater,
						extents, unbound,
						floatingSubject, result,
						context);
				}
			}
			return new QueryIterCommonParent(result, results, context);
		}

		boolean valid = true;
		for (Operand<Geometry> o : ops) {
			Node n = o.getRootNode();
			Geometry e = o.getRepresentation();
			valid = valid
				&& testFloatingExtent(e, extents, floater, floatingSubject);
			if (valid) {
				if (n.isVariable()) {
					Var var = Var.alloc(n);
					if (!results.contains(var)) {
						results.add(var, n);
						index.getQueryCache().put(n, e);
					}
				}
			}
		}
		QueryIterator ret = null;
		if (valid) {
			ret = IterLib.result(results, context);
		} else {
			ret = IterLib.noResults(context);
		}
		return ret;
	}

	protected class FloatingGeometryIterator extends QueryIter {
		private Iterator<Record<Geometry>> underlyingIterator;
		private Node nodeToBind;
		private BindingMap nextElement;
		private FloatingCircle myFloater;
		private List<Geometry> myExtents;
		private boolean hasBeenHasNexted = true;
		private boolean hasNextResult = false;
		private Binding binding;
		private boolean floatingSubject;
		private QueryCache<Geometry> queryCache;

		public FloatingGeometryIterator(Iterator<Record<Geometry>> underlyingIterator,
			Binding binding, Node nodeToBind,
			FloatingCircle floater,
			List<Geometry> extents,
			boolean floatingSubject,
			QueryCache<Geometry> queryCache,
			ExecutionContext context) {
			super(context);
			this.underlyingIterator = underlyingIterator;
			this.binding = binding;
			this.nodeToBind = nodeToBind;
			this.myFloater = floater;
			this.myExtents = extents;
			this.floatingSubject = floatingSubject;
			this.queryCache = queryCache;
		}

		@Override
		public boolean hasNextBinding() {
			if (!hasBeenHasNexted) {
				return hasNextResult;
			}

			boolean foundNext = false;
			while (!foundNext && underlyingIterator.hasNext()) {
				Record<Geometry> next = underlyingIterator.next();
				if (testFloatingExtent(next.getValue(), myExtents, myFloater,
					floatingSubject)) {
					nextElement = BindingFactory.create(binding);
					nextElement.add(Var.alloc(nodeToBind), next.getKey());
					queryCache.put(next.getKey(), next.getValue());

					foundNext = true;
				}
			}
			hasBeenHasNexted = false;
			hasNextResult = foundNext;
			return hasNextResult;

		}

		@Override
		public Binding moveToNextBinding() {
			if (null == nextElement) {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
			}
			hasBeenHasNexted = true;
			Binding b = nextElement;
			nextElement = null;
			return b;
		}

		@Override
		protected void closeIterator() {
			NiceIterator.close(underlyingIterator);
		}

		@Override
		protected void requestCancel() {
			// TODO can we cancel?
		}
	}

	protected static class SingleGeometryIterator extends QueryIter {
		private Iterator<Record<Geometry>> underlyingIterator;
		private Var variableNode;
		private QueryCache<Geometry> queryCache;

		public SingleGeometryIterator(Node variableNode,
			Iterator<Record<Geometry>> underlyingIterator,
			QueryCache<Geometry> queryCache,
			ExecutionContext context) {
			super(context);
			this.underlyingIterator = underlyingIterator;
			this.variableNode = Var.alloc(variableNode);
			this.queryCache = queryCache;
		}

		@Override
		public boolean hasNextBinding() {
			return underlyingIterator.hasNext();
		}

		@Override
		public Binding moveToNextBinding() {
			BindingMap result = BindingFactory.create();
			Record<Geometry> next = underlyingIterator.next();
			result.add(variableNode, next.getKey());
			queryCache.put(next.getKey(), next.getValue());
			return result;
		}

		@Override
		protected void closeIterator() {
			NiceIterator.close(underlyingIterator);
		}

		@Override
		protected void requestCancel() {
			// TODO can we cancel?
		}
	}

	private class ExtentRepeatApplyIterator extends QueryIterRepeatApply {

		private List<Geometry> extents;
		private List<Node> unbound;
		private Node rootNode;
		private FloatingCircle circle;
		private boolean floatingSubject;

		public ExtentRepeatApplyIterator(SpatialIndex spIndex, Node rootNode,
			FloatingCircle circle,
			List<Geometry> extents,
			List<Node> unbound,
			boolean floatingSubject,
			QueryIterator input,
			ExecutionContext context) {
			super(input, context);
			this.rootNode = rootNode;
			this.circle = circle;
			this.extents = extents;
			this.unbound = unbound;
			this.floatingSubject = floatingSubject;
		}

		@Override
		protected QueryIterator nextStage(Binding binding) {
			List<Geometry> exts = new ArrayList<>(extents);
			for (Node unb : unbound) {
				Var var = Var.alloc(unb);
				Node n = binding.get(var);
				if (null != n) {
					Geometry ext = index.getQueryCache().get(n);
					if (null != ext) {
						addExtent(exts, ext);
					}
				}
			}
			return bindExtentsForFloatingExtents(exts, rootNode, circle,
				floatingSubject, getIndex(),
				binding, getExecContext());
		}

	}

	private static void addExtent(List<Geometry> extents, Geometry extentToAdd) {
		// check extent hasn't already been added
		boolean contains = false;
		for (Geometry extent : extents) {
			if (extentToAdd instanceof Point && extent instanceof Point) {
				contains = ((Point) extentToAdd).equals(extent);
			} else if ((extentToAdd instanceof Polygon && extent instanceof Polygon)
				|| (extentToAdd instanceof LineString && extent instanceof LineString)) {
				Polygon e1 = (Polygon) extentToAdd;
				Polygon e2 = (Polygon) extent;
				if (e1.getCoordinates().length == e2.getCoordinates().length) {
					Coordinate[] p1s = e1.getCoordinates();
					Coordinate[] p2s = e2.getCoordinates();
					for (int j = 0; j < p1s.length; j++) {
						contains = p1s[j].equals(p2s[j]);
						// break at first unmatched point
						if (!contains) {
							break;
						}
					}
				}
			}
			if (contains) {
				break;
			}
		}
		if (!contains) {
			extents.add(extentToAdd);
		}
	}

	@Override
	public String toString() {
		return "SpatialPropertyFunction [uri=" + uri + "]";
	}
}
