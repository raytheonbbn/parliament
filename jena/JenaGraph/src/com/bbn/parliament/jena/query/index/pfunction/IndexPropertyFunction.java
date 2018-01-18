package com.bbn.parliament.jena.query.index.pfunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.QueryableIndex;
import com.bbn.parliament.jena.query.SolverUtil;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactory;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryHelper;
import com.bbn.parliament.jena.query.optimize.KbOptimize;
import com.bbn.parliament.jena.query.optimize.TransformIndexPropertyFunction;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternFactory;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternPropertyFunction;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionEval;
import com.hp.hpl.jena.sparql.util.IterLib;

/**
 * Abstract base class for index property functions. IndexPropertyFunctions
 * extend the standard ARQ <code>PropertyFunctionEval</code> property function
 * class to allow the index to interact with the query operands.
 * <br><br>
 * Instances need to be registered with the
 * <code>com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry</code>. This
 * is typically done by the {@link Index#register(Graph, Node)} method when the
 * {@link IndexManager} registers the <code>Index</code>
 * <br><br>
 * The {@link KbOptimize} algebra optimizer rewrites the query such that all
 * triples associated with the <code>IndexPropertyFunction</code> are given to
 * the instance as a pattern. This allows the property function to see the
 * complete context of it's input and output.
 *
 * @author rbattle
 *
 * @param <T> the type of object that is indexed.
 *
 * @see Index
 * @see IndexPropertyFunctionFactory
 * @see KbOptimize
 * @see TransformIndexPropertyFunction
 */
public abstract class IndexPropertyFunction<T>
extends PropertyFunctionEval {

	protected static List<Node> getNodes(PropFuncArg arg, Binding binding) {
		List<Node> nodes = new ArrayList<>();
		if (arg.isList()) {
			for (Node n : arg.getArgList()) {
				if (n.isVariable()) {
					Var var = Var.alloc(n);
					Node node = binding.get(var);
					if (null == node) {
						nodes.add(n);
					} else {
						nodes.add(node);
					}
				} else if (n.isConcrete()) {
					nodes.add(n);
				}
			}
		} else {
			Node n = arg.getArg();
			if (n.isVariable()) {
				Var var = Var.alloc(n);
				Node node = binding.get(var);
				if (null == node) {
					nodes.add(n);
				} else {
					nodes.add(node);
				}
			} else if (n.isConcrete()) {
				nodes.add(n);
			}
		}
		return nodes;
	}

	protected OperandFactory<T> operandFactory;
	protected BasicPattern pattern;
	protected QueryableIndex<T> index;

	private final Class<? extends QueryableIndex<T>> indexClass;
	//private final Class<? extends OperandFactory<T>> operandFactoryClass;

	/**
	 * Create a new instance for the given index type and operand factory. This defaults to
	 * accepting both lists and single arguments as the subject and object.
	 *
	 * @param indexClass the type of index.
	 * @param operandFactoryClass the operand factory.
	 */
	public IndexPropertyFunction(Class<? extends QueryableIndex<T>> indexClass, Class<? extends OperandFactory<T>> operandFactoryClass) {
		this(PropFuncArgType.PF_ARG_EITHER, PropFuncArgType.PF_ARG_EITHER,
			indexClass, operandFactoryClass);
	}

	/**
	 * Create a new instance.
	 *
	 * @param subjArgType the type of subject argument to accept.
	 * @param objFuncArgType the type of object argument to accept.
	 * @param indexClass the type of index.
	 * @param operandFactoryClass the operand factory.
	 */
	public IndexPropertyFunction(PropFuncArgType subjArgType,
		PropFuncArgType objFuncArgType,
		Class<? extends QueryableIndex<T>> indexClass, Class<? extends OperandFactory<T>> operandFactoryClass) {
		super(subjArgType, objFuncArgType);
		this.indexClass = indexClass;
		//this.operandFactoryClass = operandFactoryClass;
		try {
			operandFactory = operandFactoryClass.newInstance();
		} catch (InstantiationException e) {
			throw new QueryBuildException("Could not instantiate operand factory", e) ;
		} catch (IllegalAccessException e) {
			throw new QueryBuildException("Could not access operand factory", e) ;
		}
	}

	public void setPattern(BasicPattern pattern) {
		this.pattern = pattern;
	}

	/**
	 * Answer whether this instance has a pattern of triples.
	 *
	 * @return <code>true</code> if there is a pattern and it is not empty; otherwise
	 *         <code>false</code>.
	 */
	public boolean hasPattern() {
		return (null != pattern && !pattern.isEmpty());
	}

	/** Get the pattern of triples associated with this instance. */
	public BasicPattern getPattern() {
		return pattern;
	}

	/** Get the operand factory. */
	public OperandFactory<T> getOperandFactory() {
		return operandFactory;
	}

	/** Get the index. */
	protected QueryableIndex<T> getIndex() {
		return index;
	}

	/** {@inheritDoc} */
	@Override
	public void build(PropFuncArg argSubject, Node predicate,
		PropFuncArg argObject, ExecutionContext context) {
		super.build(argSubject, predicate, argObject, context);
		// get index
		Graph graph = context.getActiveGraph();
		List<Index<?>> indexes = IndexManager.getInstance().getIndexes(graph);
		for (Index<?> i : indexes) {
			if (indexClass.isInstance(i)) {
				index = indexClass.cast(i);
				break;
			}
		}
		if (null != index) {
			operandFactory.setIndex(index);
		}
	}

	/** {@inheritDoc}. Marked as final so nothing can override this. */
	@Override
	public final QueryIterator exec(QueryIterator input, PropFuncArg argSubject,
		Node predicate, PropFuncArg argObject, ExecutionContext execCxt) {
		return super.exec(input, argSubject, predicate, argObject, execCxt);
	}

	/** {@inheritDoc} */
	@Override
	public final QueryIterator execEvaluated(Binding binding,
		PropFuncArg argSubject, Node predicate, PropFuncArg argObject,
		ExecutionContext context) {

		if (null == index) {
			// no index found
			return IterLib.noResults(context);
		}
		Graph graph = context.getActiveGraph();

		// update subjects/objects
		List<Node> subjects = getNodes(argSubject, binding);
		List<Node> objects = getNodes(argObject, binding);
		Map<Node, Operand<T>> operands = OperandFactoryHelper
			.getOperands(operandFactory, subjects, objects, binding, pattern, true);

		if (hasPattern()) {
			// analyze pattern to see what should be handled by indexes and in what order

			List<Triple> usedTriples = new ArrayList<>();

			for (Operand<T> op : operands.values()) {
				usedTriples.addAll(op.getTriples());
			}
			List<Triple> remainingTriples = new ArrayList<>();
			remainingTriples.addAll(pattern.getList());
			remainingTriples.removeAll(usedTriples);
			BasicPattern remainingPattern = BasicPattern.wrap(remainingTriples);

			IndexSubPatternPropertyFunction<T> funcPattern = IndexSubPatternFactory
				.create(this, predicate, subjects, objects, operands);

			return SolverUtil.solve(funcPattern, remainingPattern, binding,
				context, graph,
				SolverUtil.DEFAULT_SOLVER_EXECUTOR);
		}
		return execBinding(binding, subjects, objects, operands, context);
	}

	/**
	 * Execute a binding with the given set of inputs.
	 *
	 * @param binding the current binding.
	 * @param subjects the subjects of the property function.
	 * @param objects the objects of the property function.
	 * @param operands the current operand map.
	 * @param context the execution context.
	 * @return an iterator of binding results.
	 */
	public abstract QueryIterator execBinding(Binding binding,
		List<Node> subjects, List<Node> objects,
		Map<Node, Operand<T>> operands, ExecutionContext context);
}
