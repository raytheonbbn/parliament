package com.bbn.parliament.kb_graph.index.temporal.pt;

import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;

import com.bbn.parliament.kb_graph.index.temporal.Constants;

/**
 * This enum is the index of the index, i.e., it holds all necessary fields for
 * each temporal entity than the temporal index can use.
 *
 * @author mhale
 */
public enum TemporalIndexField {
	INSTANT(Constants.PT_AS_INSTANT, Constants.OT_INSTANT, XSDDatatype.XSDdateTime) {
		@Override
		public void addSpecificStatements(Node root, List<Triple> triples) {
			triples.add(Triple.create(root, RDF.type.asNode(), getType()));
		}
	},
	INTERVAL(Constants.PT_AS_INTERVAL, Constants.PROPER_INTERVAL, new PTInterval(Constants.PT_TIME_INTERVAL.getURI())) {
		@Override
		public void addSpecificStatements(Node root, List<Triple> triples) {
			triples.add(Triple.create(root, RDF.type.asNode(), getType()));
			triples.add(Triple.create(root, RDF.type.asNode(), Constants.OT_INTERVAL));
		}
	};

	private final Node predicate;
	private final Node type;
	private final RDFDatatype datatype;

	public void addInferredStatements(Node root, List<Triple> triples) {
		triples.add(Triple.create(root, RDF.type.asNode(), Constants.TEMPORAL_ENTITY));
		addSpecificStatements(root, triples);
	}

	public abstract void addSpecificStatements(Node root, List<Triple> triples);

	/**
	 * @param predicate A Node referring to the unique predicate used when
	 *        assigning literals to resources of the corresponding field
	 *        mentioned above.
	 * @param type A Node referencing the temporal object type of the
	 *        corresponding field
	 * @param datatype A new instance of a subclass of {@link PTDatatype} (and in
	 *        turn {@link RDFDatatype}). The URI entered in the constructor will
	 *        be registered in the Jena type mapper upon initialization.
	 */
	private TemporalIndexField(Node predicate, Node type, RDFDatatype datatype) {
		this.predicate = predicate;
		this.type = type;
		this.datatype = datatype;
	}

	public RDFDatatype getDatatype() {
		return datatype;
	}

	public Node getType() {
		return type;
	}

	public Node getPredicate() {
		return predicate;
	}
}
