package com.bbn.parliament.jena.query.index.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryBase;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunctionFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.util.IterLib;

public class MockPropertyFunction extends IndexPropertyFunction<Integer> {
	public static final String URI = "http://example.org/mock#property";
	private static boolean called = false;

	public static boolean isCalled() {
		return called;
	}

	public MockPropertyFunction() {
		super(MockIndex.class, MockOperandFactory.class);
	}

	@Override
	public QueryIterator execBinding(Binding binding, List<Node> subjects,
		List<Node> objects, Map<Node, Operand<Integer>> operands,
		ExecutionContext context) {

		called = true;

		Node subject = subjects.get(0);
		Node object = objects.get(0);
		if (subject.isURI()) {
			Record<Integer> obj = index.find(subject);
			if (null == obj) {
				return IterLib.noResults(context);
			}
			BindingMap b = BindingFactory.create(binding);
			b.add(Var.alloc(object), ResourceFactory.createTypedLiteral(1)
				.asNode());
			return IterLib.result(b, context);
		} else if (subject.isVariable()) {
			List<Binding> bindings = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				BindingMap b = BindingFactory.create(binding);
				b.add(Var.alloc(object), ResourceFactory.createTypedLiteral(1)
					.asNode());
				bindings.add(b);
			}
			return new QueryIterPlainWrapper(bindings.iterator(), context);
		} else {
			return IterLib.noResults(context);
		}
	}

	public static class MockOperandFactory extends OperandFactoryBase<Integer> {
		@Override
		public Operand<Integer> createOperand(Node rootNode,
			BasicPattern pattern, Binding binding) {

			for (Triple t : pattern) {
				if (t.getPredicate().hasURI(URI)) {
					List<Triple> ts = new ArrayList<>();
					ts.add(t);
					return new Operand<>(rootNode, 0, ts);
				}
			}
			return null;
		}

		@Override
		public Operand<Integer> createOperand(Node rootNode,
			Binding binding) {
			// return new Operand<MockIndexedObject>(rootNode, new
			// MockIndexedObject());
			return null;
		}
	}

	public static class MockPropFxnFactory implements IndexPropertyFunctionFactory<Integer> {
		@Override
		public IndexPropertyFunction<Integer> create(String uri) {
			return new MockPropertyFunction();
		}
	}
}
