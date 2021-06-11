import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

void issueSelectQuery(String sparqlSelectQuery) {
	QueryExecution exec = QueryExecutionFactory.sparqlService(
		"http://localhost:8089/parliament/sparql",
		sparqlSelectQuery);
	try {
		for (ResultSet rs = exec.execSelect(); rs.hasNext();) {
			QuerySolution qs = rs.next();
			// Do something useful with the query solution set here
		}
	} finally {
		exec.close();
	}
}
