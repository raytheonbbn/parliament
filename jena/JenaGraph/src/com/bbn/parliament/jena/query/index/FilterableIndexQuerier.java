package com.bbn.parliament.jena.query.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_GreaterThan;
import com.hp.hpl.jena.sparql.expr.E_GreaterThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_LessThan;
import com.hp.hpl.jena.sparql.expr.E_LessThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_NotEquals;
import com.hp.hpl.jena.sparql.expr.Expr;

public interface FilterableIndexQuerier extends IndexPatternQuerier {
	public static final String FILTER_PROP_NAMESPACE = "http://parliament.semwebcentral.org/index/filter#";

	public static enum FilterPredicate {
		equals, notEquals, greaterThan, greaterThanOrEqual, lessThan, lessThanOrEqual;

		public String toUri() {
			return FILTER_PROP_NAMESPACE + this.name();
		}

		public static FilterPredicate valueOfNode(Node n) {
			if (!n.isURI() || !n.getNameSpace().equals(FILTER_PROP_NAMESPACE)) {
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
