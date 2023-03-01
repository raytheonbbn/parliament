package com.bbn.parliament.jena.query.index.pfunction;

import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropFuncArgType;
import org.apache.jena.sparql.pfunction.PropertyFunctionEval;
import org.apache.jena.sparql.util.IterLib;

import com.bbn.parliament.jena.graph.KbGraphStore;

public abstract class PFGraphsAsSubject extends PropertyFunctionEval {
	/**
	 * Get a list of one or more node objects provided to the property function.
	 *
	 * @param pfArg The property function.
	 * @return The list of node objects provided to the property function.
	 */
	private static final List<Node> getNodeArguments(PropFuncArg pfArg) {
		if (pfArg.isList()) {
			return pfArg.getArgList();
		} else if (pfArg.isNode()) {
			return Collections.singletonList(pfArg.getArg());
		} else {
			return Collections.emptyList();
		}
	}

	protected PFGraphsAsSubject(PropFuncArgType objectType) {
		super(PropFuncArgType.PF_ARG_EITHER, objectType);
	}

	protected abstract boolean checkObject(Node node);

	protected abstract boolean processGraphObject(Binding binding, Node graphName,
		Graph graph, Node object, ExecutionContext context);

	@Override
	public QueryIterator execEvaluated(Binding binding, PropFuncArg subject,
		Node predicate, PropFuncArg object, ExecutionContext execCxt) {
		DatasetGraph dsg = execCxt.getDataset();

		if (!(dsg instanceof KbGraphStore)) {
			throw new JenaException(predicate + " may only be run against Parliament KBs.");
		}
		KbGraphStore kbGraphStore = (KbGraphStore) dsg;
		List<Node> graphs = getNodeArguments(subject);
		for (Node g : graphs) {
			if (!g.isURI()) {
				throw new JenaException("The subject must be the URI of a graph");
			}
			if (!KbGraphStore.DEFAULT_GRAPH_NODE.equals(g)) {
				Graph graph = kbGraphStore.getGraph(g);
				if (null == graph) {
					throw new JenaException("The subject must be the URI of a graph");
				}
			}
		}

		List<Node> objects = getNodeArguments(object);
		for (Node o : objects) {
			if (!checkObject(o)) {
				throw new JenaException("'%s' is an invalid object for %s"
					.formatted(o, predicate));
			}
		}

		boolean success = false;
		for (Node g : graphs) {
			@SuppressWarnings("resource")
			Graph graph = KbGraphStore.DEFAULT_GRAPH_NODE.equals(g)
				? kbGraphStore.getDefaultGraph()
				: kbGraphStore.getGraph(g);
				for (Node o : objects) {
					success = processGraphObject(binding, g, graph, o, execCxt);
					if (!success) {
						break;
					}
				}
		}
		success = true;

		Literal result = ResourceFactory.createPlainLiteral(success ? "Success" : "Failure");
		return IterLib.oneResult(binding, Var.alloc("result"), result.asNode(), execCxt);
	}
}
