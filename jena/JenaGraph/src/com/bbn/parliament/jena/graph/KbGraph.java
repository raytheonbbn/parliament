// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Capabilities;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.Kb;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.union.KbUnionableGraph;
import com.bbn.parliament.jena.util.NodeUtil;
import com.bbn.parliament.jni.KbConfig;
import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.jni.KbInstance.CountStmtsResult;
import com.bbn.parliament.jni.KbInstance.GetExcessCapacityResult;
import com.bbn.parliament.jni.ReificationIterator;
import com.bbn.parliament.jni.StmtIterator;
import com.bbn.parliament.jni.StmtIterator.Statement;

public class KbGraph extends GraphBase implements KbUnionableGraph, Closeable {
	public static final String MAGICAL_BNODE_PREFIX = "~@#$BNODE";

	private static Logger log = LoggerFactory.getLogger(KbGraph.class);
	private KbInstance kb;
	private KbConfig config;
	private boolean isClosed;
	// private NodeIdHash nodeIdHash;
	private Map<Node, Long> nodeIdHash;
	private OptimizationMethod optimizationMethod;

	private String relativeDirectory;

	static {
		Kb.init();
	}

	/**
	 * Creates a KbGraph that wraps the Parliament instance referred to by the
	 * given KbConfig instance.
	 *
	 * @param config The configuration to pass to the underlying Parliament
	 *        instance.
	 * @param relativeDirectory The path of the directory where this graph's
	 *        Parliament files should be stored. This path must be relative to
	 *        the kbDirectoryPath specified in the Parliament configuration. Set
	 *        to null to use the kbDirectoryPath. This setting is used internally
	 *        to create named graphs within a Parliament KB.
	 * @param optMethod The optimization method that the query optimizer should
	 *        use. Should be set to OptimizationMethod.DefaultOptimization.
	 */
	public KbGraph(KbConfig config, String relativeDirectory, OptimizationMethod optMethod) {
		try {
			kb = new KbInstance(config);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		this.config = config;
		this.relativeDirectory = relativeDirectory;
		isClosed = false;
		// nodeIdHash = new NodeIdHash(100);
		nodeIdHash = Collections.synchronizedMap(new LRUHash(1000));
		this.optimizationMethod = optMethod;
	}

	public KbConfig getConfig() {
		return config;
	}

	public String getRelativeDirectory() {
		return relativeDirectory;
	}

	@Override
	public void performDelete(Triple t) {
		long subjId, predId, objId;

		subjId = getKbId(t.getSubject(), false);
		predId = getKbId(t.getPredicate(), false);
		objId = getKbId(t.getObject(), false);
		if (subjId != -2 && predId != -2 && objId != -2) {
			kb.deleteStmt(subjId, predId, objId);
		}
	}

	public boolean isTripleInferred(Triple t) {
		long subjId, predId, objId;
		subjId = getKbId(t.getSubject(), false);
		predId = getKbId(t.getPredicate(), false);
		objId = getKbId(t.getObject(), false);
		if (subjId != -2 && predId != -2 && objId != -2) {
			try (StmtIterator si = kb.find(subjId, predId, objId, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
				if (si.hasNext()){
					Statement statement = si.next();
					return statement.isInferred();
				}else{
					return false;
				}
			}
		}
		return false;
	}

	public long getKbId(Node n, boolean createIfNotExists) {
		long id = KbInstance.NULL_RSRC_ID;
		if (n != null && !n.isVariable()) {
			Long idObject = nodeIdHash.get(n);
			if (idObject != null) {
				id = idObject.longValue();
			} else {
				String stringRep = NodeUtil.getStringRepresentation(n);
				id = kb.uriToRsrcId(stringRep, n.isLiteral(), createIfNotExists);
				if (!createIfNotExists && id == KbInstance.NULL_RSRC_ID) {
					id = -2;
				} else {
					nodeIdHash.put(n, id);
				}
			}
		}
		return id;
	}

	/**
	 * Returns an iterator over all the Triples that match the triple pattern.
	 *
	 * @param m
	 *            a Triple[Match] encoding the pattern to look for
	 * @return an iterator of all triples in this graph that match m
	 */
	@SuppressWarnings("resource")
	@Override
	public ExtendedIterator<Triple> graphBaseFind(Triple m) {
		try {
			long subjId, predId, objId;
			subjId = getKbId(m.getMatchSubject(), false);
			predId = getKbId(m.getMatchPredicate(), false);
			objId = getKbId(m.getMatchObject(), false);
			if (subjId == -2 || predId == -2 || objId == -2) {
				return NiceIterator.emptyIterator();
			} else {
				return new KbTripleIterator(this,
					kb.find(subjId, predId, objId, KbInstance.SKIP_DELETED_STMT_ITER_FLAG));
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public ReificationIterator findReifications(Node name, Node subject, Node predicate,
		Node object) {
		long subjId, predId, objId, nameId;
		subjId = getKbId(Node.ANY.equals(subject) ? null : subject, false);
		predId = getKbId(Node.ANY.equals(predicate) ? null : predicate, false);
		objId = getKbId(Node.ANY.equals(object) ? null : object, false);
		nameId = getKbId(Node.ANY.equals(name) ? null : name, false);
		if (subjId == -2 || predId == -2 || objId == -2 || nameId == -2){
			return ReificationIterator.EMPTY_ITERATOR;
		}else{
			ReificationIterator ri = kb.findReifications(nameId, subjId, predId, objId);
			return ri;
		}
	}

	@Override
	public long getNodeCountInPosition(Node node, int position) {
		long id = getKbId(node, false);
		if (id == -2) {
			return 0;
		}

		return switch (position) {
			case 1 -> kb.subjectCount(id);
			case 2 -> kb.predicateCount(id);
			case 3 -> kb.objectCount(id);
			default -> Long.MAX_VALUE;
		};
	}

	public void flush() {
		kb.sync();
	}

	@Override
	public void performAdd(Triple t) {
		long subject = getKbId(t.getSubject(), true);
		long predicate = getKbId(t.getPredicate(), true);
		long object = getKbId(t.getObject(), true);
		kb.addStmt(subject, predicate, object, false);
	}

	/**
	 * Free all resources, any further use of this graph is an error.
	 */
	@Override
	public void close() {
		if (!isClosed) {
			kb.finalize();
			log.debug("KbGraph closed");
			super.close();
			isClosed = true;
		}
	}

	@Override
	public void finalize() {
		close();
	}

	/**
	 * Answer true iff this graph is empty. "Empty" means "has as few triples as
	 * it can manage", because an inference graph may have irremovable axioms and
	 * their consequences.
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * For a concrete graph this returns the number of triples in the graph. For
	 * graphs which might infer additional triples it results an estimated lower
	 * bound of the number of triples. For example, an inference graph might
	 * return the number of triples in the raw data graph.
	 */
	@Override
	public int graphBaseSize() {
		CountStmtsResult result = kb.countStmts();
		return (int) (result.getTotal() - result.getNumDel());
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		ExtendedIterator<Triple> iterator = this.find(Node.ANY, Node.ANY,
			Node.ANY);
		while (iterator.hasNext()) {
			buffer.append(iterator.next() + "\n");
		}
		return buffer.toString();
	}

	@Override
	public Capabilities getCapabilities() {
		return new KbCapabilities();
	}

	private static class KbCapabilities implements Capabilities {
		@Override
		public boolean addAllowed() {
			return true;
		}

		@Override
		public boolean addAllowed(boolean everyTriple) {
			return true;
		}

		@Override
		public boolean canBeEmpty() {
			return true;
		}

		@Override
		public boolean deleteAllowed() {
			return true;
		}

		@Override
		public boolean deleteAllowed(boolean everyTriple) {
			return true;
		}

		@Override
		public boolean findContractSafe() {
			return true;
		}

		@Override
		public boolean handlesLiteralTyping() {
			return false;
		}

		@Override
		public boolean iteratorRemoveAllowed() {
			return true;
		}

		@Override
		public boolean sizeAccurate() {
			return true;
		}
	}

	@Override
	public void clear() {
		kb.finalize();
		kb = null;
		KbInstance.deleteKb(config, null);
		nodeIdHash.clear();
		try {
			kb = new KbInstance(config);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		// since this does not "clear" in a way that a graph listener can listen
		// on, manually clear indexes
		for (Index<?> index : IndexManager.getInstance().getIndexes(this)) {
			index.clear();
		}
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	public Node getResourceNodeForId(long resourceId) {
		String representation = kb.rsrcIdToUri(resourceId);
		Node result = null;
		if (representation.startsWith(MAGICAL_BNODE_PREFIX)) {
			// The equivalent concept for the (BlankNodeId) API is AnonId. Historically, that has been in the org.apache.jena.rdf.model package.
			result = NodeFactory.createBlankNode(BlankNodeId.create(representation
				.substring(MAGICAL_BNODE_PREFIX.length())));
		} else {
			result = NodeFactory.createURI(representation);
		}
		return result;
	}

	public Node getLiteralNodeForId(long resourceId) {
		String literal = kb.rsrcIdToUri(resourceId);
		String lexicalForm = "";
		String lang = "";
		String datatype = "";
		Node result = null;

		int caretIndex = literal.lastIndexOf("^^");
		int atIndex = literal.lastIndexOf('@');
		int quoteIndex = literal.lastIndexOf('\"');
		int singleQuoteIndex = literal.lastIndexOf('\'');
		if (caretIndex > quoteIndex && caretIndex > singleQuoteIndex) {
			datatype = literal.substring(caretIndex + 2);
			literal = literal.substring(0, caretIndex)
				+ literal.substring(caretIndex + 2, literal.length());
		}
		if (atIndex > quoteIndex && atIndex > singleQuoteIndex) {
			lang = literal.substring(atIndex + 1);
			literal = literal.substring(0, atIndex)
				+ literal.substring(atIndex + 1, literal.length());
		}

		if (singleQuoteIndex > quoteIndex) {
			lexicalForm = literal.substring(1, singleQuoteIndex);
		} else {
			lexicalForm = literal.substring(1, quoteIndex);
		}
		if (!datatype.equals("")) {
			result = NodeFactory.createLiteral(lexicalForm, lang, NodeFactory.getType(datatype));
		} else if (!lang.equals("")) {
			result = NodeFactory.createLiteral(lexicalForm, lang, null);
		} else {
			result = NodeFactory.createLiteral(lexicalForm);
		}
		return result;
	}

	public boolean validateUnderlyingStorage(PrintStream s) {
		return kb.validate(s);
	}

	public CountStmtsResult getUnderlyingStorageStatistics() {
		return kb.countStmts();
	}

	public GetExcessCapacityResult getUnderlyingStorageExcessCapacity() {
		return kb.getExcessCapacity();
	}

	public long getUnderlyingStorageRsrcCount() {
		return kb.rsrcCount();
	}

	public long getUnderlyingStorageRuleCount() {
		return kb.ruleCount();
	}

	public OptimizationMethod getOptimizationMethod() {
		return optimizationMethod;
	}

	/**
	 * Get a quick size estimate using Parliament's raw statement count.
	 * This will include statements that have been deleted, and
	 * not include statements that are virtual.
	 *
	 * @return Raw statement count
	 */
	public long getQuickSizeEstimate() {
		return kb.stmtCount();
	}

	/** Exports the KB in N-Triples format. */
	public void dumpAsNTriples(OutputStream s, boolean includeInferredStmts,
		boolean includeDeletedStmts, boolean useAsciiOnlyEncoding) {
		kb.dumpKbAsNTriples(s, includeInferredStmts, includeDeletedStmts,
			useAsciiOnlyEncoding);
	}
}
