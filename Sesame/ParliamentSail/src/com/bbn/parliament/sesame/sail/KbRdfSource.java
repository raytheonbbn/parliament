// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbRdfSource.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sesame.sail.NamespaceIterator;
import org.openrdf.sesame.sail.RdfSource;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.StatementIterator;
import org.openrdf.sesame.sail.query.BooleanExpr;
import org.openrdf.sesame.sail.query.GraphPattern;
import org.openrdf.sesame.sail.query.GraphPatternQuery;
import org.openrdf.sesame.sail.query.PathExpression;
import org.openrdf.sesame.sail.query.Query;
import org.openrdf.sesame.sail.query.QueryOptimizer;
import org.openrdf.sesame.sail.query.SetOperator;
import org.openrdf.sesame.sail.query.TriplePattern;
import org.openrdf.sesame.sail.query.Var;
import org.openrdf.sesame.sail.util.EmptyStatementIterator;
import org.openrdf.sesame.sailimpl.memory.MemNamespaceIterator;

import com.bbn.parliament.jni.Config;
import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.jni.StmtIterator;
import com.bbn.parliament.queryoptimization.Constraint;
import com.bbn.parliament.queryoptimization.TreeWidthQueryOptimizer;

public class KbRdfSource implements RdfSource, ValueFactory
{
	/** Key used to specify a data directory in the initialization parameters. */
	public static final String DATA_DIR_KEY  = "dir";
	public static final String OPTIMIZATION_TYPE_KEY = "optimizationType";
	public static final String NAMESPACE_ONT = "http://parliament.semwebcentral.org/2004/10/namespace-ont";

	private static Logger      _logger = Logger.getLogger(KbRdfSource.class.getName());

	private Config             _config              = null;
	private String             _dir                 = null;
	private KbInstance         _kb                  = null;
	private boolean            _isWritable          = true;
	protected SyncSail         _syncSail            = null;
	private boolean            _useTreeOptimization = false;

	protected Config getConfig()
	{
		return _config;
	}

	protected String getDirectory()
	{
		return _dir;
	}

	public KbInstance getKb()
	{
		return _kb;
	}

	protected void openKb(String dir, boolean writeable) throws Throwable
	{
		_isWritable = writeable;
		_config = Config.readFromFile();
		_config.m_kbDirectoryPath = dir;
		_config.m_readOnly = !_isWritable;
		_kb = new KbInstance(_config);
	}

	public KbRdfSource()
	{
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		_useTreeOptimization  = "tree".equals(configParams.get(OPTIMIZATION_TYPE_KEY));
		_dir = (String) configParams.get(DATA_DIR_KEY);
		if (_dir == null)
		{
			throw new SailInitializationException("Missing parameter: dir");
		}
		try
		{
			openKb(_dir, true);
		}
		catch (Throwable t)
		{
			throw new SailInitializationException(t);
		}
	}

	@Override
	public void shutDown()
	{
		_kb.finalize();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public NamespaceIterator getNamespaces()
	{
		KbUri namespacePrefix = KbUri.get(_kb, NAMESPACE_ONT + "#prefix");
		if (namespacePrefix == null)
		{
			return new MemNamespaceIterator(new ArrayList());
		}
		else
		{
			return new KbNamespaceIterator(
				getStatements(null, namespacePrefix, null));
		}
	}

	long toLong(Object object, boolean createIfMissing)
	{
		if (object instanceof KbValue)
		{
			return ((KbValue) object).getIndex(_kb);
		}
		else if (object instanceof URI)
		{
			return _kb.uriToRsrcId(((URI) object).getURI(), false, createIfMissing);
		}
		else if (object instanceof Literal)
		{
			return _kb.uriToRsrcId(((Literal) object).getLabel(), true, createIfMissing);
		}
		else if (object instanceof BNode)
		{
			return _kb.uriToRsrcId(((BNode) object).getID(), false, createIfMissing);
		}
		else
		{
			System.err.println("unhandled toLong for " + object.getClass() + " " + object);
			return -1L;
		}
	}

	@Override
	public StatementIterator getStatements(Resource subj, URI pred, Value obj)
	{
		return getStatements(subj, pred, obj, false);
	}

	@SuppressWarnings("resource")
	public StatementIterator getStatements(Resource subj, URI pred, Value obj, boolean skipInferred)
	{
		long subId = KbInstance.NULL_RSRC_ID;
		long predId = KbInstance.NULL_RSRC_ID;
		long objId = KbInstance.NULL_RSRC_ID;
		boolean allRsrcInKb = true;
		if (subj != null)
		{
			subId = toLong(subj, false);
			allRsrcInKb = allRsrcInKb && (subId != KbInstance.NULL_RSRC_ID);
		}
		if (pred != null)
		{
			predId = toLong(pred, false);
			allRsrcInKb = allRsrcInKb && (predId != KbInstance.NULL_RSRC_ID);
		}
		if (obj != null)
		{
			objId = toLong(obj, false);
			allRsrcInKb = allRsrcInKb && (objId != KbInstance.NULL_RSRC_ID);
		}

		if (!allRsrcInKb)
		{
			// If any of the resource id's came back null, give up now, because
			// there's clearly nothing to return.
			return new EmptyStatementIterator();
		}
		else
		{
			int flags = KbInstance.SKIP_DELETED_STMT_ITER_FLAG;
			if (skipInferred)
			{
				flags |= KbInstance.SKIP_INFERRED_STMT_ITER_FLAG;
			}
			if (obj != null)
			{
				flags |= (obj instanceof Literal)
					? KbInstance.SKIP_NON_LITERAL_STMT_ITER_FLAG
						: KbInstance.SKIP_LITERAL_STMT_ITER_FLAG;
			}
			StmtIterator iter = _kb.find(subId, predId, objId, flags);
			return new KbStatementIterator(_kb, _syncSail, iter);
		}
	}

	@Override
	public boolean hasStatement(Resource subj, URI pred, Value obj)
	{
		// optimize later? use counts?
		boolean retval;
		StatementIterator iterator = getStatements(subj, pred, obj);
		try
		{
			retval = iterator.hasNext();
		}
		finally
		{
			iterator.close();
		}
		return retval;
	}

	protected long checkVar(long min, Var var, int position)
	{
		if (var.hasValue())
		{
			Value value1 = var.getValue();
			KbValue value = null;
			// XXX To unmask the Parliament access violation of bug #115,
			// change this line to check value instead of value1. See the
			// bug report in bugzilla for a detailed explanation of what
			// the real issue is and why I believe this hides it.
			if (value1 instanceof KbValue)
			{
				value = (KbValue) value1;
			}
			else if (value1 instanceof URI)
			{
				value = KbUri.get(_kb, ((URI) value1).getURI());
			}
			else if (value1 instanceof Literal)
			{
				value = KbLiteral.get(_kb, ((Literal) value1)
					.getLabel());
			}
			else
			{
				throw new RuntimeException("KbRdfSource.checkVar of "
					+ value1.getClass() + " " + value1);
			}
			if (value == null)
			{
				// Nothing came back from the KB, so no results.
				return 0;
			}
			var.setValue(value);
			long count = Long.MAX_VALUE;
			switch (position)
			{
			case 1:
				count = _kb.subjectCount(value.getIndex(_kb));
				break;
			case 2:
				count = _kb.predicateCount(value.getIndex(_kb));
				break;
			case 3:
				count = _kb.objectCount(value.getIndex(_kb));
				break;
			}
			if (count < min)
			{
				return count;
			}
		}
		return min;
	}

	private static class TriplePatternCount
	{
		TriplePattern _triplePattern;
		long          _count;
		long          _estimate;
		@SuppressWarnings("unused")
		List<Var>     _unboundVariables = new ArrayList<>();

		TriplePatternCount(TriplePattern triplePattern, long count)
		{
			_triplePattern = triplePattern;
			_count = count;
			_estimate = 0;
			_unboundVariables = getRealVariables(triplePattern);
		}

		@Override
		public String toString()
		{
			return _triplePattern.toString() + "Count: " + _count + " Estimate: "
				+ _estimate;
		}
	}

	public static List<Var> getRealVariables(PathExpression pe)
	{
		List<Var> vars = new ArrayList<>();
		pe.getVariables(vars);
		ListIterator<Var> iterator = vars.listIterator();
		while (iterator.hasNext())
		{
			Var var = iterator.next();
			if (var.hasValue())
			{
				iterator.remove();
			}
		}
		return vars;
	}

	/**
	 * Re-order all TriplePatterns based on Parliament counts.
	 */
	protected void orderByCounts(Query query)
	{
		if (query instanceof GraphPatternQuery)
		{
			GraphPatternQuery gpquery = (GraphPatternQuery) query;
			GraphPattern graphPattern = gpquery.getGraphPattern();
			if (!_useTreeOptimization){
				orderByCounts(graphPattern, new ArrayList<Var>(), 1);
			}else{
				treeWidthOrderByCounts(graphPattern, new ArrayList<Constraint>(), new ArrayList<Var>());
			}
		}
		else if (query instanceof SetOperator)
		{
			SetOperator operator = (SetOperator) query;
			orderByCounts(operator.getLeftArg());
			orderByCounts(operator.getRightArg());
		}
	}


	@SuppressWarnings("rawtypes")
	protected void treeWidthOrderByCounts(GraphPattern graphPattern, List<Constraint> fixedConstraints, List<Var> boundVariables){

		List<Constraint> constraints = new ArrayList<>();
		List<TriplePattern> triplePatterns = new ArrayList<>();
		List<BooleanExpr> booleanExpressions = new ArrayList<>();
		List<Object> other = new ArrayList<>();

		//build constraints and count variables, build other expressions into list
		List<Var> variables = new ArrayList<>();

		for (int i=0; i<graphPattern.getExpressions().size(); i++){
			Object o = graphPattern.getExpressions().get(i);
			if (o instanceof TriplePattern){
				TriplePattern triplePattern = (TriplePattern)o;
				long min = getTripleMinimum(triplePattern);
				Constraint constraint = new Constraint();
				constraint.setMaximumProduct(min);
				List<Var> tripleVars = getRealVariables(triplePattern);
				for (Var var : tripleVars){
					if (!variables.contains(var)){
						variables.add(var);
					}
					constraint.addVariable(variables.indexOf(var));
				}
				constraints.add(constraint);
				triplePatterns.add(triplePattern);
			}else if (o instanceof BooleanExpr){
				booleanExpressions.add((BooleanExpr) o);
			}else{
				other.add(o);
			}
		}

		//Create tree optimizer
		TreeWidthQueryOptimizer optimizer = new TreeWidthQueryOptimizer(fixedConstraints, constraints, variables.size());


		//Use tree optimizer
		List<Integer> optimizeResult = optimizer.optimizeConstraints();

		//order expressions, filling in booleans after they're bound
		List<Object> result = new ArrayList<>();
		for (int i=0; i<optimizeResult.size(); i++){
			TriplePattern triplePattern = triplePatterns.get(optimizeResult.get(i));
			boundVariables.addAll(getRealVariables(triplePattern));
			result.add(triplePattern);
			for (int j = 0; j < booleanExpressions.size(); j++) {
				if (variablesAreBound(booleanExpressions.get(j), boundVariables)) {
					result.add(booleanExpressions.get(j));
					booleanExpressions.remove(j);
					j--;
				}
			}
		}

		result.addAll(other);
		result.addAll(booleanExpressions);

		// Recurse
		Iterator childLists = graphPattern.getOptionals().iterator();
		while (childLists.hasNext()) {

			List<Var> boundVarsCopy = new ArrayList<>();
			boundVarsCopy.addAll(boundVariables);
			List<Constraint> newFixedConstraints = new ArrayList<>();
			newFixedConstraints.addAll(fixedConstraints);
			newFixedConstraints.addAll(constraints);
			treeWidthOrderByCounts((GraphPattern) childLists.next(), fixedConstraints, boundVarsCopy);

		}

	}


	@SuppressWarnings("rawtypes")
	protected void orderByCounts(GraphPattern graphPattern,
		List<Var> boundVariables, long currentResultSetEstimate)
	{
		// compute counts based on constant values
		List expressions = graphPattern.getExpressions();
		List<TriplePatternCount> triplePatterns = new ArrayList<>();
		List<BooleanExpr> booleanExpressions = new ArrayList<>();
		List<Object> other = new ArrayList<>();

		if (_logger.isDebugEnabled())
		{
			_logger.debug("Beginning Count Outputs");
		}
		for (Object pe : expressions)
		{
			if (pe instanceof TriplePattern)
			{
				TriplePattern tp = (TriplePattern) pe;
				long min = getTripleMinimum(tp);

				TriplePatternCount tpc = new TriplePatternCount(tp, min);
				triplePatterns.add(tpc);
				if (_logger.isDebugEnabled())
				{
					_logger.debug("TripleCount: " + tpc);
				}
			}
			else if (pe instanceof BooleanExpr)
			{
				booleanExpressions.add((BooleanExpr) pe);
			}
			else
			{
				other.add(pe);
			}
		}
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Ending Count Outputs");
		}

		long time = System.currentTimeMillis();
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Beginning Graph Ordering");
		}
		OrderExpressionResult orderExpressionResult = orderExpressions(
			triplePatterns, booleanExpressions, other, boundVariables,
			currentResultSetEstimate);
		graphPattern.setExpressions(orderExpressionResult.getExpressionList());

		if (_logger.isDebugEnabled())
		{
			_logger.debug("Ending Graph Ordering, took "
				+ (System.currentTimeMillis() - time) + " ms");
		}

		// recurse for sublists
		Iterator childLists = graphPattern.getOptionals().iterator();
		while (childLists.hasNext())
		{
			// *Don't* actually pass in the other stuff for now
			/* DK: 12/4/07.  I'm not really sure what the "for now" in the
			 * above line means.  I can't think of a reason why you would not want to pass
			 * the bindings.
			 */
			List<Var> boundVarsCopy = new ArrayList<>();
			boundVarsCopy.addAll(orderExpressionResult.getBoundVariables());
			orderByCounts((GraphPattern) childLists.next(),
				boundVarsCopy,
				5
				);
			//          orderByCounts((GraphPattern) childLists.next(),
			//             new ArrayList<Var>(), 1);
		}
	}

	private long getTripleMinimum(TriplePattern tp) {
		long min = Long.MAX_VALUE;

		min = checkVar(min, tp.getSubjectVar(), 1);
		min = checkVar(min, tp.getPredicateVar(), 2);
		min = checkVar(min, tp.getObjectVar(), 3);
		return min;
	}

	private static OrderExpressionResult orderExpressions(
		List<TriplePatternCount> triplePatterns, List<BooleanExpr> booleanExpressions,
		List<Object> other, List<Var> boundVariables, long currentResultSetEstimate)
	{
		List<Object> result = new ArrayList<>();

		while (triplePatterns.size() > 0)
		{
			TriplePatternCount tpc = findNextTriplePatternCount(
				triplePatterns, boundVariables, currentResultSetEstimate);
			if (tpc._estimate > 0)
			{
				currentResultSetEstimate = tpc._estimate;
			}
			else
			{
				currentResultSetEstimate = 1;
			}
			boundVariables.addAll(getRealVariables(tpc._triplePattern));
			result.add(tpc._triplePattern);
			triplePatterns.remove(tpc);
			for (int i = 0; i < booleanExpressions.size(); i++)
			{
				if (variablesAreBound(booleanExpressions.get(i), boundVariables))
				{
					result.add(booleanExpressions.get(i));
					booleanExpressions.remove(i);
					i--;
				}
			}
		}

		result.addAll(other);
		result.addAll(booleanExpressions);

		return new OrderExpressionResult(result, currentResultSetEstimate,
			boundVariables);
	}

	private static TriplePatternCount findNextTriplePatternCount(
		List<TriplePatternCount> triplePatterns, List<Var> boundVariables,
		long currentResultSetEstimate)
	{
		TriplePatternCount minTriplePatternCount = triplePatterns.get(0);
		setEstimate(minTriplePatternCount, boundVariables,
			currentResultSetEstimate);
		for (int i = 1; i < triplePatterns.size(); i++)
		{
			TriplePatternCount triplePatternCount = triplePatterns.get(i);
			setEstimate(triplePatternCount, boundVariables,
				currentResultSetEstimate);
			if (triplePatternCount._estimate < minTriplePatternCount._estimate)
			{
				minTriplePatternCount = triplePatternCount;
			}
		}
		return minTriplePatternCount;
	}

	private static void setEstimate(TriplePatternCount tpc, List<Var> boundVariables,
		long currentResultSetEstimate)
	{
		if (sharesVariables(tpc, boundVariables))
		{
			tpc._estimate = tpc._count;
		}
		else
		{
			tpc._estimate = tpc._count * currentResultSetEstimate;
			if ((tpc._count > tpc._estimate || currentResultSetEstimate > tpc._estimate)
				&& tpc._count != 0 && currentResultSetEstimate != 0)
			{
				tpc._estimate = Long.MAX_VALUE;
			}
		}
	}

	private static boolean sharesVariables(TriplePatternCount tpc,
		List<Var> boundVariables)
	{
		boolean result = false;
		List<Var> tripleVariables = getRealVariables(tpc._triplePattern);
		for (Var var : tripleVariables)
		{
			if (boundVariables.contains(var))
			{
				result = true;
				break;
			}
		}
		return result;
	}

	private static boolean variablesAreBound(BooleanExpr expr, List<Var> boundVariables)
	{
		boolean result = true;
		List<?> expressionVariables = new ArrayList<>();
		expr.getVariables(expressionVariables);
		for (Object o : expressionVariables)
		{
			if (!boundVariables.contains(o))
			{
				result = false;
				break;
			}
		}
		return result;
	}

	@Override
	public Query optimizeQuery(Query query)
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Query Pre-Optimization:\n" + query);
		}

		// apply the default optimizations
		QueryOptimizer.optimizeQuery(query);
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Query Post 'Default'-Optimization:\n" + query);
		}

		// order by counts
		orderByCounts(query);
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Query Post Parliament-Optimization:\n" + query);
		}

		return query;
	}

	@Override
	public ValueFactory getValueFactory()
	{
		return this;
	}

	@Override
	public BNode createBNode()
	{
		if (_syncSail == null || _syncSail.isWriting())
		{
			return KbBNode.create(_kb);
		}
		else
		{
			throw new UnimplementedException();
		}
	}

	@Override
	public BNode createBNode(String nodeId)
	{
		if (nodeId.startsWith("node"))
		{
			return new KbBNode(_kb, Long.parseLong(nodeId.substring(4)));
		}
		else
		{
			throw new UnimplementedException();
		}
	}

	@Override
	public Literal createLiteral(String value)
	{
		if (_syncSail == null || _syncSail.isWriting())
		{
			return KbLiteral.create(_kb, value);
		}
		else
		{
			Literal toReturn = KbLiteral.get(_kb, value);
			if (toReturn == null)
			{
				toReturn = new LiteralImpl(value);
			}
			return toReturn;
		}
	}

	@Override
	public Literal createLiteral(String value, String language)
	{
		if (_syncSail == null || _syncSail.isWriting())
		{
			return KbLiteral.create(_kb, value);
		}
		else
		{
			Literal toReturn = KbLiteral.get(_kb, value);
			if (toReturn == null)
			{
				toReturn = new LiteralImpl(value, language);
			}
			return toReturn;
		}
	}

	@Override
	public Literal createLiteral(String value, URI datatype)
	{
		if (_syncSail == null || _syncSail.isWriting())
		{
			return KbLiteral.create(_kb, value);
		}
		else
		{
			Literal toReturn = KbLiteral.get(_kb, value);
			if (toReturn == null)
			{
				toReturn = new LiteralImpl(value, datatype);
			}
			return toReturn;
		}
	}

	@Override
	public Statement createStatement(Resource subject, URI predicate, Value object)
	{
		throw new UnimplementedException();
	}

	@Override
	public URI createURI(String uri)
	{
		if (_syncSail == null || _syncSail.isWriting())
		{
			return KbUri.create(_kb, uri);
		}
		else
		{
			URI toReturn = KbUri.get(_kb, uri);
			if (toReturn == null)
			{
				toReturn = new URIImpl(uri);
			}
			return toReturn;
		}
	}

	@Override
	public URI createURI(String namespace, String localName)
	{
		if (_syncSail == null || _syncSail.isWriting())
		{
			return KbUri.create(_kb, namespace + localName);
		}
		else
		{
			URI toReturn = KbUri.get(_kb, namespace + localName);
			if (toReturn == null)
			{
				toReturn = new URIImpl(namespace, localName);
			}
			return toReturn;
		}
	}

	public void setSyncSail(SyncSail syncSail)
	{
		_syncSail = syncSail;
	}

	private static class OrderExpressionResult
	{
		private List<Object> _expressionList;
		private long         _estimate;
		private List<Var>    _boundVariables;

		public OrderExpressionResult(List<Object> expressionList,
			long estimate, List<Var> boundVariables)
		{
			_expressionList = expressionList;
			_estimate = estimate;
			_boundVariables = boundVariables;
		}

		public List<Var> getBoundVariables()
		{
			return _boundVariables;
		}

		@SuppressWarnings("unused")
		public long getEstimate()
		{
			return _estimate;
		}

		public List<Object> getExpressionList()
		{
			return _expressionList;
		}
	}
}
