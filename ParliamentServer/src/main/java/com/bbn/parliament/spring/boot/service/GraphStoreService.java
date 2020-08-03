package com.bbn.parliament.spring.boot.service;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.bridge.ActionRouter;
import com.bbn.parliament.jena.handler.ExportHandler;
import com.bbn.parliament.jena.handler.InsertHandler;

@Component("graphStoreService")
public class GraphStoreService {

	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

	@Autowired
	ActionRouter actionRouter;

	public void doGet(String graphURI, HttpServletRequest req, HttpServletResponse resp) throws Exception {

		try {
			//all encompassing construct on specified graph

			ExportHandler handler = new ExportHandler();
			handler.handleFormURLEncodedRequest(req, resp);

		} catch (Exception e) {
			LOG.info(e.toString());
			throw new Exception();
		}
	}

	public void doPut(String contentType, String graphURI, HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp) throws Exception {

		try {
			//delete graph
			doDelete(graphURI);

			//set graph
			 doPost(contentType, graphURI, requestEntity, req, resp);

		} catch (Exception e) {
			LOG.info(e.toString());
			throw new Exception();
		}
	}

	public void doDelete(String graphURI) throws Exception {

		try {
			//delete graph
			String sparqlStmt;
			sparqlStmt = (graphURI == null) ? "DROP DEFAULT ;" : String.format("DROP GRAPH <%1s> ;", graphURI);
			actionRouter.execUpdate(sparqlStmt, "Parliament-GraphStoreService");

			//ClearHandler handler = new ClearHandler();
			//handler.handleFormURLEncodedRequest(graphURI, req, resp);
		} catch (Exception e) {
			LOG.info(e.toString());
			throw new Exception();
		}
	}

	public void doPost(String contentType, String graphURI, HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp) throws Exception {

		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(contentType, graphURI, req.getRemoteAddr(), requestEntity, resp);

		} catch (Exception e) {
			LOG.info(e.toString());
			throw new Exception();
		}
	}

	public void doFilePost(String contentType, String graphURI, MultipartFile[] files, HttpServletRequest req, HttpServletResponse resp) throws Exception {

		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleFileRequest("auto", graphURI, req.getRemoteAddr(), files, resp);

		} catch (Exception e) {
			LOG.info(e.toString());
			throw new Exception();
		}
	}

}
