package com.bbn.parliament.jena.graph.index.spatial.geosparql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.GML;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geo;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.WKT;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryBase;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.vocabulary.RDF;
import com.vividsolutions.jts.geom.Geometry;

public class GeoSPARQLOperandFactory extends OperandFactoryBase<Geometry> {
	/** {@inheritDoc} */
	@Override
	public Operand<Geometry> createOperand(Node rootNode, BasicPattern pattern, Binding binding) {
		return createOperand(rootNode, pattern, binding, new ArrayList<Triple>());
	}

	private static boolean containsOGCType(List<String> types) {
		if (types.contains(Geo.Geometry.getURI())) {
			return true;
		}
		for (WKT.Type type : WKT.Type.values()) {
			if (types.contains(type.getURI())) {
				return true;
			}
		}

		for (GML.Type type : GML.Type.values()) {
			if (types.contains(type.getURI())) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsAsWKT(BasicPattern pattern) {
		return containsProperty(pattern, Geo.asWKT.getURI());
	}

	private static boolean containsAsGML(BasicPattern pattern) {
		return containsProperty(pattern, Geo.asGML.getURI());
	}

	private static boolean containsProperty(BasicPattern pattern, String uri) {
		for (Triple t : pattern) {
			if (t.getPredicate().hasURI(uri)) {
				return true;
			}
		}
		return false;
	}
	private Operand<Geometry> createOperand(Node rootNode,
		BasicPattern pattern, Binding binding, List<Triple> usedTriples) {

		Operand<Geometry> op = createOperand(rootNode, binding);
		if (null != op) {
			return op;
		}

		// get triples from pattern that contain rootNode as subject
		BasicPattern triples = getTriplesWithSubject(rootNode, pattern);
		List<String> types = getTypes(triples);
		Geometry extent = null;
		if (containsOGCType(types)) {
			if (containsAsWKT(pattern)) {
				extent = processWKT(rootNode, triples, binding, usedTriples);
			} else if (containsAsGML(pattern)){
				extent = processGML(rootNode, triples, binding, usedTriples);
			}
		}
		// extent can be null
		return new Operand<>(rootNode, extent, usedTriples);
	}

	private static Geometry process(Node rootNode, BasicPattern triples, Binding binding,
		List<Triple> usedTriples, String predicateURI, String literalDataTypeURI) {
		for (Triple t : triples) {
			if (t.getPredicate().hasURI(predicateURI)) {
				Node object = t.getObject();
				// update object from binding first
				if (object.isVariable()) {
					Var v = Var.alloc(object);
					object = binding.get(v);
				}

				if (null != object && object.isLiteral() && literalDataTypeURI.equals(object.getLiteralDatatypeURI())) {
					BasicPattern pattern = getTriplesWithSubject(t.getSubject(), triples);

					for (Triple gt : pattern) {
						if (gt.getPredicate().equals(RDF.Nodes.type)) {
							if (containsOGCType(Arrays.asList(new String[] { gt.getObject().getURI() }))) {
								usedTriples.add(gt);
							}
						}
					}
					usedTriples.add(t);
					return (Geometry)object.getLiteralValue();
				} else if (null == object) {
					BasicPattern pattern = getTriplesWithSubject(t.getSubject(), triples);

					for (Triple gt : pattern) {
						if (gt.getPredicate().equals(RDF.Nodes.type)) {
							if (containsOGCType(Arrays.asList(new String[] { gt.getObject().getURI() }))) {
								usedTriples.add(gt);
							}
						}
					}
					usedTriples.add(t);
					return null;
				}
			}
		}
		return null;
	}

	private static Geometry processWKT(Node rootNode, BasicPattern triples, Binding binding, List<Triple> usedTriples) {
		return process(rootNode, triples, binding, usedTriples, Geo.asWKT.getURI(), WKT.WKTLiteral.getURI());
	}

	private static Geometry processGML(Node rootNode, BasicPattern triples, Binding binding, List<Triple> usedTriples) {
		return process(rootNode, triples, binding, usedTriples, Geo.asGML.getURI(), GML.GMLLiteral.getURI());
	}

	/** {@inheritDoc} */
	@Override
	public Operand<Geometry> createOperand(Node rootNode, Binding binding) {
		if (rootNode.isVariable()) {
			Node n = binding.get(Var.alloc(rootNode));
			if (null != n && n.isURI()) {
				Geometry extent = findRepresentation(n);
				Operand<Geometry> op = new Operand<>(rootNode);
				op.setRepresentation(extent);
				return op;
			}
		} else if (rootNode.isConcrete()) {
			Geometry extent = findRepresentation(rootNode);
			Operand<Geometry> op = new Operand<>(rootNode);
			op.setRepresentation(extent);

			return op;
		}
		return null;
	}
}
