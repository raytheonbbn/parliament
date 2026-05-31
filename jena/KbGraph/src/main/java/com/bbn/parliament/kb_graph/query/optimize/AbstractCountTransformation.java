package com.bbn.parliament.kb_graph.query.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.query.ReifiedTriple;

public abstract class AbstractCountTransformation extends AbstractKbGraphReorderTransformation {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractCountTransformation.class);

	public AbstractCountTransformation(KbGraph graph) {
		super(graph);
	}

	protected long checkVar(long min, Node node, int position) {
		if (node.isConcrete()) {
			@SuppressWarnings("resource")
			long count = getGraph().getNodeCountInPosition(node, position);
			if (count < min) {
				return count;
			}
		}
		return min;
	}

	protected List<Triple> orderByCounts(List<Triple> triples,
		List<Node> boundVariables, long currentResultSetEstimate,
		boolean useNewOrderingHeuristic) {
		// compute counts based on constant values:
		var tpCounts = calculateTriplePatternCounts(triples);

		long start = System.currentTimeMillis();
		LOG.debug("Beginning Graph Ordering");
		var orderExpressionResult = orderExpressions(tpCounts,
			boundVariables,
			currentResultSetEstimate,
			useNewOrderingHeuristic,
			Integer.MAX_VALUE);

		if (LOG.isDebugEnabled()) {
			long duration = System.currentTimeMillis() - start;
			LOG.debug("Ending Graph Ordering, took {} ms", duration);
			LOG.debug("Result: {}", orderExpressionResult);
		}
		return orderExpressionResult.getExpressionList();
	}

	protected List<TriplePatternCount> calculateTriplePatternCounts(List<Triple> triples) {
		var tpCounts = triples.stream()
			.map(tp -> new TriplePatternCount(tp, getTripleMinimum(tp)))
			.collect(Collectors.toCollection(ArrayList::new));
		if (LOG.isDebugEnabled()) {
			LOG.debug("Beginning Count Outputs");
			tpCounts.forEach(tpc -> LOG.debug("   TripleCount: {}", tpc));
			LOG.debug("Ending Count Outputs");
		}
		return tpCounts;
	}

	protected long getTripleMinimum(Triple tp) {
		long min = Long.MAX_VALUE;

		if (!isPartOfReification(tp)) {
			min = checkVar(min, tp.getSubject(), 1);
			min = checkVar(min, tp.getPredicate(), 2);
			min = checkVar(min, tp.getObject(), 3);
			if (tp instanceof ReifiedTriple rtp) {
				min = checkVar(min, rtp.getName(), 3);
			}
		} else {
			min = checkVar(min, tp.getSubject(), 3);
			if (tp.getPredicate().equals(RDF.Nodes.subject)) {
				min = checkVar(min, tp.getObject(), 1);
			}
			if (tp.getPredicate().equals(RDF.Nodes.predicate)) {
				min = checkVar(min, tp.getObject(), 2);
			}
			if (tp.getPredicate().equals(RDF.Nodes.object)) {
				min = checkVar(min, tp.getObject(), 3);
			}
			//if rdf:type rdf:Statement, don't need to do anything
		}
		return min;
	}

	private static boolean isPartOfReification(Triple tp) {
		if (tp instanceof ReifiedTriple || !tp.getPredicate().isConcrete()) {
			return false;
		}
		var pred = tp.getPredicate();
		return pred.equals(RDF.Nodes.subject)
			|| pred.equals(RDF.Nodes.predicate)
			|| pred.equals(RDF.Nodes.object)
			|| (pred.equals(RDF.Nodes.type)
				&& tp.getObject().equals(RDF.Nodes.Statement));
	}

	protected static OrderExpressionResult orderExpressions(
		List<TriplePatternCount> triplePatterns, List<Node> boundVariables,
		long currentResultSetEstimate, boolean useNewOrderingHeuristic,
		int maxTriples) {
		List<Triple> result = new ArrayList<>();

		long currentEstimate = currentResultSetEstimate;
		int triplesAdded = 0;
		while (triplePatterns.size() > 0 && triplesAdded <= maxTriples) {
			TriplePatternCount tpc = findNextTriplePatternCount(triplePatterns,
				boundVariables,
				currentEstimate,
				useNewOrderingHeuristic);
			currentEstimate = (tpc.estimate > 0) ? tpc.estimate : 1;
			boundVariables.addAll(OptimizeUtil.getVariables(tpc.triple));
			result.add(tpc.triple);
			triplePatterns.remove(tpc);
			++triplesAdded;
		}

		return new OrderExpressionResult(result, currentEstimate, boundVariables);
	}

	private static TriplePatternCount findNextTriplePatternCount(
		List<TriplePatternCount> triplePatterns, List<Node> boundVariables,
		long currentResultSetEstimate, boolean useNewOrderingHeuristic) {
		TriplePatternCount minTriplePatternCount = triplePatterns.get(0);
		if (!useNewOrderingHeuristic) {
			setEstimate(minTriplePatternCount, boundVariables, currentResultSetEstimate);
		} else {
			newSetEstimate(minTriplePatternCount, boundVariables, currentResultSetEstimate);
		}
		for (int i = 1; i < triplePatterns.size(); i++) {
			TriplePatternCount triplePatternCount = triplePatterns.get(i);
			setEstimate(triplePatternCount, boundVariables, currentResultSetEstimate);
			if (triplePatternCount.estimate < minTriplePatternCount.estimate) {
				minTriplePatternCount = triplePatternCount;
			}
		}
		return minTriplePatternCount;
	}

	private static void setEstimate(TriplePatternCount tpc, List<Node> boundVariables,
		long currentResultSetEstimate) {
		if (sharesVariables(tpc, boundVariables)) {
			tpc.estimate = tpc.count;
		} else {
			tpc.estimate = tpc.count * currentResultSetEstimate;
			if ((tpc.count > tpc.estimate || currentResultSetEstimate > tpc.estimate)
				&& tpc.count != 0 && currentResultSetEstimate != 0) {
				tpc.estimate = Long.MAX_VALUE;
			}
		}
	}

	private static boolean sharesVariables(TriplePatternCount tpc,
		List<Node> boundVariables) {
		boolean result = false;
		List<Node> tripleVariables = OptimizeUtil.getVariables(tpc.triple);
		for (Node var : tripleVariables) {
			if (boundVariables.contains(var)) {
				result = true;
				break;
			}
		}
		return result;
	}

	private static void newSetEstimate(TriplePatternCount tpc, List<Node> oldVars,
		long currentResultSetEstimate) {

		List<Node> newVars = OptimizeUtil.getVariables(tpc.triple);

		boolean oldSubsetOfNew = newVars.containsAll(oldVars);
		boolean newSubsetOfOld = oldVars.containsAll(oldVars);
		boolean sharesVars = sharesVariables(tpc, oldVars);

		if (oldVars.size() == 0) {
			tpc.estimate = tpc.count;
		} else if (oldVars.size() == 1 && newSubsetOfOld) {
			tpc.estimate = Math.min(currentResultSetEstimate, tpc.count);
		} else if (newSubsetOfOld) {
			tpc.estimate = currentResultSetEstimate;
		} else if (sharesVars && oldSubsetOfNew) {
			tpc.estimate = Math.max(currentResultSetEstimate, tpc.count);
		} else if (sharesVars) {
			tpc.estimate = tpc.count * currentResultSetEstimate / 2;
			if ((tpc.count > tpc.estimate || currentResultSetEstimate > tpc.estimate)
				&& tpc.count != 0 && currentResultSetEstimate != 0) {
				tpc.estimate = Long.MAX_VALUE;
			}
		} else {
			tpc.estimate = tpc.count * currentResultSetEstimate;
			if ((tpc.count > tpc.estimate || currentResultSetEstimate > tpc.estimate)
				&& tpc.count != 0 && currentResultSetEstimate != 0) {
				tpc.estimate = Long.MAX_VALUE;
			}
		}
	}

	public long estimateSelectivity(BasicPattern pattern) {
		// double size = graph.size();
		// double product = size;
		// for (Triple triple : triples){
		// product *= (getTripleMinimum(triple) / size);
		// }
		// return (int)product;
		List<TriplePatternCount> tpcs = calculateTriplePatternCounts(pattern.getList());
		return orderExpressions(tpcs, new ArrayList<>(), 1, false, 3).getEstimate();
	}
}
