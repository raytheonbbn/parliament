/**
 *
 */
package com.bbn.parliament.jena.graph.index.temporal.extent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.bbn.parliament.jena.graph.index.temporal.Constants;
import com.bbn.parliament.jena.graph.index.temporal.TemporalIndex;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.NiceIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/** @author rbattle */
public class TemporalRecordFactory implements RecordFactory<TemporalExtent> {
	private static final Logger LOG = LoggerFactory.getLogger(TemporalRecordFactory.class);
	private static final List<Triple> MATCHES;

	static {
		List<Triple> matches = new ArrayList<>(Constants.VALID_TYPES.length);
		for (Node type : Constants.VALID_TYPES) {
			matches.add(Triple.create(Node.ANY, RDF.Nodes.type, type));
		}
		MATCHES = Collections.unmodifiableList(matches);
	}

	private TemporalIndex index;
	private Graph graph;
	private Map<Node, String> nodesToProcess;
	private Map<Node, List<Node>> nodesToWaitFor;

	public TemporalRecordFactory(Graph graph, TemporalIndex index) {
		this.graph = graph;
		this.index = index;
		nodesToProcess = new HashMap<>();
		nodesToWaitFor = new HashMap<>();
	}

	/** {@inheritDoc} */
	@Override
	public Record<TemporalExtent> createRecord(Triple t) {
		LOG.trace("Creating index record for triple '{}'", t);
		Node subject = t.getSubject();
		Node predicate = t.getPredicate();
		Node object = t.getObject();
		TemporalExtent value = null;
		Node key = subject;
		if (object.isURI() && RDF.type.asNode().equals(predicate)) {
			if (!isValidType(object)) {
				LOG.trace("Is a type triple, but not a valid type");
				return null;
			}
			LOG.trace("Is a type triple with a valid type");
			value = createExtent(subject, object);
			if (null == value) {
				nodesToProcess.put(subject, object.getURI());
			} else {
				nodesToProcess.remove(subject);
				List<Node> nodes = waitingForNode(subject);
				if (nodes.size() > 0) {
					for (Node n : nodes) {
						processNode(n);
					}
					nodesToWaitFor.remove(subject);
				}
			}
		} else {
			LOG.trace("Is not a type triple");
			if (nodesToProcess.containsKey(subject)) {
				LOG.trace("nodesToProcess contains subject");
				value = processNode(subject);
				key = subject;
			}
			if (nodesToProcess.containsKey(object)) {
				LOG.trace("nodesToProcess contains object");
				value = processNode(object);
				key = object;
			}
		}
		if (LOG.isTraceEnabled()) {
			if (null == value) {
				return null;
			} else {
				LOG.trace("Created index record for triple '{}'", t);
				return Record.create(key, value);
			}
		} else {
			return (null == value) ? null : Record.create(key, value);
		}
	}

	private TemporalExtent processNode(Node node) {
		TemporalExtent ret = null;
		Iterator<Triple> triples = graph.find(node, RDF.type.asNode(), Node.ANY);
		try {
			while (triples.hasNext()) {
				Triple triple = triples.next();
				Node obj = triple.getObject();
				if (!obj.isURI()) {
					continue;
				}
				TemporalExtent extent = createExtent(node, obj);
				if (null != extent) {
					nodesToProcess.remove(node);
					List<Node> nodes = waitingForNode(node);
					if (nodes.size() > 0) {
						for (Node n : nodes) {
							extent = processNode(n);
						}
						nodesToWaitFor.remove(node);
					}
					ret = extent;
				}
			}
		} finally {
			NiceIterator.close(triples);
		}
		return ret;
	}

	private TemporalExtent createExtent(Node subject, Node object) {
		TemporalExtent extent = null;
		if (Constants.PROPER_INTERVAL.equals(object)) {
			extent = createProperInterval(subject);
		} else if (Constants.DATE_TIME_INTERVAL.equals(object)) {
			extent = createDateTimeInterval(subject);
		}
		return extent;
	}

	private void waitForNode(Node node, Node nodeToWaitFor) {
		List<Node> waiting = nodesToWaitFor.get(nodeToWaitFor);
		if (waiting == null) {
			waiting = new ArrayList<>();
			nodesToWaitFor.put(nodeToWaitFor, waiting);
		}
		if (!waiting.contains(node)) {
			waiting.add(node);
		}
	}

	private List<Node> waitingForNode(Node nodeToWaitFor) {
		List<Node> nodes = nodesToWaitFor.get(nodeToWaitFor);
		if (nodes == null) {
			nodes = Collections.emptyList();
		}
		return nodes;
	}

	private TemporalExtent createProperInterval(Node subject) {
		Iterator<Triple> triples;

		triples = graph.find(subject, Constants.INTERVAL_STARTED_BY, Node.ANY);
		Node start = null;
		try {
			while (triples.hasNext()) {
				Triple triple = triples.next();
				if (!triple.getObject().isLiteral()) {
					start = triple.getObject();
				}
			}
		} finally {
			NiceIterator.close(triples);
		}
		if (start == null) {
			return null;
		}

		triples = graph.find(subject, Constants.INTERVAL_FINISHED_BY, Node.ANY);
		Node end = null;
		try {
			while (triples.hasNext()) {
				Triple triple = triples.next();
				if (!triple.getObject().isLiteral()) {
					end = triple.getObject();
				}
			}
		} finally {
			NiceIterator.close(triples);
		}
		if (end == null) {
			return null;
		}

		Record<TemporalExtent> record = null;

		record = index.find(start);
		if (null == record) {
			waitForNode(subject, start);
			return null;
		}
		TemporalInstant startInstant = (TemporalInstant) record.getValue();

		record = index.find(end);
		if (null == record) {
			waitForNode(subject, end);
			return null;
		}
		TemporalInstant endInstant = (TemporalInstant) record.getValue();

		return new TemporalInterval(startInstant, endInstant);
	}

	private TemporalExtent createDateTimeInterval(Node subject) {
		Iterator<Triple> triples = graph.find(subject, Constants.DATE_TIME, Node.ANY);
		Node dateTime = null;
		try {
			while (triples.hasNext()) {
				Triple triple = triples.next();
				if (triple.getObject().isLiteral()) {
					dateTime = triple.getObject();
				}
			}
		} finally {
			NiceIterator.close(triples);
		}
		if (dateTime == null) {
			return null;
		}
		Object o = dateTime.getLiteralValue();

		if (!(o instanceof XSDDateTime)) {
			return null;
		}

		return new TemporalInstant((XSDDateTime) o);
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

	private static boolean isValidType(Node node) {
		boolean result = false;
		for (Node n : Constants.VALID_TYPES) {
			if (n.equals(node)) {
				result = true;
				break;
			}
		}
		return result;
	}
}
