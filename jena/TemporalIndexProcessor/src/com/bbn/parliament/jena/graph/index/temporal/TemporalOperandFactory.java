/**
 *
 */
package com.bbn.parliament.jena.graph.index.temporal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryBase;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/** @author rbattle */
public class TemporalOperandFactory extends OperandFactoryBase<TemporalExtent> {
	private static Logger log = LoggerFactory.getLogger(TemporalOperandFactory.class);

	public TemporalOperandFactory() {
		super();
	}

	/**
	 * @return The temporal entity extracted from the query expression.
	 * Reverses the list of triples in order to sort them by newest to oldest.
	 */
	private static TemporalExtent extractExtent(List<Triple> pattern, Binding binding,
			List<Triple> usedTriples, TemporalIndexField p) {
		TemporalExtent extent = null;
		Collections.reverse(pattern);
		for (Triple t : pattern) {
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
			if (o instanceof TemporalExtent)	{
				return (TemporalExtent) o;
			}
			else if (o instanceof XSDDateTime)	{
				return new TemporalInstant((XSDDateTime) o);
			}
		}
		return extent;
	}

	/** {@inheritDoc} */
	@Override
	public Operand<TemporalExtent> createOperand(Node rootNode, Binding binding) {
		if (rootNode.isVariable()) {
			Node n = binding.get(Var.alloc(rootNode));
			Operand<TemporalExtent> op = new Operand<>(rootNode);
			if (null != n) {
				log.trace("Found binding for '{}':  '{}'", rootNode, n);
				TemporalExtent extent = findRepresentation(n);
				log.trace("Representation for '{}':  '{}'", rootNode, extent);
				op.setRepresentation(extent);
			}
			// return op even if no representation.  This is checked later.
			return op;
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

	/**
	 * Converts {@link Triple}s recognized by the index into {@link Operand}s representing temporal entities.
	 *
	 * @return An {@link Operand} consisting of the URI/variable name, the {@link TemporalExtent}
	 * it represents, and the triple(s) from which that representation is derived.
	 */
	private Operand<TemporalExtent> createOperand(Node rootNode,
			BasicPattern pattern, Binding binding, List<Triple> usedTriples) {

		Operand<TemporalExtent> op = createOperand(rootNode, binding);
		if (null != op.getRepresentation()) {
			return op;
		}

		// get triples from pattern that contain rootNode as subject
		BasicPattern triples = getTriplesWithSubject(rootNode, pattern);

		TemporalExtent extent = null;
		//Iterate through all known types of temporal entities for a matching type association
		for (TemporalIndexField p : TemporalIndexField.values())	{
			List<Triple> objects = getTriplesWithPredicate(triples, p.getPredicate());
			if ((extent = extractExtent(objects, binding, usedTriples, p)) != null)	{
				//When an operand is found, add all statements (including inferred statements) from the
				//query used in the operand's representation
				addTypeTriple(Constants.TEMPORAL_ENTITY.getURI(), rootNode, usedTriples);
				addTypeTriple(p.getType().getURI(), rootNode, usedTriples);
				return new Operand<>(rootNode, extent, usedTriples);
			}
		}

		// extent is null
		return new Operand<>(rootNode, null, usedTriples);
	}
}
