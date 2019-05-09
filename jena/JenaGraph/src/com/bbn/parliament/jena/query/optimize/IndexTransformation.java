package com.bbn.parliament.jena.query.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.query.index.GraphSubPattern;
import com.bbn.parliament.jena.query.index.IndexPattern;
import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.bbn.parliament.jena.query.index.IndexPatternQuerierManager;
import com.bbn.parliament.jena.query.optimize.pattern.EstimablePattern;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPattern;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternBGP;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternFactory;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternPropertyFunction;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderProc;

public class IndexTransformation extends AbstractGraphReorderTransformation {
	static Logger log = LoggerFactory.getLogger(IndexTransformation.class);

	static IndexSubPatternBGP createSubPattern(
		IndexPatternQuerier querier, BasicPattern processed, Graph graph) {
		if (null == querier) {
			return IndexSubPatternFactory.create(graph);
		}
		return IndexSubPatternFactory.create(querier, processed);
	}

	static IndexSubPatternBGP createSubPattern(
		IndexPatternQuerier querier, Graph graph) {
		if (null == querier) {
			return IndexSubPatternFactory.create(graph);
		}
		return IndexSubPatternFactory.create(querier);
	}

	List<Index<?>> indexes;
	IndexSubPatternPropertyFunction<?> propertyPattern;
	public IndexTransformation(Graph graph, List<Index<?>> indexes) {
		this(graph, indexes, null);
	}

	public IndexTransformation(Graph graph, List<Index<?>> indexes, IndexSubPatternPropertyFunction<?> propertyPattern) {
		super(graph);
		this.indexes = indexes;
		this.propertyPattern = propertyPattern;
	}

	@Override
	public ReorderProc reorderIndexes(BasicPattern pattern) {
		return new IndexReorderProc();
	}

	private class IndexReorderProc implements ReorderProc {

		public IndexReorderProc() {
		}

		@Override
		public IndexPattern reorder(BasicPattern bgp) {
			List<Triple> triples = new ArrayList<>(bgp.getList());
			List<IndexSubPatternBGP> subPatterns = new ArrayList<>();

			if (bgp.size() > 0) {
				for (Index<?> index :indexes) {
					IndexPatternQuerier querier = IndexPatternQuerierManager.getInstance().get(index);
					if (null == querier) {
						continue;
					}
					BasicPattern processed = querier.examine(bgp);
					if (processed.size() > 0) {
						triples.removeAll(processed.getList());
						subPatterns.add(createSubPattern(querier, processed, graph));
					}
				}

				BasicPattern remaining = BasicPattern.wrap(triples);
				// remaining triples go to KbGraphIndex graph
				if (!remaining.isEmpty()) {
					subPatterns.add(new GraphSubPattern(graph, remaining));
				}
				subPatterns = splitBySubgraph(subPatterns);
			}

			List<IndexSubPattern> allWithProperty = new ArrayList<>(subPatterns);

			if (null != propertyPattern) {
				allWithProperty.add(propertyPattern);
			}

			// don't reorder if only 1 thing
			if (allWithProperty.size() > 1) {
				allWithProperty = reorder(allWithProperty);
			}

			IndexPattern result = new IndexPattern();
			for (IndexSubPattern subPattern : allWithProperty) {
				result.addAll(subPattern);
			}
			return result;
		}

		private List<IndexSubPatternBGP> splitBySubgraph(
			List<IndexSubPatternBGP> subPatterns) {
			List<IndexSubPatternBGP> result = new ArrayList<>();
			for (int i = 0; i < subPatterns.size(); i++) {
				IndexSubPatternBGP subPattern = subPatterns.get(i);
				IndexPatternQuerier querier = subPattern.getQuerier();
				List<List<String>> partitions = partitionElements(subPattern);

				if (partitions.size() == 1) {
					result.add(subPattern);
				} else {
					List<Triple> triples = subPattern.getList();
					boolean[] usedTriples = new boolean[triples.size()];

					for (int j = 0; j < partitions.size(); j++) {
						List<String> partition = partitions.get(j);
						IndexSubPatternBGP newSubquery = createSubPattern(querier, graph);
						for (int k = 0; k < triples.size(); k++) {
							Triple triple = triples.get(k);
							if (isLinkedTo(partition, getTripleVars(triple))) {
								newSubquery.add(triple);
								usedTriples[k] = true;
							}
						}
						result.add(newSubquery);
					}

					// create sub pattern out of unused triples
					for (int j = 0; j < usedTriples.length; j++) {
						if (!usedTriples[j]) {
							IndexSubPatternBGP newSubquery = createSubPattern(querier, graph);
							newSubquery.add(triples.get(j));
							result.add(newSubquery);
						}
					}
				}
			}

			return result;
		}

		private List<IndexSubPattern> reorder(List<IndexSubPattern> unsorted) {
			if (log.isTraceEnabled()) {
				log.trace("reorder:  given IndexSubPattern list:");
				for (IndexSubPattern p : unsorted) {
					log.trace("reorder:  next index sub-pattern{}{}",
						System.getProperty("line.separator"), p);
				}
			}

			Map<IndexSubPattern, Long> indexSelectivity = new HashMap<>();

			List<IndexSubPattern> patterns = new ArrayList<>(unsorted);
			List<IndexSubPattern> reordered = new ArrayList<>(patterns.size());

			while (!patterns.isEmpty()) {
				log.trace("reorder:  Top of main loop");
				long minSelectivity = Long.MAX_VALUE;
				int minSelectivityIndex = 0;
				for (int i = 0; i < patterns.size(); i++) {
					IndexSubPattern subPattern = patterns.get(i);

					Long tmpSel = indexSelectivity.get(subPattern);

					long selectivity;
					if (tmpSel == null) {
						selectivity = (subPattern instanceof EstimablePattern)
							? ((EstimablePattern) subPattern).estimate()
								: Long.MAX_VALUE;
							indexSelectivity.put(subPattern, selectivity);
					} else {
						selectivity = tmpSel.longValue();
					}
					log.trace("reorder:  selectivity {} for IndexSubPattern {}",
						selectivity, subPattern);
					if (selectivity < minSelectivity) {
						minSelectivityIndex = i;
						minSelectivity = selectivity;
					}
				}
				log.trace("reorder:  minSelectivityIndex = {}", minSelectivityIndex);
				IndexSubPattern minSelPat = patterns.remove(minSelectivityIndex);
				reordered.add(minSelPat);
				Set<Node> minSelPatVars = minSelPat.getVariables();
				List<IndexSubPattern> linkedToRemove = new ArrayList<>();
				for (IndexSubPattern toCheck : patterns) {
					Set<Node> toCheckVars = toCheck.getVariables();
					outerVarIteration:
						for (Node n1 : minSelPatVars) {
							for (Node n2 : toCheckVars) {
								if (n1.equals(n2)) {
									reordered.add(toCheck);
									linkedToRemove.add(toCheck);
									break outerVarIteration;
								}
							}
						}
				}
				if (log.isTraceEnabled()) {
					log.trace("reorder:  linked IndexSubPattern list:");
					for (IndexSubPattern p : linkedToRemove) {
						log.trace("reorder:  next index sub-pattern{}{}",
							System.getProperty("line.separator"), p);
					}
				}
				patterns.removeAll(linkedToRemove);
			}
			if (log.isTraceEnabled()) {
				log.trace("reorder:  optimized IndexSubPattern list:");
				for (IndexSubPattern p : reordered) {
					log.trace("reorder:  next index sub-pattern{}{}",
						System.getProperty("line.separator"), p);
				}
			}
			return reordered;
		}
	}

	protected static List<List<String>> partitionElements(
		IndexSubPatternBGP subquery) {
		List<List<String>> result = new ArrayList<>();
		List<Triple> triples = subquery.getList();
		for (int i = 0; i < triples.size(); i++) {
			addToPartitions(result, triples.get(i));
		}
		return result;
	}

	/** Get a list of the variables in a triple. */
	static List<Node> getTripleVars(Triple triple) {
		List<Node> varsInThisTriple = new ArrayList<>();
		if (triple.getSubject().isVariable()) {
			varsInThisTriple.add(triple.getSubject());
		}
		if (triple.getPredicate().isVariable()) {
			varsInThisTriple.add(triple.getPredicate());
		}
		if (triple.getObject().isVariable()) {
			varsInThisTriple.add(triple.getObject());
		}
		return varsInThisTriple;
	}

	/**
	 * Add the variables in the triple to the pre-existing partitions. Must make sure that
	 * newly linked partitions become one partition, etc.
	 *
	 * @param variablePartitions list of current variablePartitions
	 * @param triple the new triple to add variables from
	 * @param dataSource the dataSource for this triple
	 */
	private static void addToPartitions(List<List<String>> variablePartitions,
		Triple triple) {
		List<Node> varsInThisTriple = getTripleVars(triple);
		List<String> foundPartition = null;
		for (int i = 0; i < variablePartitions.size(); i++) {
			List<String> partition = variablePartitions.get(i);
			if (isLinkedTo(partition, varsInThisTriple)) {
				if (foundPartition == null) {
					for (Node var : varsInThisTriple) {
						String varNm = var.getName();
						if (!partition.contains(varNm)) {
							partition.add(varNm);
						}
					}
					foundPartition = partition;
				} else {
					for (int j = 0; j < partition.size(); j++) {
						foundPartition.add(partition.get(j));
					}
					variablePartitions.remove(i);
					i--;
				}
			}
		}
		if (foundPartition == null) {
			List<String> newPartition = new ArrayList<>();
			for (Node var : varsInThisTriple) {
				newPartition.add(var.getName());
			}
			if (newPartition.size() > 0) {
				variablePartitions.add(newPartition);
			}
		}
	}

	/**
	 * Whether or not the triple shares a variable with the partition, for the given data
	 * source.
	 *
	 * @param partition VariablePartition to check
	 * @param varsInThisTriple variables to check against the partition
	 * @param dataSource the datasource for the variables in this triple
	 * @return true if they share a variable in the data source
	 */
	static boolean isLinkedTo(List<String> partition,
		List<Node> varsInThisTriple) {
		for (Node var : varsInThisTriple) {
			if (partition.contains(var.getName())) {
				return true;
			}
		}
		return false;
	}
}
