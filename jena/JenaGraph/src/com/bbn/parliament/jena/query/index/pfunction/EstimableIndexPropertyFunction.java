package com.bbn.parliament.jena.query.index.pfunction;

import java.util.List;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.QueryableIndex;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;

/**
 * An {@link IndexPropertyFunction} that estimate how many results it will
 * provide for a given input. This is useful when planning the execution order
 * of a query.
 *
 * @param <T> the type of data that is indexed.
 * @author rbattle
 */
public abstract class EstimableIndexPropertyFunction<T> extends IndexPropertyFunction<T> {
	public EstimableIndexPropertyFunction(Class<? extends QueryableIndex<T>> indexClass,
		Class<? extends OperandFactory<T>> operandFactoryClass) {
		this(PropFuncArgType.PF_ARG_EITHER, PropFuncArgType.PF_ARG_EITHER,
			indexClass, operandFactoryClass);
	}

	public EstimableIndexPropertyFunction(PropFuncArgType subjArgType,
		PropFuncArgType objFuncArgType, Class<? extends QueryableIndex<T>> indexClass,
		Class<? extends OperandFactory<T>> operandFactoryClass) {
		super(subjArgType, objFuncArgType, indexClass, operandFactoryClass);
	}

	/**
	 * Estimate how many results will be returned for the given list of subjects,
	 * objects, and operands.
	 *
	 * @param subjects the subjects of the function.
	 * @param objects the objects of the function.
	 * @param operands the current state of the operands.
	 * @return an upper bound on the number of results.
	 */
	public abstract long estimate(List<Node> subjects, List<Node> objects,
		Map<Node, Operand<T>> operands);
}
