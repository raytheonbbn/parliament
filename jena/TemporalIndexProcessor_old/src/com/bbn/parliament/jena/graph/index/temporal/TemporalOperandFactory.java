package com.bbn.parliament.jena.graph.index.temporal;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryBase;

/** @author rbattle */
public class TemporalOperandFactory extends OperandFactoryBase<TemporalExtent> {
	private static Logger log = LoggerFactory.getLogger(TemporalOperandFactory.class);

	public TemporalOperandFactory() {
		super();
	}

	private TemporalInterval extractInterval(Node rootNode,
		BasicPattern pattern, Binding binding, List<Triple> usedTriples) {
		TemporalInterval interval = null;
		TemporalInstant start = null;
		TemporalInstant end = null;
		for (Triple t : pattern) {
			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}
			if (Constants.INTERVAL_STARTED_BY.equals(predicate)) {
				usedTriples.add(t);
				Node object = t.getObject();
				Operand<TemporalExtent> time = createOperand(object, pattern,
					binding, usedTriples);
				TemporalInstant instant = (TemporalInstant) time
					.getRepresentation();
				if (null == instant) {
					continue;
				}
				start = instant;
			} else if (Constants.INTERVAL_FINISHED_BY.equals(predicate)) {
				usedTriples.add(t);
				Node object = t.getObject();
				Operand<TemporalExtent> time = createOperand(object, pattern,
					binding, usedTriples);
				TemporalInstant instant = (TemporalInstant) time
					.getRepresentation();
				if (null == instant) {
					continue;
				}
				end = instant;
			}
		}
		if (start != null || end != null) {
			interval = new TemporalInterval(start, end);
		}
		return interval;
	}

	private static TemporalInstant extractInstant(Node rootNode, BasicPattern pattern,
		Binding binding, List<Triple> usedTriples) {
		TemporalInstant extent = null;
		for (Triple t : pattern) {
			Node predicate = t.getPredicate();
			if (!predicate.isURI()) {
				continue;
			}
			if (Constants.DATE_TIME.equals(predicate)) {
				usedTriples.add(t);
				Node object = t.getObject();

				if (object.isVariable()) {
					Var var = Var.alloc(object);
					object = binding.get(var);
				}
				if (null == object || !object.isLiteral()) {
					continue;
				}
				Object o = object.getLiteralValue();
				if (o instanceof XSDDateTime dt) {
					extent = new TemporalInstant(dt);
				} else {
					throw new RuntimeException("Instants must be typed with xsd:dateTime");
				}
			}
		}
		return extent;
	}

	/** {@inheritDoc} */
	@Override
	public Operand<TemporalExtent> createOperand(Node rootNode, Binding binding) {
		if (rootNode.isVariable()) {
			Node n = binding.get(Var.alloc(rootNode));
			if (null != n) {
				log.trace("Found binding for '{}':  '{}'", rootNode, n);
				TemporalExtent extent = findRepresentation(n);
				log.trace("Representation for '{}':  '{}'", rootNode, extent);
				Operand<TemporalExtent> op = new Operand<>(rootNode);
				op.setRepresentation(extent);
				return op;
			}
		} else if (rootNode.isConcrete()) {
			TemporalExtent extent = findRepresentation(rootNode);

			Operand<TemporalExtent> op = new Operand<>(rootNode);
			op.setRepresentation(extent);

			return op;
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Operand<TemporalExtent> createOperand(Node rootNode,
		BasicPattern pattern, Binding binding) {
		return createOperand(rootNode, pattern, binding, new ArrayList<Triple>());
	}

	private Operand<TemporalExtent> createOperand(Node rootNode,
		BasicPattern pattern, Binding binding, List<Triple> usedTriples) {

		Operand<TemporalExtent> op = createOperand(rootNode, binding);
		if (null != op) {
			return op;
		}

		// get triples from pattern that contain rootNode as subject
		BasicPattern triples = getTriplesWithSubject(rootNode, pattern);
		List<String> types = getTypes(triples);
		TemporalExtent extent = null;
		if (types.contains(Constants.DATE_TIME_INTERVAL.getURI())) {
			extent = extractInstant(rootNode, triples, binding, usedTriples);
			addTypeTriple(Constants.DATE_TIME_INTERVAL.getURI(), rootNode, usedTriples);
		} else if (types.contains(Constants.PROPER_INTERVAL.getURI())) {
			extent = extractInterval(rootNode, triples, binding, usedTriples);
			addTypeTriple(Constants.PROPER_INTERVAL.getURI(), rootNode, usedTriples);
		}

		// extent can be null
		return new Operand<>(rootNode, extent, usedTriples);
	}
}
