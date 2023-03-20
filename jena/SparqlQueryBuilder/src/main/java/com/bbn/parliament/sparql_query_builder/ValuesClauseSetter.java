// Copyright (c) 2019, 2020 Raytheon BBN Technologies Corp.

package com.bbn.parliament.sparql_query_builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitorBase;

class ValuesClauseSetter extends ElementVisitorBase {
	private transient final List<String> variables;
	private transient final String markerValue;
	private transient final List<Collection<RDFNode>> newValues;

	public ValuesClauseSetter(String variable, String markerValue, Collection<RDFNode> newValues) {
		variables = Collections.unmodifiableList(Arrays.asList(variable));
		this.markerValue = markerValue;
		this.newValues = Collections.unmodifiableList(newValues.stream()
			.map(Arrays::asList)
			.map(Collections::unmodifiableList)
			.collect(Collectors.toList()));
		checkArity();
	}

	public ValuesClauseSetter(String variable, String markerValue, RDFNode... newValues) {
		variables = Collections.unmodifiableList(Arrays.asList(variable));
		this.markerValue = markerValue;
		this.newValues = Collections.unmodifiableList(Arrays.stream(newValues)
			.map(Arrays::asList)
			.map(Collections::unmodifiableList)
			.collect(Collectors.toList()));
		checkArity();
	}

	public ValuesClauseSetter(List<String> variables, String markerValue, RDFNode[]... newValues) {
		this.variables = Collections.unmodifiableList(variables);
		this.markerValue = markerValue;
		this.newValues = Collections.unmodifiableList(Arrays.stream(newValues)
			.map(Arrays::asList)
			.map(Collections::unmodifiableList)
			.collect(Collectors.toList()));
		checkArity();
	}

	public ValuesClauseSetter(List<String> variables, String markerValue, Collection<RDFNode[]> newValues) {
		this.variables = Collections.unmodifiableList(variables);
		this.markerValue = markerValue;
		this.newValues = Collections.unmodifiableList(newValues.stream()
			.map(Arrays::asList)
			.map(Collections::unmodifiableList)
			.collect(Collectors.toList()));
		checkArity();
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public ValuesClauseSetter(List<String> variables, String markerValue, Collection<RDFNode>... newValues) {
		this.variables = Collections.unmodifiableList(variables);
		this.markerValue = markerValue;
		this.newValues = Collections.unmodifiableList(Arrays.stream(newValues)
			.map(Collections::unmodifiableCollection)
			.collect(Collectors.toList()));
		checkArity();
	}

	private void checkArity() {
		long numNonMatchingBindings = this.newValues.stream()
			.filter(binding -> binding.size() != this.variables.size())
			.count();
		if (numNonMatchingBindings > 0) {
			throw new IllegalArgumentException(String.format(
				"%1$d bindings have a different arity than the variables list",
				numNonMatchingBindings));
		}
	}

	@Override
	public void visit(ElementData el) {
		if (varListsMatch(el) && firstBindingIsMarker(el)) {
			List<Binding> rowList = el.getRows();
			rowList.clear();
			newValues.stream()
				.map(newValuesRow -> mapNewValuesRowToBinding(el.getVars(), newValuesRow))
				.forEach(binding -> rowList.add(binding));
		}
	}

	private boolean varListsMatch(ElementData el) {
		List<String> elVarNames = el.getVars().stream()
			.map(var -> var.getVarName())
			.collect(Collectors.toList());
		return variables.equals(elVarNames);
	}

	private boolean firstBindingIsMarker(ElementData el) {
		Var firstVar = el.getVars().stream().findFirst().orElse(null);
		Binding firstBinding = el.getRows().stream().findFirst().orElse(null);
		if (firstVar == null || firstBinding == null) {
			return false;
		}

		Node fvofb = firstBinding.get(firstVar);	// Short for firstValueOfFirstBinding
		return (fvofb.isLiteral() && fvofb.getLiteralLexicalForm().equals(markerValue))
			|| (fvofb.isURI() && fvofb.getURI().equals(markerValue));
	}

	private static Binding mapNewValuesRowToBinding(List<Var> vars, Collection<RDFNode> newValuesRow) {
		BindingMap bmap = BindingFactory.create();
		int i = 0;	// NOPMD - DataflowAnomalyAnalysis (Deprecated)
		for (RDFNode rdfNode : newValuesRow) {
			bmap.add(vars.get(i), rdfNode.asNode());
			++i;
		}
		return bmap;
	}

	// ===== No-op methods to ensure the recursion continues =====

	@Override
	public void visit(ElementUnion el) {
		el.getElements().forEach(childEl -> childEl.visit(this));
	}

	@Override
	public void visit(ElementGroup el) {
		el.getElements().forEach(childEl -> childEl.visit(this));
	}

	@Override
	public void visit(ElementSubQuery el) {
		el.getQuery().getQueryPattern().visit(this);
	}
}
