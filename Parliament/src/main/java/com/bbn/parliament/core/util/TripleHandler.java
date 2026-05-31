package com.bbn.parliament.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.core.jni.KbInstance;

public class TripleHandler implements StreamRDF {
	private static Logger LOG = LoggerFactory.getLogger(TripleHandler.class);

	private final KbInstance _kb;
	private final Map<Node_Blank, Long> _bNodeMap;
	private long _startTime = System.currentTimeMillis();
	private long _stmtCount;

	public TripleHandler(KbInstance kb) {
		_kb = Objects.requireNonNull(kb, "kb");
		_bNodeMap = new HashMap<>();
		_startTime = System.currentTimeMillis();
		_stmtCount = 0;
	}

	@Override
	public void start() {
		_startTime = System.currentTimeMillis();
		_stmtCount = 0;
	}

	@Override
	public void triple(Triple triple) {
		var subjectId = getResourceId(triple.getSubject());
		var predicateId = getResourceId(triple.getPredicate());
		var objectId = getResourceId(triple.getObject());
		_kb.addStmt(subjectId, predicateId, objectId, false);
		++_stmtCount;
	}

	private long getResourceId(Node node) {
		if (node instanceof Node_Blank bNode) {
			var bNodeId = _bNodeMap.get(bNode);
			if (bNodeId == null) {
				var resourceId = _kb.createAnonymousRsrc();
				_bNodeMap.put(bNode, resourceId);
				return resourceId;
			} else {
				return bNodeId.longValue();
			}
		} else if (node instanceof Node_URI uriNode) {
			return _kb.uriToRsrcId(uriNode.getURI(), false, true);
		} else if (node instanceof Node_Literal litNode) {
			return _kb.uriToRsrcId(litNode.toString(true), true, true);
		} else {
			throw new UnsupportedOperationException("Nodes must be URI, Literal, or blank");
		}
	}

	// Quads that are the default graph or no graph are redirected
	@Override
	public void quad(Quad quad) {
		if (quad.isDefaultGraph() || quad.getGraph() == null) {
			this.triple(quad.asTriple());
		} else {
			throw new UnsupportedOperationException("Quads and named graphs are not supported");
		}
	}

	@Override
	public void base(String base) {
		// Do nothing
	}

	@Override
	public void prefix(String prefix, String iri) {
		// Do nothing
	}

	@Override
	public void finish() {
		if (LOG.isInfoEnabled()) {
			var duration = (System.currentTimeMillis() - _startTime) / 1000.0;
			var rate = (duration > 0) ? (_stmtCount / duration) : 0;

			LOG.info("Inserted {} statements in {} seconds ({} statements per second)",
				_stmtCount, duration, rate);
		}
	}
}
