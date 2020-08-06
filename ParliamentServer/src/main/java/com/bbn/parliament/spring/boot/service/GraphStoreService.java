package com.bbn.parliament.spring.boot.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.bridge.ActionRouter;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.handler.ExportHandler;
import com.bbn.parliament.jena.handler.InsertHandler;

@Component("graphStoreService")
public class GraphStoreService {
	@Autowired
	ActionRouter actionRouter;

	@SuppressWarnings("static-method")
	public void doGet(String graphURI, String contentType, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, MissingGraphException, DataFormatException {
		ExportHandler handler = new ExportHandler();
		handler.handleRequest(req, resp, graphURI, contentType, false);
	}

	public void doPut(String contentType, String graphURI,
			HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, QueryExecutionException {
		doDelete(graphURI);
		doPost(contentType, graphURI, requestEntity, req, resp);
	}

	public void doDelete(String graphURI) throws QueryExecutionException {
		String sparqlStmt = (graphURI == null)
			? "DROP DEFAULT ;"
			: String.format("DROP GRAPH <%1s> ;", graphURI);
		actionRouter.execUpdate(sparqlStmt, "Parliament-GraphStoreService");
	}

	@SuppressWarnings("static-method")
	public void doPost(String contentType, String graphURI,
			HttpEntity<byte[]> requestEntity, HttpServletRequest req,
			HttpServletResponse resp) throws IOException, QueryExecutionException {
		InsertHandler handler = new InsertHandler();
		handler.handleRequest(contentType, graphURI, req.getRemoteAddr(), requestEntity, resp);
	}

	@SuppressWarnings("static-method")
	public void doFilePost(String contentType, String graphURI, MultipartFile[] files,
			HttpServletRequest req, HttpServletResponse resp)
			throws IOException, QueryExecutionException {
		InsertHandler handler = new InsertHandler();
		handler.handleFileRequest("auto", graphURI, req.getRemoteAddr(), files, resp);
	}
}
