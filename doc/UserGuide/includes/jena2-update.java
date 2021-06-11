import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

void issueUpdate(String sparqlUpdate)
{
	UpdateRequest request = UpdateFactory.create(sparqlUpdate);
	try {
		UpdateExecutionFactory
			.createRemote(request, "http://localhost:8089/parliament/sparql")
			.execute();
	} catch (NullPointerException ex) {
		StackTraceElement topFrame = ex.getStackTrace()[0];
		if ("org.openjena.riot.web.HttpOp".equals(topFrame.getClassName())
				&& "httpResponse".equals(topFrame.getMethodName())
				&& 345 == topFrame.getLineNumber()) {
			// Ignoring spurious NPE
		} else {
			throw new RuntimeException("Re-thrown NPE:", ex);
		}
	}
}
