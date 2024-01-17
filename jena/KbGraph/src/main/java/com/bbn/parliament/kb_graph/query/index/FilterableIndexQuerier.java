package com.bbn.parliament.kb_graph.query.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_GreaterThan;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_LessThan;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.expr.E_NotEquals;
import org.apache.jena.sparql.expr.Expr;

import com.bbn.parliament.kb_graph.Constants;

public interface FilterableIndexQuerier extends IndexPatternQuerier {
	public static enum FilterPredicate {
		equals, notEquals, greaterThan, greaterThanOrEqual, lessThan, lessThanOrEqual;

		public String toUri() {
			return Constants.FILTER_PROP_NS + this.name();
		}

		public static FilterPredicate valueOfNode(Node n) {
			if (!n.isURI() || !n.getNameSpace().equals(Constants.FILTER_PROP_NS)) {
				return null;
			}
			return valueOf(n.getLocalName());
		}

		public static List<String> uris() {
			List<String> values = new ArrayList<>();
			for (FilterPredicate fp : FilterPredicate.values()) {
				values.add(fp.toUri());
			}
			return values;
		}

		public static  Map<Class<? extends Expr>, FilterPredicate> expressionMap() {
			Map<Class<? extends Expr>, FilterPredicate> filters = new HashMap<>();

			filters.put(E_Equals.class, FilterPredicate.equals);
			filters.put(E_NotEquals.class, FilterPredicate.notEquals);
			filters.put(E_GreaterThan.class, FilterPredicate.greaterThan);
			filters.put(E_GreaterThanOrEqual.class,
				FilterPredicate.greaterThanOrEqual);
			filters.put(E_LessThan.class, FilterPredicate.lessThan);
			filters.put(E_LessThanOrEqual.class,
				FilterPredicate.lessThanOrEqual);

			return filters;
		}
	}

	public boolean processFilterExpression(BasicPattern pattern, Expr expression);
}
