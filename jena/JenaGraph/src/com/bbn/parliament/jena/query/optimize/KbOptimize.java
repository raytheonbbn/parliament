package com.bbn.parliament.jena.query.optimize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.query.index.pfunction.algebra.OpIndexPropFunc;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpWalker;
import com.hp.hpl.jena.sparql.algebra.Transform;
import com.hp.hpl.jena.sparql.algebra.Transformer;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.optimize.OpVisitorExprPrepare;
import com.hp.hpl.jena.sparql.algebra.optimize.Optimize;
import com.hp.hpl.jena.sparql.algebra.optimize.Rewrite;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformExpandOneOf;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformFilterConjunction;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformFilterDisjunction;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformFilterEquality;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformJoinStrategy;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformPathFlattern;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformPropertyFunction;
import com.hp.hpl.jena.sparql.util.Context;

/**
 * An algebra optimizer for Parliament. The <code>KbOptimize</code> is an
 * algebra rewriter that copies the optimizations from the standard
 * {@link Optimize} class and has a few extras to support Parliament specific
 * enhancements. The biggest difference is the handling of property functions
 * for accessing indexes.
 * <br><br>
 * The {@link TransformIndexPropertyFunction} transformer is applied before
 * property functions are created in order to reorder triples so that all the
 * triples that the index property function needs are bound before it is called.
 * <br><br>
 * It is run again after property functions are created to transform any
 * {@link OpPropFunc}s that refer to index property functions into
 * {@link OpIndexPropFunc}.
 *
 * @author rbattle
 */
public class KbOptimize implements Rewrite {
	private static Logger log = LoggerFactory.getLogger(KbOptimize.class);

	public static Optimize.RewriterFactory factory = new Optimize.RewriterFactory() {

		@Override
		public Rewrite create(Context context) {
			return new KbOptimize(context);
		}
	};

	public static void register() {
		Optimize.setFactory(factory);
	}

	protected Context context;

	protected boolean arqOptimization = false;

	public KbOptimize(Context context) {
		this(context, true);
	}

	public KbOptimize(Context context, boolean arqOptimization) {
		this.context = context;
		this.arqOptimization = arqOptimization;
		// this.context.set(ARQ.optFilterPlacement, false);
	}

	@Override
	public Op rewrite(Op rewrite) {

		Op op = rewrite;
		// taken from ARQ optimize
		// Prepare expressions.
		OpWalker.walk(op, new OpVisitorExprPrepare(context));

		// reorder triples so that they are all bound for IndexPropertyFunctions
		if (IndexManager.getInstance().size() > 0) {
			op = apply("Index Property Functions",
				new TransformIndexPropertyFunction(context), op);
		}

		// Need to allow subsystems to play with this list.
		if (context.isTrueOrUndef(ARQ.propertyFunctions))
			op = apply("Property Functions",
				new TransformPropertyFunction(context), op);

		// need to run again to turn property functions into
		// IndexPropertyFunctions
		if (IndexManager.getInstance().size() > 0) {
			op = apply("Index Property Functions",
				new TransformIndexPropertyFunction(context), op);
		}

		if (context.isTrueOrUndef(ARQ.optFilterConjunction))
			op = apply("filter conjunctions to ExprLists",
				new TransformFilterConjunction(), op);

		if (context.isTrueOrUndef(ARQ.optFilterExpandOneOf))
			op = apply("Break up IN and NOT IN", new TransformExpandOneOf(), op);

		// Find joins/leftJoin that can be done by index joins (generally
		// preferred as fixed memory overhead).
		op = apply("Join strategy", new TransformJoinStrategy(), op);

		// TODO Improve filter placement to go through assigns that have
		// no effect.  Do this before filter placement and other sequence
		// generating transformations or improve to place in a sequence.

		if (context.isTrueOrUndef(ARQ.optFilterEquality)) {
			op = apply("Filter Equality", new TransformFilterEquality(), op);
		}

		if (context.isTrueOrUndef(ARQ.optFilterDisjunction))
			op = apply("Filter Disjunction", new TransformFilterDisjunction(), op);

		if (context.isTrueOrUndef(ARQ.optFilterPlacement)) {
			// This can be done too early (breaks up BGPs).
			op = apply("Filter Placement",
				new TransformFilterPlacementWithOptional(), op);
		}

		op = apply("Path flattening", new TransformPathFlattern(), op);
		// Mark

		return op;
	}

	public static Op apply(String label, Transform transform, Op op) {
		Op op2 = Transformer.transformSkipService(transform, op);

		final boolean debug = false;

		if (debug) {
			log.info("Transform: " + label);
			if (op == op2) {
				log.info("No change (==)");
				return op2;
			}

			if (op.equals(op2)) {
				log.info("No change (equals)");
				return op2;
			}
			log.info("\n" + op.toString());
			log.info("\n" + op2.toString());
		}
		return op2;
	}
}
