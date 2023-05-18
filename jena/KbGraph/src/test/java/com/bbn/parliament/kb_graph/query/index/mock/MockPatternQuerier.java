package com.bbn.parliament.kb_graph.query.index.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.engine.iterator.QueryIterRepeatApply;
import org.apache.jena.sparql.util.IterLib;

import com.bbn.parliament.kb_graph.query.index.IndexPatternQuerier;

public class MockPatternQuerier implements IndexPatternQuerier {
	public static String NAMESPACE = "http://example.org/mock#";

	private boolean estimated;
	private boolean queried;
	private boolean examined;
	private static int counter = 0;
	private int numItems;
	private boolean hasBinding;

	public MockPatternQuerier(int numItems) {
		this.numItems = numItems;
	}

	@Override
	public long estimate(BasicPattern pattern) {
		estimated = true;
		return numItems;
	}

	public boolean hasBinding() {
		return hasBinding;
	}

	@Override
	public QueryIterator query(final BasicPattern pattern, QueryIterator input,
		final ExecutionContext context) {
		queried = true;
		QueryIterator ret = new QueryIterRepeatApply(input, context) {
			@Override
			protected QueryIterator nextStage(Binding binding) {
				hasBinding = !binding.isEmpty();
				BasicPattern bgp = Substitute.substitute(pattern, binding);
				List<Binding> bindings = new ArrayList<>();
				for (int i = 0; i < numItems; i++) {
					BindingMap b = BindingFactory.create(binding);
					boolean changed = false;
					for (Triple t : bgp) {
						if (t.getSubject().isVariable()) {
							b.add(Var.alloc(t.getSubject()), NodeFactory.createBlankNode(BlankNodeId.create("node" + (counter++))));
							changed = true;
						}
						if (t.getObject().isVariable()) {
							b.add(Var.alloc(t.getObject()), NodeFactory.createBlankNode(BlankNodeId.create("node" + (counter++))));
							changed = true;
						}
					}
					if (changed) {
						bindings.add(b);
					}
				}
				if (bindings.size() > 0) {
					return new QueryIterPlainWrapper(bindings.iterator(), context);
				} else {
					return IterLib.result(binding, context);
				}
			}
		};
		return ret;
	}

	@Override
	public BasicPattern examine(BasicPattern pattern) {
		examined = true;
		BasicPattern p = new BasicPattern();
		for (Triple t : pattern) {
			Node predicate = t.getPredicate();
			if (predicate.isURI() && NAMESPACE.equals(predicate.getNameSpace())) {
				p.add(t);
			}
		}
		return p;
	}

	public boolean isEstimated() {
		return estimated;
	}

	public boolean isQueried() {
		return queried;
	}

	public boolean isExamined() {
		return examined;
	}
}
