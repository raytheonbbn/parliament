// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.pfunction;

import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.pfunction.PFuncSimpleAndList;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.util.IterLib;

import com.bbn.parliament.jena.graph.KbGraphStore;

/**
 * This property function allows you to create KbUnionGraphs (you can delete
 * them using regular SPARQL/Update semantics: {@code "drop graph <uri>"}). Here
 * is an example SPARQL/Update query to create a union graph:
 *
 * <pre>
 * PREFIX parPF: &lt;java:com.bbn.parliament.jena.pfunction.&gt;
 * INSERT { }
 * WHERE
 * {
 *    &lt;http://example.org/union1&gt; parPF:createUnionGraph ( &lt;http://example.org/NewGraph&gt; &lt;http://example.org/NewGraph2&gt; ) .
 * }
 * </pre>
 *
 * @author sallen
 */
public class createUnionGraph extends PFuncSimpleAndList {
	@Override
	public QueryIterator execEvaluated(Binding binding, Node subject, Node predicate, PropFuncArg object, ExecutionContext execCxt) {
		DatasetGraph dsg = execCxt.getDataset();

		if (!(dsg instanceof KbGraphStore kbGraphStore)) {
			throw new JenaException("createUnionGraph property function may only be run against Parliament KBs.");
		} else {
			if (null == subject) {
				throw new JenaException("The subject must contain a URI for the new KbUnionGraph");
			}

			List<Node> args = getNodeArguments(object);

			if (args.size() != 2) {
				throw new JenaException("This property function requires exactly two arguments that correspond to the left and right graphs");
			}
			Node graphName = subject;
			Node leftGraphName = args.get(0);
			Node rightGraphName = args.get(1);

			if (!graphName.isURI()) {
				throw new JenaException("createUnionGraph property function must have a URI for the graph as its first argument.");
			}
			if (!leftGraphName.isURI()) {
				throw new JenaException("createUnionGraph property function must have a URI for the Left graph as its second argument.");
			}
			if (!rightGraphName.isURI()) {
				throw new JenaException("createUnionGraph property function must have a URI for the Right graph as its third argument.");
			}

			boolean success = kbGraphStore.addUnionGraph(graphName, leftGraphName, rightGraphName);

			Node result = ResourceFactory.createPlainLiteral(success ? "Success" : "Failure").asNode();

			return IterLib.oneResult(binding, Var.alloc("result"), result, execCxt);
		}
	}

	/**
	 * Get a list of one or more node objects provided to the property function.
	 *
	 * @param pfArg The property function.
	 * @return The list of node objects provided to the property function.
	 */
	private final static List<Node> getNodeArguments(PropFuncArg pfArg)
	{
		List<Node> toReturn = Collections.emptyList();

		// This function is necessary to handle both
		// single or multiple additional entities.
		if(pfArg.isList()) {
			toReturn = pfArg.getArgList();
		} else if(pfArg.isNode()) {
			toReturn = Collections.singletonList(pfArg.getArg());
		}

		return toReturn;
	}
}
