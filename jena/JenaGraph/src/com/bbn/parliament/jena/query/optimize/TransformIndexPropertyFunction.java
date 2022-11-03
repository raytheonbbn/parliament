package com.bbn.parliament.jena.query.optimize;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.sparql.util.Context;

import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.query.index.pfunction.algebra.IndexPropertyFunctionGenerator;

public class TransformIndexPropertyFunction extends TransformCopy {
	private Context context;

	public TransformIndexPropertyFunction(Context context) {
		this.context = context;
	}

	/** {@inheritDoc} */
	@Override
	public Op transform(OpTriple opTriple) {
		return transform(opTriple.asBGP());
	}

	/** {@inheritDoc} */
	@Override
	public Op transform(OpBGP opBGP) {
		// no indexes
		if (IndexManager.getInstance().size() == 0) {
			return opBGP;
		}

		// no property functions registered
		PropertyFunctionRegistry registry = PropertyFunctionRegistry.get(context);
		if (null == registry) {
			return opBGP;
		}

		return IndexPropertyFunctionGenerator.buildIndexPropertyFunctions(opBGP, context);
	}
}
