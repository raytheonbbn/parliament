package com.bbn.parliament.spring.boot.service;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.jena.handler.InsertHandler;
import com.bbn.parliament.jena.handler.ExportHandler;
import com.bbn.parliament.jena.handler.ClearHandler;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;


@Component("graphStoreService")
public class GraphStoreService {

	public static void doGet(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//all encompassing construct on specified graph
			
			ExportHandler handler = new ExportHandler();
			handler.handleRequest(req, resp);
			
		} catch (Exception e) {
			
		}
	}
	
	public static void doPut(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//delete graph
			
			//set graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
		} catch (Exception e) {
			
		}
	}
	
	public static void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//delete graph
			ClearHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
		} catch (Exception e) {
			
		}
	}
	
	public static void doPost(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
		} catch (Exception e) {
			
		}
	}
	
	public static void doPatch(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
		} catch (Exception e) {
			
		}
	}
	


}
