package com.bbn.parliament.jena.query.index.pfunction.algebra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryHelper;
import com.bbn.parliament.jena.query.index.pfunction.EstimableIndexPropertyFunction;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunctionFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.binding.BindingRoot;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionFactory;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.graph.GNode;
import com.hp.hpl.jena.sparql.util.graph.GraphList;

/**
 * A generator for {@link IndexPropertyFunction}s. This is modeled on ARQ's
 * <code>com.hp.hpl.jena.sparql.algebra.PropertyFunctionGenerator</code>.
 *
 * @author rbattle
 *
 * @see com.hp.hpl.jena.sparql.algebra.PropertyFunctionGenerator
 */
public class IndexPropertyFunctionGenerator {

	public static Op buildIndexPropertyFunctions(OpBGP opBGP, Context context) {
		if (opBGP.getPattern().isEmpty())
			return opBGP;
		return compilePattern(opBGP.getPattern(), context);
	}

	private static Op compilePattern(BasicPattern pattern, Context context) {
		// Split into triples and property functions.

		PropertyFunctionRegistry registry = chooseRegistry(context);

		// 1/ Find property functions.
		// Property functions may involve other triples (for list arguments)
		// (but leave the property function triple in-place as a marker)
		// 2/ Find arguments for property functions
		// (but leave the property function triple in-place as a marker)
		// 3/ For remaining triples, put into basic graph patterns,
		// and string together the procedure calls and BGPs.

		List<Triple> propertyFunctionTriples = new ArrayList<>(); // Property functions seen
		BasicPattern triples = new BasicPattern(pattern); // A copy of all triples (mutated later)

		// Find the triples invoking property functions, and those not.
		findPropertyFunctions(pattern, registry, propertyFunctionTriples);

		if (propertyFunctionTriples.size() == 0) {
			// No property functions.
			return new OpBGP(pattern);
		}

		// Map triple => property function instance
		Map<Triple, PropertyFunctionInstance> pfInvocations = new HashMap<>();
		// Removes triples of list arguments. This mutates 'triples'
		findPropertyFunctionArgs(context, triples, propertyFunctionTriples,
			pfInvocations);

		// Now make the OpSequence structure.
		Op op = makeStages(triples, pfInvocations);
		return op;
	}

	private static void findPropertyFunctions(BasicPattern pattern, PropertyFunctionRegistry registry,
		List<Triple> propertyFunctionTriples) {
		// Step 1 : find property functions (if any); collect triples.
		// Not list arg triples at this point.
		for (Triple t : pattern) {
			if (isPropertyFunction(registry, t))
				propertyFunctionTriples.add(t);
		}
	}

	private static void findPropertyFunctionArgs(Context context,
		BasicPattern triples, List<Triple> propertyFunctionTriples,
		Map<Triple, PropertyFunctionInstance> pfInvocations) {
		// Step 2 : for each property function, remove associated triples in list
		// arguments;
		// Leave the propertyFunction triple itself.

		List<Triple> triplesToRemove = new ArrayList<>();
		for (Iterator<Triple> iter = propertyFunctionTriples.iterator(); iter
			.hasNext();) {
			Triple pf = iter.next();
			PropertyFunctionInstance pfi = magicProperty(context, pf, triples, triplesToRemove);
			pfInvocations.put(pf, pfi);
		}
		triples.getList().removeAll(triplesToRemove);
	}

	private static class PropertyFunctionInstance {
		private PropFuncArg subjArgs;
		private PropFuncArg objArgs;
		private BasicPattern dependentTriples;
		private IndexPropertyFunction<?> pf;
		private Triple pfTriple;

		PropertyFunctionInstance(PropFuncArg sArgs, Triple pfTriple,
			PropFuncArg oArgs,
			BasicPattern dependentTriples,
			IndexPropertyFunction<?> pf) {
			this.subjArgs = sArgs;
			this.pfTriple = pfTriple;
			this.objArgs = oArgs;
			this.dependentTriples = dependentTriples;
			this.pf = pf;
		}

		Triple getTriple() {
			return pfTriple;
		}

		Node getPredicate() {
			return pfTriple.getPredicate();
		}

		IndexPropertyFunction<?> getPropertyFunction() {
			return pf;
		}

		BasicPattern getDependentTriples() {
			return dependentTriples;
		}

		PropFuncArg getSubjectArgList() {
			return subjArgs;
		}

		PropFuncArg getObjectArgList() {
			return objArgs;
		}

	}

	private static Op makeStages(BasicPattern triples,
		Map<Triple, PropertyFunctionInstance> pfInvocations) {
		// Step 3 : Make the operation expression.
		// For each property function, insert the implementation
		// For each block of non-property function triples, make a BGP.

		Op op = null;
		BasicPattern pattern = null;
		LinkedList<PropertyFunctionInstance> orderedList = new LinkedList<>();
		for (Map.Entry<Triple, PropertyFunctionInstance> entry : pfInvocations
			.entrySet()) {
			IndexPropertyFunction<?> pf = entry.getValue().getPropertyFunction();
			if (pf instanceof EstimableIndexPropertyFunction) {
				orderedList.addFirst(entry.getValue());
			} else {
				orderedList.addLast(entry.getValue());
			}
		}
		LinkedList<Triple> ts = new LinkedList<>();
		ts.addAll(triples.getList());
		while (!ts.isEmpty()) {
			Triple t = ts.pop();
			if (!orderedList.isEmpty() && orderedList.peek().getTriple().equals(t)) {
				op = flush(pattern, op);
				pattern = null;

				PropertyFunctionInstance pfi = orderedList.pop();// pfInvocations.get(t);
				BasicPattern p = new BasicPattern(pfi.getDependentTriples());

				//            OpPropFunc pf = new OpPropFunc(pfi.getPredicate(),
				//                                           pfi.getSubjectArgList(),
				//                                           pfi.getObjectArgList(), op);

				OpIndexPropFunc opPF = new OpIndexPropFunc(pfi.getPredicate(),
					pfi.getSubjectArgList(),
					pfi.getObjectArgList(), p, op);
				pattern = new BasicPattern();
				op = opPF;
				continue;
			} else if (pfInvocations.containsKey(t)) {
				ts.add(t);
				continue;
			}

			// Regular triples - make sure there is a basic pattern in progress.
			if (pattern == null)
				pattern = new BasicPattern();
			pattern.add(t);
		}
		op = flush(pattern, op);
		while (!orderedList.isEmpty()) {
			PropertyFunctionInstance pfi = orderedList.pop();// pfInvocations.get(t);
			BasicPattern p = new BasicPattern(pfi.getDependentTriples());

			//         OpPropFunc pf = new OpPropFunc(pfi.getPredicate(),
			//                                        pfi.getSubjectArgList(),
			//                                        pfi.getObjectArgList(), op);

			OpIndexPropFunc opPF = new OpIndexPropFunc(pfi.getPredicate(),
				pfi.getSubjectArgList(),
				pfi.getObjectArgList(), p, op);
			pattern = new BasicPattern();
			op = opPF;
		}

		return op;
	}

	private static Op flush(BasicPattern pattern, Op op) {
		if (pattern == null || pattern.isEmpty()) {
			if (op == null)
				return OpTable.unit();
			return op;
		}
		OpBGP opBGP = new OpBGP(pattern);
		return OpSequence.create(op, opBGP);
	}

	public static PropertyFunctionRegistry chooseRegistry(Context context) {
		PropertyFunctionRegistry registry = PropertyFunctionRegistry.get(context);
		// Else global
		if (registry == null)
			registry = PropertyFunctionRegistry.get();
		return registry;
	}

	private static boolean isPropertyFunction(PropertyFunctionRegistry registry,
		Triple pfTriple) {
		if (!pfTriple.getPredicate().isURI())
			return false;

		String uri = pfTriple.getPredicate().getURI();
		if (registry.manages(uri)) {
			PropertyFunctionFactory factory = registry.get(uri);
			if (factory instanceof IndexPropertyFunctionFactory) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Split a pattern into partitions such that no variable overlaps partitions.
	 *
	 * @param pattern
	 *           the pattern to split.
	 * @return a list of partitions.
	 */
	private static List<Partition> partition(BasicPattern pattern) {
		List<Partition> partitions = new ArrayList<>();

		for (Triple t : pattern) {
			Partition current = null;
			Stack<Partition> toCheck = new Stack<>();
			toCheck.addAll(partitions);
			while (toCheck.size() > 0) {
				Partition p = toCheck.pop();
				if (p.handlesTriple(t)) {
					if (null == current) {
						current = p;
					} else {
						current.addAll(p);
						partitions.remove(p);
					}
				}
			}
			if (null == current) {
				current = new Partition();
				partitions.add(current);
			}
			current.add(t);
		}

		return partitions;
	}

	/**
	 * A pattern that keeps track of the variables/uris contained in it's
	 * triples.
	 *
	 * @author rbattle
	 */
	private static final class Partition extends BasicPattern {
		private Set<String> vars;

		/**
		 * Create a new instance.
		 */
		public Partition() {
			vars = new HashSet<>();
		}

		/**
		 * Answer whether the variable/URI is contained in this instance.
		 *
		 * @param varOrURI
		 *           a variable or URI to check.
		 * @return <code>true</code> if it is contained; otherwise
		 *         <code>false</code>.
		 */
		public boolean contains(String varOrURI) {
			return vars.contains(varOrURI);
		}

		/**
		 * Answer whether the triple should be handled by this instance.
		 *
		 * @param t
		 *           the triple to check.
		 * @return <code>true</code> if the triple contains variables already in
		 *         the pattern; otherwise <code>false</code>.
		 */
		public boolean handlesTriple(Triple t) {
			for (String v : getVariablesAndURIS(t)) {
				if (contains(v)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void add(Triple t) {
			vars.addAll(getVariablesAndURIS(t));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addAll(BasicPattern other) {
			for (Triple t : other) {
				add(t);
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Triple t : getList()) {
				sb.append(t);
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	static List<String> getVariablesAndURIS(Triple t) {
		List<String> vars = new ArrayList<>(2);
		for (Node n : new Node[] { t.getSubject(), t.getObject() }) {
			String var = getVariableOrURI(n);
			if (null != var) {
				vars.add(var);
			}
		}
		return vars;
	}

	private static String getVariableOrURI(Node n) {
		if (n.isVariable()) {
			return n.getName();
		} else if (n.isURI()) {
			return n.getURI();
		}
		return null;
	}

	private static <T> BasicPattern getDependentTriples(
		IndexPropertyFunction<T> pf, PropertyFunctionRegistry registry,
		Triple pfTriple, PropFuncArg subjArgs, PropFuncArg objArgs,
		BasicPattern triples) {
		List<Partition> partitions = partition(triples);
		for (Partition p : partitions) {
			if (p.handlesTriple(pfTriple)) {

				List<Triple> toCheck = new ArrayList<>(p.getList());
				List<Triple> toRemove = new ArrayList<>();
				for (Triple t : toCheck) {
					if (isPropertyFunction(registry, t)) {
						toRemove.add(t);
					}
				}
				toCheck.removeAll(toRemove);

				List<Node> subjects = new ArrayList<>();
				List<Node> objects = new ArrayList<>();
				if (subjArgs.isList()) {
					subjects.addAll(subjArgs.getArgList());
				} else {
					subjects.add(subjArgs.getArg());
				}
				if (objArgs.isList()) {
					objects.addAll(objArgs.getArgList());
				} else {
					objects.add(objArgs.getArg());
				}
				Map<Node, Operand<T>> operands = OperandFactoryHelper
					.getOperands(pf.getOperandFactory(), subjects, objects,
						BindingRoot.create(), triples, false);
				BasicPattern toReturn = new BasicPattern();
				for (Map.Entry<Node, Operand<T>> entry : operands.entrySet()) {
					for (Triple t : entry.getValue().getTriples()) {
						if (!toReturn.getList().contains(t)) {
							toReturn.add(t);
						}
					}
				}
				// List<Triple> toReturn =
				// pf.examine(BasicPattern.wrap(toCheck)).getList();//new
				// ArrayList<Triple>(getTriples(BasicPattern.wrap(toCheck), vars));

				return toReturn;
			}
		}

		return new BasicPattern();
	}

	// Remove all triples associated with this magic property.
	// Make an instance record.
	private static PropertyFunctionInstance magicProperty(Context context,
		Triple pfTriple, BasicPattern triples, List<Triple> triplesToRemove) {
		List<Triple> listTriples = new ArrayList<>();

		GNode sGNode = new GNode(triples, pfTriple.getSubject());
		GNode oGNode = new GNode(triples, pfTriple.getObject());
		List<Node> sList = null;
		List<Node> oList = null;

		if (GraphList.isListNode(sGNode)) {
			sList = GraphList.members(sGNode);
			GraphList.allTriples(sGNode, listTriples);
		}
		if (GraphList.isListNode(oGNode)) {
			oList = GraphList.members(oGNode);
			GraphList.allTriples(oGNode, listTriples);
		}

		PropFuncArg subjArgs = new PropFuncArg(sList, pfTriple.getSubject());
		PropFuncArg objArgs = new PropFuncArg(oList, pfTriple.getObject());

		Node predicate = pfTriple.getPredicate();
		String uri = predicate.getURI();

		PropertyFunctionRegistry registry = chooseRegistry(context);
		IndexPropertyFunction<?> pf = ((IndexPropertyFunctionFactory<?>) registry
			.get(uri)).create(uri);

		BasicPattern dependentTriples = getDependentTriples(pf, registry,
			pfTriple, subjArgs,
			objArgs, triples);

		// Confuses single arg with a list of one.
		PropertyFunctionInstance pfi = new PropertyFunctionInstance(
			subjArgs,
			pfTriple,
			objArgs,
			dependentTriples,
			pf);

		triples.getList().removeAll(listTriples);
		//      triples.getList().removeAll(dependentTriples.getList());
		triplesToRemove.addAll(dependentTriples.getList());
		return pfi;
	}
}
