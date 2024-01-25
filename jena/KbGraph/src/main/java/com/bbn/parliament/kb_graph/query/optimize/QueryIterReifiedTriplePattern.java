
package com.bbn.parliament.kb_graph.query.optimize;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.engine.iterator.QueryIter;
import org.apache.jena.sparql.engine.iterator.QueryIterRepeatApply;

import com.bbn.parliament.core.jni.ReificationIterator;
import com.bbn.parliament.core.jni.ReificationIterator.Reification;
import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.query.ReifiedTriple;

/** @author dkolas */
public class QueryIterReifiedTriplePattern extends QueryIterRepeatApply
{
	private final ReifiedTriple pattern;

	public QueryIterReifiedTriplePattern(QueryIterator input, ReifiedTriple pattern, ExecutionContext cxt)
	{
		super(input, cxt) ;
		this.pattern = pattern ;
	}

	@Override
	protected QueryIterator nextStage(Binding binding)
	{
		return new ReifiedTripleMapper(binding, pattern, getExecContext()) ;
	}

	private static class ReifiedTripleMapper extends QueryIter
	{
		private Node s ;
		private Node p ;
		private Node o ;
		private Node name;
		private Binding binding ;
		private KbGraph kbGraph;

		private ReificationIterator reificationIterator;
		private Binding slot = null ;
		private boolean finished = false ;

		ReifiedTripleMapper(Binding binding, ReifiedTriple pattern, ExecutionContext cxt)
		{
			super(cxt) ;
			this.s = substitute(pattern.getSubject(), binding) ;
			this.p = substitute(pattern.getPredicate(), binding) ;
			this.o = substitute(pattern.getObject(), binding) ;
			this.name = substitute(pattern.getName(), binding);

			this.binding = binding ;
			Graph graph = cxt.getActiveGraph() ;

			if (!(graph instanceof KbGraph)){
				throw new RuntimeException("QueryIterReifiedTriplePattern somehow being used on a non-Parliament kb graph!");
			}
			kbGraph = (KbGraph)graph;
			reificationIterator = kbGraph.findReifications(name, s, p, o) ;


		}

		@SuppressWarnings("unused")
		private static Node tripleNode(Node node)
		{
			if ( node.isVariable() )
				return Node.ANY ;
			return node ;
		}

		private static Node substitute(Node node, Binding binding)
		{
			if ( Var.isVar(node) )
			{
				Node x = binding.get(Var.alloc(node)) ;
				if ( x != null )
					return x ;
			}
			return node ;
		}

		private Binding mapper(ReifiedTriple r)
		{
			BindingMap results = BindingFactory.create(binding) ;

			if ( ! insert(s, r.getSubject(), results) )
				return null ;
			if ( ! insert(p, r.getPredicate(), results) )
				return null ;
			if ( ! insert(o, r.getObject(), results) )
				return null ;
			if ( !insert(name, r.getName(), results)){
				return null;
			}
			return results ;
		}

		private static boolean insert(Node inputNode, Node outputNode, BindingMap results)
		{
			if ( ! Var.isVar(inputNode) )
				return true ;

			Var v = Var.alloc(inputNode) ;
			Node x = results.get(v) ;
			if ( x != null )
				return outputNode.equals(x) ;

			results.add(v, outputNode) ;
			return true ;
		}

		@Override
		protected boolean hasNextBinding()
		{
			if ( finished ) return false ;
			if ( slot != null ) return true ;

			while(reificationIterator.hasNext() && slot == null )
			{
				ReifiedTriple t = getReifiedTriple(reificationIterator.next()) ;
				slot = mapper(t) ;
			}
			if ( slot == null )
				finished = true ;
			return slot != null ;
		}

		private ReifiedTriple getReifiedTriple(Reification next) {
			Node su = kbGraph.getResourceNodeForId(next.getSubject());
			Node pr = kbGraph.getResourceNodeForId(next.getPredicate());
			Node ob = next.isLiteral()
				? kbGraph.getLiteralNodeForId(next.getObject())
				: kbGraph.getResourceNodeForId(next.getObject());
			Node nm = kbGraph.getResourceNodeForId(next.getStatementName());

			return new ReifiedTriple(nm, su, pr, ob);
		}

		@Override
		protected Binding moveToNextBinding()
		{
			if ( ! hasNextBinding() )
				throw new ARQInternalErrorException() ;
			Binding r = slot ;
			slot = null ;
			return r ;
		}

		// Called for a super-class's close() method:
		@Override
		protected void closeIterator()
		{
			if ( reificationIterator != null ){
				reificationIterator.close();
			}
			reificationIterator = null ;
		}

		@Override
		protected void requestCancel() {
			// TODO can we cancel?
		}
	}
}
