package com.bbn.parliament.jena.graph.index.spatial.standard;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.bbn.parliament.jena.graph.index.spatial.standard.data.BufferedGeometry;
import com.bbn.parliament.jena.graph.index.spatial.standard.data.FloatingCircle;
import com.bbn.parliament.kb_graph.query.index.operand.Operand;
import com.bbn.parliament.kb_graph.query.index.operand.OperandFactoryBase;

/** @author rbattle */
public class SpatialOperandFactory extends OperandFactoryBase<Geometry> {
	public SpatialOperandFactory() {
		super();
	}

	/** {@inheritDoc} */
	@Override
	public Operand<Geometry> createOperand(Node rootNode,
		BasicPattern pattern, Binding binding) {
		return createOperand(rootNode, pattern, binding,
			new ArrayList<Triple>());
	}

	private Operand<Geometry> createOperand(Node rootNode,
		BasicPattern pattern, Binding binding, List<Triple> usedTriples) {

		Operand<Geometry> op = createOperand(rootNode, binding);
		if (null != op.getRepresentation()) {
			return op;
		}

		// get triples from pattern that contain rootNode as subject
		BasicPattern triples = getTriplesWithSubject(rootNode, pattern);
		List<String> types = getTypes(triples);
		Geometry extent = null;
		if (types.contains(StdConstants.GML_POINT)) {
			extent = extractPoint(rootNode, triples, binding, usedTriples);
			addTypeTriple(StdConstants.GML_POINT, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_POINT_H)) {
			extent = extractPoint(rootNode, triples, binding, usedTriples);
			addTypeTriple(StdConstants.GML_POINT, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_LINE_STRING)) {
			extent = extractLineString(rootNode, triples, binding, usedTriples);
			addTypeTriple(StdConstants.GML_LINE_STRING, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_LINE_STRING_H)) {
			extent = extractLineString(rootNode, triples, binding, usedTriples);
			addTypeTriple(StdConstants.GML_LINE_STRING_H, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_LINEAR_RING)) {
			extent = extractLinearRing(rootNode, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.GML_LINEAR_RING, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_LINEAR_RING_H)) {
			extent = extractLinearRing(rootNode, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.GML_LINEAR_RING_H, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_POLYGON)) {
			extent = extractPolygon(rootNode, triples, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.GML_POLYGON, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_POLYGON_H)) {
			extent = extractPolygon(rootNode, triples, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.GML_POLYGON_H, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_CIRCLE)) {
			extent = extractCircle(rootNode, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.GML_CIRCLE, rootNode, usedTriples);
		} else if (types.contains(StdConstants.GML_CIRCLE_H)) {
			extent = extractCircle(rootNode, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.GML_CIRCLE_H, rootNode, usedTriples);
		} else if (types.contains(StdConstants.SPATIAL_BUFFER)) {
			extent = extractBuffer(rootNode, triples, pattern, binding, usedTriples);
			addTypeTriple(StdConstants.SPATIAL_BUFFER, rootNode, usedTriples);
		}

		// extent can be null
		return new Operand<>(rootNode, extent, usedTriples);
	}

	/** {@inheritDoc} */
	@Override
	public Operand<Geometry> createOperand(Node rootNode, Binding binding) {
		if (rootNode.isVariable()) {
			Node n = binding.get(Var.alloc(rootNode));
			Operand<Geometry> op = new Operand<>(rootNode);
			if (null != n) {
				Geometry extent = findRepresentation(n);
				op.setRepresentation(extent);
			}
			// return op even if no representation.  This is checked later.
			return op;
		} else if (rootNode.isConcrete()) {
			Geometry extent = findRepresentation(rootNode);

			//if (null == extent) {
			//	// iterate through binding objects to get ExtentBinding
			//	Binding b = binding;
			//	while (null == extent && b != null) {
			//		if (b instanceof ExtentBinding) {
			//			ExtentBinding<Geometry> eb = new ExtentBinding<Geometry>(b);
			//			extent = eb.getExtent(rootNode);
			//		} else {
			//			b = b.getParent();
			//		}
			//	}
			//}
			Operand<Geometry> op = new Operand<>(rootNode);
			op.setRepresentation(extent);

			return op;
		}
		return null;
	}

	private static Point extractPoint(Node rootNode, BasicPattern rootPattern,
		Binding binding, List<Triple> usedTriples) {

		for (Triple t : rootPattern) {
			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}
			String uri = predicate.getURI();
			if (StdConstants.GML_POS.equals(uri) || StdConstants.GML_POS_H.equals(uri)) {
				Node object = t.getObject();
				if (object.isVariable()) {
					Var v = Var.alloc(object);
					Node n = binding.get(v);
					if (null != n) {
						object = n;
					}
				}
				if (!object.isLiteral()) {
					continue;
				}
				Object value = object.getLiteralValue();
				if (null == value || !String.class.isInstance(value)) {
					throw new RuntimeException("Invalid value for point");
				}
				String pointList = (String) object.getLiteralValue();
				Coordinate point = SpatialGeometryFactory
					.getCoordinateFromPos(pointList);
				Point geoPoint = SpatialGeometryFactory.GEOMETRY_FACTORY
					.createPoint(point);
				usedTriples.add(t);
				return geoPoint;
			}
		}
		return null;
	}

	private static LineString extractLineString(Node rootNode, BasicPattern rootPattern,
		Binding binding, List<Triple> usedTriples) {

		for (Triple t : rootPattern) {
			if (!t.getPredicate().isURI()) {
				continue;
			}
			String uri = t.getPredicate().getURI();
			if (StdConstants.GML_POS_LIST.equals(uri) || StdConstants.GML_POS_LIST_H.equals(uri)) {
				Node object = t.getObject();
				if (object.isVariable()) {
					Var v = Var.alloc(object);
					Node n = binding.get(v);
					if (null != n) {
						object = n;
					}
				}
				if (!object.isLiteral()) {
					continue;
				}
				String pointList = object.getLiteralLexicalForm();
				Coordinate[] coords = SpatialGeometryFactory
					.getCoordinatesFromPosList(pointList);
				LineString geom = SpatialGeometryFactory.GEOMETRY_FACTORY
					.createLineString(coords);
				usedTriples.add(t);
				return geom;
			}
		}
		return null;
	}

	private Polygon extractPolygon(Node rootNode, BasicPattern rootPattern,
		BasicPattern pattern, Binding binding, List<Triple> usedTriples) {
		LinearRing exterior = null;
		List<LinearRing> interior = new ArrayList<>();
		for (Triple t : rootPattern) {
			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}
			String uri = predicate.getURI();
			if (StdConstants.GML_EXTERIOR.equals(uri)
				|| StdConstants.GML_EXTERIOR_H.equals(uri)
				|| StdConstants.GML_INTERIOR.equals(uri)
				|| StdConstants.GML_INTERIOR_H.equals(uri)) {
				boolean isExterior = false;
				if (StdConstants.GML_EXTERIOR.equals(uri) || StdConstants.GML_EXTERIOR_H.endsWith(uri)) {
					isExterior = true;
				}
				Node object = t.getObject();
				if (object.isConcrete()) {
					// load ring from index
					Geometry objExtent = findRepresentation(object);
					if (objExtent instanceof LinearRing ring) {
						if (isExterior) {
							exterior = ring;
						} else {
							interior.add(ring);
						}
						usedTriples.add(t);
					} else {
						throw new RuntimeException(
							"Polygon bound to ring %1$s that is not a linear ring"
							.formatted(object.getURI()));
					}
				} else {
					Operand<Geometry> ringOp = createOperand(object, pattern,
						binding, usedTriples);
					if (null == ringOp || !(ringOp.getRepresentation() instanceof LinearRing)) {
						throw new RuntimeException(
							"Polygons not bound to linear rings are not supported.");
					}
					LinearRing ring = (LinearRing) ringOp.getRepresentation();
					if (isExterior) {
						exterior = ring;
					} else {
						interior.add(ring);
					}
					usedTriples.add(t);
				}
			}
		}
		if (null != exterior) {
			Polygon polygon = SpatialGeometryFactory.GEOMETRY_FACTORY
				.createPolygon(exterior,
					interior.toArray(new LinearRing[] {}));
			return polygon;
		}
		return null;
	}

	private static LinearRing extractLinearRing(Node rootNode, BasicPattern rootPattern,
		Binding binding, List<Triple> usedTriples) {
		for (Triple t : rootPattern) {
			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}

			String uri = predicate.getURI();
			if (StdConstants.GML_POS_LIST.equals(uri) || StdConstants.GML_POS_LIST_H.equals(uri)) {
				Node object = t.getObject();
				Object value = null;
				if (object.isLiteral()) {
					value = object.getLiteralValue();
				} else if (object.isVariable()) {
					Var v = Var.alloc(object);
					Node n = binding.get(v);
					if (null != n && n.isLiteral()) {
						value = n.getLiteralValue();
					} else {
						continue;
					}
				}

				if (null == value || !String.class.isInstance(value)) {
					throw new RuntimeException("Invalid value for linear ring");
				}
				String pointList = (String) value;
				Coordinate[] coords = SpatialGeometryFactory
					.makeRing(SpatialGeometryFactory
						.getCoordinatesFromPosList(pointList));
				LinearRing ring = SpatialGeometryFactory.GEOMETRY_FACTORY
					.createLinearRing(coords);
				usedTriples.add(t);
				return ring;
			}
		}
		return null;
	}

	private static Geometry extractCircle(Node rootNode, BasicPattern rootPattern,
		Binding binding, List<Triple> usedTriples) {
		double radius = 0.0;
		String pos = null;
		boolean hasRadius = false;
		for (Triple t : rootPattern) {
			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}

			String uri = predicate.getURI();
			Node object = t.getObject();
			if (StdConstants.GML_RADIUS.equals(uri) || StdConstants.GML_RADIUS_H.equals(uri)) {
				Object value = null;
				if (object.isLiteral()) {
					value = object.getLiteralValue();
				} else if (object.isVariable()) {
					Var v = Var.alloc(object);
					Node n = binding.get(v);
					if (null != n && n.isLiteral()) {
						value = n.getLiteralValue();
					} else {
						continue;
					}
				}
				if (null == value || !Number.class.isInstance(value)) {
					throw new RuntimeException("Invalid value for circle radius");
				}
				radius = ((Number) value).doubleValue() * 1000;
				hasRadius = true;
				usedTriples.add(t);
			} else if (StdConstants.GML_POS.equals(uri) || StdConstants.GML_POS_H.equals(uri)) {
				Object value = null;
				if (object.isLiteral()) {
					value = object.getLiteralValue();
				} else if (object.isVariable()) {
					Var v = Var.alloc(object);
					Node n = binding.get(v);
					if (null != n && n.isLiteral()) {
						value = n.getLiteralValue();
					} else {
						continue;
					}
				}
				if (null == value || !String.class.isInstance(value)) {
					throw new RuntimeException(
						"Invalid value for circle position");
				}
				pos = (String) value;
				usedTriples.add(t);
			}
		}
		if (!hasRadius) {
			throw new RuntimeException("No radius specified for circle");
		}
		if (pos != null) {
			Coordinate point = SpatialGeometryFactory.getCoordinateFromPos(pos);
			Point p = SpatialGeometryFactory.GEOMETRY_FACTORY
				.createPoint(point);
			BufferedGeometry b = SpatialGeometryFactory
				.createBufferedGeometry(p, radius);
			return b.getBufferedGeometry();
		}
		FloatingCircle c = SpatialGeometryFactory
			.createFloatingCircle(radius);
		return c;
	}

	/** Try to bind a buffer to the operator. */
	private BufferedGeometry extractBuffer(Node rootNode, BasicPattern rootPattern,
		BasicPattern pattern, Binding binding, List<Triple> usedTriples) {

		double distance = 0.0;
		boolean hasDistance = false;
		Geometry extent = null;

		for (Triple t : rootPattern) {

			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}
			String uri = predicate.getURI();
			if (StdConstants.SPATIAL_DISTANCE.equals(uri)) {
				usedTriples.add(t);
				Object value = null;
				Node object = t.getObject();
				if (object.isLiteral()) {
					value = object.getLiteralValue();
				} else if (object.isVariable()) {
					Var v = Var.alloc(object);
					Node n = binding.get(v);
					if (null != n && n.isLiteral()) {
						value = n.getLiteralValue();
					} else {
						continue;
					}
				}

				if (null == value || !Number.class.isInstance(value)) {
					throw new RuntimeException(
						"Invalid value for buffer distance");
				}
				distance = (((Number) value).doubleValue()) * 1000;
				hasDistance = true;
			} else if (StdConstants.SPATIAL_EXTENT.equals(uri)) {
				Node object = t.getObject();
				Operand<Geometry> objectOp = createOperand(object, pattern,
					binding, usedTriples);
				usedTriples.add(t);
				if (null == objectOp) {
					continue;
				}

				extent = objectOp.getRepresentation();
			}
		}
		if (null == extent) {
			return null;
		}
		BufferedGeometry buffer = SpatialGeometryFactory
			.createBufferedGeometry(extent);
		if (hasDistance) {
			buffer.setDistance(distance);
		}
		return buffer;
	}
}
