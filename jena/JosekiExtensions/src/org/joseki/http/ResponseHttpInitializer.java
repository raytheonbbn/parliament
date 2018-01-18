package org.joseki.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.bbn.parliament.jena.util.JsonLdRdfWriter;

public class ResponseHttpInitializer {
	// Prevent instantiation:
	private ResponseHttpInitializer() {
	}

	public static void fixupHttpAcceptTypes() {
		List<String> mimeTypes = new ArrayList<>();
		mimeTypes.addAll(Arrays.asList(ResponseHttp.x));
		mimeTypes.add(JsonLdRdfWriter.contentType);
		String[] mimeTypesArray = mimeTypes.toArray(new String[mimeTypes.size()]);
		ResponseHttp.prefContentType = new AcceptList(mimeTypesArray);
	}
}
