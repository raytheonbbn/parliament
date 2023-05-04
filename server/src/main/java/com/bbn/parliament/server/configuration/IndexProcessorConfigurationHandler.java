// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.configuration;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Properties;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.index.IndexFactory;
import com.bbn.parliament.kb_graph.index.IndexFactoryRegistry;
import com.bbn.parliament.server.ParliamentBridge;
import com.bbn.parliament.server.configuration.vocab.ConfigOnt;
import com.bbn.parliament.server.graph.ModelManager;

/**
 * A {@link ConfigurationHandler} that registers a list of {@link IndexFactory}s
 * to support the runtime creation of indexes on a graph.
 *
 * @author rbattle
 */
public class IndexProcessorConfigurationHandler implements ConfigurationHandler {
	private static final Logger LOG = LoggerFactory.getLogger(IndexProcessorConfigurationHandler.class);

	/** {@inheritDoc} */
	@Override
	public void initialize(Resource handle) {
		StmtIterator si = handle.listProperties(ConfigOnt.index);
		while (si.hasNext()) {
			Statement s = si.nextStatement();
			Resource index = s.getResource();
			Statement indexClass = index.getProperty(ConfigOnt.processorFactoryClass);
			String className = indexClass.getString();
			Class<?> uclass;
			@SuppressWarnings("rawtypes")
			Class<? extends IndexFactory> factoryClass;
			IndexFactory<?, ?> factory = null;
			LOG.info("Loading {}", className);
			try {
				uclass = Class.forName(className);
			} catch (ClassNotFoundException | RuntimeException ex) {
				LOG.error("Error while loading " + className, ex);
				continue;
			}
			if (!IndexFactory.class.isAssignableFrom(uclass)) {
				LOG.warn("{} is not a valid IndexProcessor", className);
				continue;
			}
			factoryClass = uclass.asSubclass(IndexFactory.class);
			try {
				factory = factoryClass.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException ex) {
				LOG.error("Could not instantiate " + className, ex);
				continue;
			}
			Properties properties = createProperties(index);
			factory.configure(properties);
			IndexFactoryRegistry.getInstance().register(factory);
		}
		si = handle.listProperties(ConfigOnt.enabledByDefault);
		boolean enabledByDefault = true;
		while (si.hasNext()) {
			Statement s = si.nextStatement();
			if (s.getObject().isLiteral()) {
				enabledByDefault = s.getBoolean();
			}
		}
		IndexFactoryRegistry.getInstance().setIndexingEnabledByDefault(enabledByDefault);
	}

	private static Properties createProperties(Resource configuration) {
		Properties p = new Properties();
		StmtIterator si = configuration.listProperties();
		while (si.hasNext()) {
			Statement s = si.next();
			if (!s.getObject().isLiteral()) {
				continue;
			}
			String uri = s.getPredicate().getURI();

			Literal l = s.getObject().asLiteral();

			String value = l.getValue().toString();
			URI u = URI.create(uri);
			String prop = u.getFragment();
			p.put(prop, value);
		}

		return p;
	}

	/** {@inheritDoc} */
	@Override
	public void postModelInitialization(ParliamentBridge server, ModelManager manager) {
	}

	/** {@inheritDoc} */
	@Override
	public void preModelInitialization(ParliamentBridge server, ModelManager manager) {
	}
}