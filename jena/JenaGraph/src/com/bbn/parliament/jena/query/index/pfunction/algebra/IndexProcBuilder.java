package com.bbn.parliament.jena.query.index.pfunction.algebra;

import org.openjena.atlas.io.IndentedWriter;

import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunctionFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;
import com.hp.hpl.jena.sparql.procedure.Procedure;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.FmtUtils;
import com.hp.hpl.jena.sparql.util.PrintSerializableBase;

/**
 * A procedure builder.  This is modeled on ARQ's
 *
 * @author rbattle
 */
public class IndexProcBuilder {
	static public PropertyFunctionRegistry choosePropFuncRegistry(Context context) {
		PropertyFunctionRegistry registry = PropertyFunctionRegistry.get(context);
		if (registry == null)
			registry = PropertyFunctionRegistry.get();
		return registry;
	}

	public static Procedure build(Node procId, PropFuncArg subjArg,
		PropFuncArg objArg, BasicPattern pattern, ExecutionContext execCxt) {
		PropertyFunctionRegistry registry = choosePropFuncRegistry(execCxt
			.getContext());
		IndexPropertyFunctionFactory<?> factory = (IndexPropertyFunctionFactory<?>) registry
			.get(procId.getURI());
		return buildProcedure(factory, procId, subjArg, objArg, pattern, execCxt);
	}

	private static <T> ProcedureIndexPF<T> buildProcedure(
		IndexPropertyFunctionFactory<T> factory, Node procId,
		PropFuncArg subjArg, PropFuncArg objArg, BasicPattern pattern,
		ExecutionContext execCxt) {
		IndexPropertyFunction<T> pf = factory.create(procId.getURI());
		// sets index
		if (!(null == pattern || pattern.isEmpty())) {
			pf.setPattern(pattern);
		}
		pf.build(subjArg, procId, objArg, execCxt);
		return new ProcedureIndexPF<>(pf, subjArg, procId, objArg);
	}

	public static class ProcedureIndexPF<T> extends PrintSerializableBase
	implements Procedure {
		private IndexPropertyFunction<T> propFunc;
		private PropFuncArg subjArg;
		private PropFuncArg objArg;
		private Node pfNode;

		public ProcedureIndexPF(IndexPropertyFunction<T> propFunc, PropFuncArg subjArg, Node pfNode, PropFuncArg objArg) {
			this.propFunc = propFunc;
			this.subjArg = subjArg;
			this.pfNode = pfNode;
			this.objArg = objArg;
		}

		@Override
		public QueryIterator proc(QueryIterator input, ExecutionContext context) {
			return propFunc.exec(input, subjArg, pfNode, objArg, context);
		}

		@Override
		public void build(Node procId, ExprList args, ExecutionContext execCxt) {
		}

		@Override
		public void output(IndentedWriter out, SerializationContext sCxt) {
			out.print("ProcedureIndexPF [" + FmtUtils.stringForNode(pfNode, sCxt) + "]");
			out.print("[");
			subjArg.output(out, sCxt);
			out.print("][");
			objArg.output(out, sCxt);
			out.print("]");
			out.println();
		}
	}
}
