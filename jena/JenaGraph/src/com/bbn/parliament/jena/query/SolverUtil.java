package com.bbn.parliament.jena.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.Constants;
import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.RangeIndex;
import com.bbn.parliament.jena.query.index.GraphSubPattern;
import com.bbn.parliament.jena.query.index.IndexPattern;
import com.bbn.parliament.jena.query.index.IndexPatternIterator;
import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.bbn.parliament.jena.query.index.IndexPatternQuerierManager;
import com.bbn.parliament.jena.query.index.RangeIndexQueryIterator;
import com.bbn.parliament.jena.query.optimize.DefaultCountTransformation;
import com.bbn.parliament.jena.query.optimize.IndexTransformation;
import com.bbn.parliament.jena.query.optimize.ReorderQueryIterTriplePattern;
import com.bbn.parliament.jena.query.optimize.UpdatedStaticCountTransformation;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternPropertyFunction;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterFilterExpr;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import com.hp.hpl.jena.sparql.expr.E_GreaterThan;
import com.hp.hpl.jena.sparql.expr.E_GreaterThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_LessThan;
import com.hp.hpl.jena.sparql.expr.E_LessThanOrEqual;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.util.IterLib;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Utility class for solving queries. The methods for answering BasicPatterns,
 * patterns containing index property functions, and patterns that need to be
 * filtered.
 * <br><br>
 * Optimizations on the patterns include:
 * <ul>
 * <li>optimizing the triple order by count</li>
 * <li>optimizing the order of discrete sub patterns by estimating their counts</li>
 * <li>optimizing filters to use {@link RangeIndex}es</li>
 * </ul>
 *
 * @author rbattle
 */
public class SolverUtil {
	public interface BasicGraphSolverExecutor {
		public QueryIterator handle(BasicPattern pattern, QueryIterator input,
			ExecutionContext context);
	}

	public static BasicGraphSolverExecutor DEFAULT_SOLVER_EXECUTOR = new BasicGraphSolverExecutor() {
		@Override
		public QueryIterator handle(BasicPattern pattern, QueryIterator input,
			ExecutionContext context) {
			return new ReorderQueryIterTriplePattern(pattern, input, context);
		}
	};

	private static final Logger LOG = LoggerFactory.getLogger(SolverUtil.class);

	private static final Node rdfSubject = Node.createURI(RDF.subject.getURI());
	private static final Node rdfPredicate = Node.createURI(RDF.predicate.getURI());
	private static final Node rdfObject = Node.createURI(RDF.object.getURI());
	private static final Node rdfType = Node.createURI(RDF.type.getURI());
	private static final Node rdfStatement = Node.createURI(RDF.Statement.getURI());

	public static QueryIterator solve(
		IndexSubPatternPropertyFunction<?> indexPattern,
		BasicPattern remainingPattern, Binding binding,
		ExecutionContext context, Graph graph, BasicGraphSolverExecutor handler) {

		BasicPattern remaining = remainingPattern;
		if (remainingPattern.getList().size() > 1) {
			if (graph instanceof KbGraph kbGraph) {
				remaining = optimizeTripleOrder(remaining, kbGraph, context);
			}
		}
		List<Index<?>> indexes = IndexManager.getInstance().getIndexes(graph);
		IndexTransformation it = new IndexTransformation(graph, indexes,
			indexPattern);
		BasicPattern pattern = it.reorder(remaining);
		QueryIterator chain = IterLib.result(binding, context);
		chain = processPattern(pattern, chain, context, handler);
		return chain;
	}

	public static BasicPattern optimizeTripleOrder(BasicPattern pattern,
		KbGraph graph, ExecutionContext context) {
		// run graph level optimizations
		ReorderTransformation transformation = null;
		if (context.getContext().isTrue(Constants.DYNAMIC_OPTIMIZATION)) {
			transformation = new UpdatedStaticCountTransformation(graph);
		} else if (context.getContext()
			.isTrueOrUndef(Constants.DEFAULT_OPTIMIZATION)) {
			transformation = new DefaultCountTransformation(graph);
		}
		if (null == transformation) {
			return pattern;
		}
		return transformation.reorder(pattern);
	}

	public static QueryIterator solve(GraphSubPattern pattern, QueryIterator input,
		ExecutionContext execCxt, BasicGraphSolverExecutor handler) {
		return processPattern(pattern, input, execCxt, handler);
	}

	public static QueryIterator solve(BasicPattern pattern, QueryIterator input,
		ExecutionContext execCxt, Graph graph, BasicGraphSolverExecutor handler) {

		if (pattern instanceof GraphSubPattern) {
			return processPattern(pattern, input, execCxt, handler);
		}

		BasicPattern toProcess = pattern;
		if (graph instanceof KbGraph kbGraph && toProcess.size() > 1) {
			toProcess = collapseReifications(toProcess);
			toProcess = optimizeTripleOrder(toProcess, kbGraph, execCxt);
		}

		List<Index<?>> indexes = IndexManager.getInstance().getIndexes(graph);
		if (indexes.size() > 0) {
			toProcess = new IndexTransformation(graph, indexes)
				.reorder(toProcess);

		}
		if (LOG.isDebugEnabled() && !toProcess.equals(pattern)) {
			LOG.debug("Original Pattern: {}", pattern);
			LOG.debug("Optimized Pattern: {}", toProcess);
		}

		QueryIterator chain = input;

		chain = processPattern(toProcess, chain, execCxt, handler);
		return chain;
	}

	private static BasicPattern collapseReifications(BasicPattern toProcess) {
		List<Triple> resultTriples = new ArrayList<>();
		Map<Node, Node[]> reifications = new HashMap<>();
		for (Triple triple : toProcess){
			if (!checkReification(triple, reifications)){
				resultTriples.add(triple);
			}
		}

		BasicPattern result = new BasicPattern();
		for (Triple t : resultTriples){
			result.add(t);
		}
		for (Node name : reifications.keySet()){
			Node[] reification = reifications.get(name);
			result.add(new ReifiedTriple(name,reification[0],reification[1], reification[2]));
		}
		return result;
	}

	private static boolean checkReification(Triple triple, Map<Node, Node[]> reifications) {
		Node[] reification = null;
		if (triple.predicateMatches(rdfSubject)){
			reification = getReification(reifications, triple.getSubject());
			if (Node.ANY.equals(reification[0])){
				reification[0] = triple.getObject();
				return true;
			}
		}else if (triple.predicateMatches(rdfPredicate)) {
			reification = getReification(reifications, triple.getSubject());
			if (Node.ANY.equals(reification[1])){
				reification[1] = triple.getObject();
				return true;
			}
		}else if (triple.predicateMatches(rdfObject)) {
			reification = getReification(reifications, triple.getSubject());
			if (Node.ANY.equals(reification[2])){
				reification[2] = triple.getObject();
				return true;
			}
		}else if (triple.predicateMatches(rdfType) && triple.objectMatches(rdfStatement)){
			return true;
		}
		return false;
	}

	private static Node[] getReification(Map<Node, Node[]> reifications,
		Node subject) {
		Node[] reification = reifications.get(subject);
		if (reification == null){
			reification = new Node[3];
			reification[0] = reification[1] = reification[2] = Node.ANY;
			reifications.put(subject, reification);
		}
		return reification;
	}

	private static QueryIterator processPattern(BasicPattern pattern,
		QueryIterator input, ExecutionContext execCxt,
		BasicGraphSolverExecutor handler) {
		QueryIterator result = input;
		if (pattern instanceof IndexPattern indexPattern) {
			result = new IndexPatternIterator(indexPattern, result, execCxt);
		} else {
			result = handler.handle(pattern, input, execCxt);
		}
		return result;
	}

	private static boolean containsVar(Triple t, Node var) {
		return t.getSubject().equals(var) || t.getPredicate().equals(var)
			|| t.getObject().equals(var);
	}

	public static QueryIterator solve(ExprList exprs, BasicPattern subPattern,
		QueryIterator input, ExecutionContext context) {
		Graph graph = context.getActiveGraph();

		Map<ExprFunction2, FilterInfo> exprToIndex = new HashMap<>();
		for (Index<?> index : IndexManager.getInstance()
			.getIndexes(context.getActiveGraph())) {
			if (!(index instanceof RangeIndex)) {
				continue;
			}
			RangeIndex<?> rindex = (RangeIndex<?>) index;
			IndexPatternQuerier querier = IndexPatternQuerierManager.getInstance()
				.get(index);
			if (null == querier) {
				continue;
			}

			BasicPattern pattern = querier.examine(subPattern);
			if (pattern.size() > 0) {
				for (Expr e : exprs) {
					NodeValue constant = null;
					ExprVar var = null;
					ExprFunction2 e2 = null;
					if (!(e instanceof E_LessThan || e instanceof E_LessThanOrEqual
						|| e instanceof E_GreaterThan || e instanceof E_GreaterThanOrEqual)) {
						continue;
					}
					ExprFunction2 exp = (ExprFunction2) e;
					e2 = exp;
					Expr arg1 = exp.getArg1();
					Expr arg2 = exp.getArg2();
					if (arg1.isVariable() && arg2.isConstant()) {

						var = arg1.getExprVar();
						constant = arg2.getConstant();
					}
					if (arg2.isVariable() && arg1.isConstant()) {
						var = arg2.getExprVar();
						constant = arg1.getConstant();
					}

					if (null == var || null == constant) {
						continue;
					}
					for (Triple t : pattern) {
						if (!t.getSubject().isVariable()) {
							continue;
						}
						if (!containsVar(t, var.asVar().asNode())) {
							continue;
						}
						Var resourceVar = Var.alloc(t.getSubject());
						FilterInfo info = new FilterInfo(rindex, resourceVar);
						exprToIndex.put(e2, info);
					}
				}
			}
		}

		QueryIterator result = input;
		// move to SolveUtil
		// create class containing constant, variable, and varfirst
		// call rangeIndex.iterator(start, end) for expr and wrap to make query
		// iterators
		for (Map.Entry<ExprFunction2, FilterInfo> entry : exprToIndex.entrySet()) {
			FilterInfo info = entry.getValue();
			ExprFunction2 expr = entry.getKey();

			result = createRangeIterator(info.getIndex(), expr, info.getVar(),
				result, context);
			result = new QueryIterFilterExpr(result, expr, context);
		}
		List<Expr> remaining = new ArrayList<>(exprs.getList());
		remaining.removeAll(exprToIndex.keySet());

		result = SolverUtil.solve(subPattern, result, context, graph,
			DEFAULT_SOLVER_EXECUTOR);
		for (Expr expr : remaining) {
			result = new QueryIterFilterExpr(result, expr, context);
		}

		return result;
	}

	private static <T extends Comparable<T>> QueryIterator createRangeIterator(
		RangeIndex<T> index, ExprFunction2 expr, Var resourceVar,
		QueryIterator input, ExecutionContext context) {
		return new RangeIndexQueryIterator<>(expr, resourceVar, index, input,
			context);
	}

	private static class FilterInfo {
		private Var var;
		private RangeIndex<?> index;

		public FilterInfo(RangeIndex<?> index, Var var) {
			this.var = var;
			this.index = index;
		}

		public Var getVar() {
			return var;
		}

		public RangeIndex<?> getIndex() {
			return index;
		}
	}
}
