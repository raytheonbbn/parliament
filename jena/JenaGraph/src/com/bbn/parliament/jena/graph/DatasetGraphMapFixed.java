// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphCollection;

/**
 * This is basically a copy of
 * {@link com.hp.hpl.jena.sparql.core.DatasetGraphMap}, but it contains the fix
 * from JENA-116. This class should be removed when ARQ is updated.
 */
public class DatasetGraphMapFixed extends DatasetGraphCollection
{
	private Map<Node, Graph> graphs = new HashMap<>() ;

	private Graph defaultGraph ;

	public DatasetGraphMapFixed()
	{}

	public DatasetGraphMapFixed(Graph defaultGraph)
	{ this.defaultGraph = defaultGraph ; }

	/**
	 * Create a new DatasetGraph that initially shares the graphs of the given
	 * DatasetGraph. Adding/removing graphs will only affect this object, not the
	 * argument DatasetGraph but changed to shared graphs are seen by both objects.
	 */
	public DatasetGraphMapFixed(DatasetGraph dsg)
	{
		this(dsg.getDefaultGraph()) ;
		for ( Iterator<Node> names = dsg.listGraphNodes() ; names.hasNext() ; )
		{
			Node gn = names.next() ;
			this.addGraph(gn, dsg.getGraph(gn)) ;
		}
	}

	@Override
	public boolean containsGraph(Node graphNode)
	{
		return graphs.containsKey(graphNode) ;
	}

	@Override
	public Graph getDefaultGraph()
	{
		return defaultGraph ;
	}

	@Override
	public Graph getGraph(Node graphNode)
	{
		Graph g = graphs.get(graphNode) ;
		if ( g == null )
		{
			g = getGraphCreate() ;
			if ( g != null )
				this.addGraph(graphNode, g) ;
		}
		return g ;
	}

	/**
	 * Called from getGraph when a nonexistent graph is asked for. Return null
	 * for "nothing created as a graph"
	 */
	protected static Graph getGraphCreate() { return null ; }

	@Override
	public void addGraph(Node graphName, Graph graph)
	{
		graphs.put(graphName, graph) ;
	}

	@Override
	public void removeGraph(Node graphName)
	{
		graphs.remove(graphName) ;
	}

	@Override
	public void setDefaultGraph(Graph g)
	{
		defaultGraph = g ;
	}

	@Override
	public Iterator<Node> listGraphNodes()
	{
		return graphs.keySet().iterator() ;
	}

	@Override
	public long size()
	{
		return graphs.size() ;
	}

	@Override
	public void close()
	{
		defaultGraph.close();
		for ( Graph graph : graphs.values() )
			graph.close();
		super.close() ;
	}
}
