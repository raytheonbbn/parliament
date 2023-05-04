package com.bbn.parliament.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParliamentServletContextListener implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(ParliamentServletContextListener.class);

	public ParliamentServletContextListener() {
		LOG.info("ParliamentServletContextListener created");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.info("contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.info("Shutting down parliament servlet");
		ParliamentBridge.getInstance().stop();
	}
}
