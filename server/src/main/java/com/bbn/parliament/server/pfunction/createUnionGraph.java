// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.pfunction;

import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.pfunction.PFuncSimpleAndList;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.util.IterLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.KbGraphStore;

/**
 * This property function allows you to create KbUnionGraphs. Here is an example
 * SPARQL/Update query to create a union graph:
 *
 * <pre>{@code
 * prefix parPF: <java:com.bbn.parliament.server.pfunction.>
 * insert {} where {
 *    <http://example.org/union1> parPF:createUnionGraph
 *       ( <http://example.org/NewGraph> <http://example.org/NewGraph2> ) .
 * }
 * }</pre>
 *
 * To delete a union graph created this way, use the usual SPARQL-Update
 * construct:
 *
 * <pre>{@code
 * drop [ silent ] graph <uri>
 * }</pre>
 *
 * @author sallen
 */
public class createUnionGraph extends PFuncSimpleAndList {
	private static final Logger LOG = LoggerFactory.getLogger(createUnionGraph.class);

	@Override
	public QueryIterator execEvaluated(Binding binding, Node subject, Node predicate, PropFuncArg object, ExecutionContext execCxt) {
		if (!(execCxt.getDataset() instanceof KbGraphStore kbGraphStore)) {
			throw new JenaException("createUnionGraph property function may only be run against Parliament KBs.");
		} else if (null == subject) {
			throw new JenaException("createUnionGraph property function subject must contain a URI for the new KbUnionGraph");
		}

		List<Node> args = getNodeArguments(object);
		if (args.size() != 2) {
			throw new JenaException("createUnionGraph property function requires exactly two arguments that correspond to the left and right graphs");
		}

		Node graphName = subject;
		Node leftGraphName = args.get(0);
		Node rightGraphName = args.get(1);
		if (!graphName.isURI()) {
			throw new JenaException("createUnionGraph property function must have a URI for the graph as its first argument.");
		} else if (!leftGraphName.isURI()) {
			throw new JenaException("createUnionGraph property function must have a URI for the Left graph as its second argument.");
		} else if (!rightGraphName.isURI()) {
			throw new JenaException("createUnionGraph property function must have a URI for the Right graph as its third argument.");
		}

		boolean success = kbGraphStore.addUnionGraph(graphName, leftGraphName, rightGraphName);
		Node result = NodeFactory.createLiteral(success ? "Success" : "Failure");
		LOG.debug("createUnionGraph result for <{}>: {}", graphName.getURI(), result.getLiteralValue());
		return IterLib.oneResult(binding, Var.alloc("result"), result, execCxt);
	}

	/**
	 * Get a list of one or more node objects provided to the property function.
	 * This function is necessary to handle both single or multiple additional
	 * entities.
	 *
	 * @param pfArg The property function argument.
	 * @return The list of node objects provided to the property function.
	 */
	private static List<Node> getNodeArguments(PropFuncArg pfArg)
	{
		if (pfArg.isList()) {
			return pfArg.getArgList();
		} else if (pfArg.isNode()) {
			return Collections.singletonList(pfArg.getArg());
		} else {
			return Collections.emptyList();
		}
	}
}
