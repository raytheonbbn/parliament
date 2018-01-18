package com.bbn.parliament.jena.query;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.ResultSetStream;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable;
import com.hp.hpl.jena.util.FileManager;

public class QueryTestUtil {

	public static ResultSetRewindable makeUnique(ResultSetRewindable results) {
		// VERY crude.  Utilises the fact that bindings have value equality.
		List<Binding> x = new ArrayList<>() ;
		Set<Binding> seen = new HashSet<>() ;

		for ( ; results.hasNext() ; )
		{
			Binding b = results.nextBinding() ;
			if ( seen.contains(b) )
				continue ;
			seen.add(b) ;
			x.add(b) ;
		}
		QueryIterator qIter = new QueryIterPlainWrapper(x.iterator()) ;
		ResultSet rs = new ResultSetStream(results.getResultVars(), ModelFactory.createDefaultModel(), qIter) ;
		return ResultSetFactory.makeRewindable(rs) ;
	}

	public static boolean equals(ResultSet expected, ResultSet actual, Query query) {
		ResultSetRewindable exp = ResultSetFactory.makeRewindable(expected);
		ResultSetRewindable act = ResultSetFactory.makeRewindable(actual);
		if (query.isReduced()) {
			exp = makeUnique(exp);
			act = makeUnique(act);
		}
		if (query.isOrdered()) {
			return ResultSetCompare.equalsByValueAndOrder(exp, act);
		}
		return ResultSetCompare.equalsByValue(exp, act);
	}

	public static boolean equals(QuerySolution b1, QuerySolution b2) {
		List<String> b1Vars = new ArrayList<>();
		Iterator<String> b1It = b1.varNames();
		while (b1It.hasNext()) {
			b1Vars.add(b1It.next());
		}
		List<String> b2Vars = new ArrayList<>();
		Iterator<String> b2It = b2.varNames();
		while (b2It.hasNext()) {
			b2Vars.add(b2It.next());
		}
		boolean equals = true;
		equals = equals && (b1Vars.size() == b2Vars.size());
		equals = equals && b1Vars.containsAll(b2Vars);
		equals = equals && b2Vars.containsAll(b1Vars);
		if (equals) {
			for (String var : b1Vars) {
				RDFNode n1 = b1.get(var);
				RDFNode n2 = b2.get(var);
				equals = equals && n1.equals(n2);
				if (!equals) {
					break;
				}
			}
		}
		return equals;
	}

	public static ResultSet loadResultSet(String resultSet) {
		if (resultSet.toLowerCase().endsWith("srx")) {
			return ResultSetFactory.fromXML(getResource(resultSet));
		}
		return ResultSetFactory.fromRDF(loadModel(resultSet));
	}


	public static Model loadModel(String model) {
		Model m = ModelFactory.createDefaultModel();
		final String lmodel = model.toLowerCase();
		RDFFormat dataFormat = RDFFormat.parseFilename(model);
		if (RDFFormat.UNKNOWN.equals(dataFormat) && lmodel.endsWith("rq")) {
			dataFormat = RDFFormat.TURTLE;
		}
		FileManager.get().readModel(m, model, dataFormat.toString());
		return m;
	}


	public static InputStream getResource(String resource) {
		return Thread.currentThread().getContextClassLoader()
			.getResourceAsStream(resource);
	}
}
