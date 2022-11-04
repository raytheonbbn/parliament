package com.bbn.parliament.jena.graph.index.temporal.extent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.bbn.parliament.jena.graph.index.temporal.TemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;

/** @author rbattle */
public class TemporalRecordFactory implements RecordFactory<TemporalExtent> {
	private static final Logger LOG = LoggerFactory.getLogger(TemporalRecordFactory.class);
	private static final List<Triple> MATCHES;

	static {
		List<Triple> matches = new ArrayList<>(TemporalIndexField.values().length);
		for (TemporalIndexField p : TemporalIndexField.values()) {
			Node predicate = p.getPredicate();
			matches.add(Triple.create(Node.ANY, predicate, Node.ANY));
			//Register Parliament Time's literals
			TypeMapper.getInstance().registerDatatype(p.getDatatype());
		}
		MATCHES = Collections.unmodifiableList(matches);
	}

	public TemporalRecordFactory(Graph graph, TemporalIndex index) {
	}

	/** {@inheritDoc} */
	@Override
	public Record<TemporalExtent> createRecord(Triple t) {
		LOG.trace("Creating index record for triple '{}'", t);
		Node subject = t.getSubject();
		TemporalExtent value = null;
		Node key = subject;
		for (TemporalIndexField p : TemporalIndexField.values())	{
			if (t.getPredicate().equals(p.getPredicate()))	{
				LOG.trace("'{}' recognized by index via predicate '{}'", t, t.getPredicate());
				value = createTypedExtent(t);
			}
		}
		if (LOG.isTraceEnabled()) {
			if (null == value) {
				LOG.trace("'{}' not recognized as temporal", t);
				return null;
			} else {
				LOG.trace("Created index record for triple '{}'", t);
				return Record.create(key, value);
			}
		} else {
			return (null == value) ? null : Record.create(key, value);
		}
	}

	/**
	 * Creates a TemporalExtent type representation from a triple.
	 * @author mhale
	 */
	private static TemporalExtent createTypedExtent(Triple t)	{
		if (t.getObject().isLiteral()) {
			Node dateTime = t.getObject();
			Object o = dateTime.getLiteralValue();
			if (o instanceof TemporalExtent tempExt)	{
				return tempExt;
			} else if (o instanceof XSDDateTime dt)	{
				return new TemporalInstant(dt);	// Wrap XSDDateTime in a TemporalInstant
			}
		}
		LOG.debug("No objects of valid literals could be found triple '{}'", t);
		return null;
	}

	@Override
	public List<Triple> getTripleMatchers() {
		return MATCHES;
	}

	@SuppressWarnings("unused")
	private static boolean isMatch(Triple triple) {
		boolean result = false;
		for (Triple t : MATCHES) {
			if (t.matches(triple)) {
				result = true;
				break;
			}
		}
		return result;
	}
}