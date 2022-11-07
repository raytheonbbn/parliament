// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.configuration.ConfigurationException;
import com.bbn.parliament.jena.bridge.configuration.ConfigurationHandler;
import com.bbn.parliament.jena.bridge.configuration.IndexProcessorConfigurationHandler;
import com.bbn.parliament.jena.bridge.configuration.vocab.ConfigOnt;
import com.bbn.parliament.jena.joseki.client.RDFFormat;

public class ParliamentBridgeConfiguration {
	/** Default threshold above which DeferredFileOutputStream stores to disk. Set to 10 MB. */
	public static final int DEFAULT_DEFERRED_FILE_THRESHOLD = 10 * 1024 * 1024;
	private static final String CONFIG_ONT_FILE = "config-ont.ttl";
	private static final Logger log = LoggerFactory.getLogger(ParliamentBridgeConfiguration.class);

	private List<ConfigurationHandler> _configurationHandlers;
	private boolean _clearDataOnStartup = false;
	private List<String> _baselineDirs;
	private File _tmpDir;
	private int _deferredFileOutputStreamThreshold;
	private PrefixMap _prefixes;

	private ParliamentBridgeConfiguration() {
	}

	public List<ConfigurationHandler> getConfigurationHandlers() {
		return _configurationHandlers;
	}

	public boolean isClearDataOnStartup() {
		return _clearDataOnStartup;
	}

	public List<String> getBaselineDirs() {
		return _baselineDirs;
	}

	public File getTmpDir() {
		return _tmpDir;
	}

	public int getDeferredFileOutputStreamThreshold() {
		return _deferredFileOutputStreamThreshold;
	}

	public PrefixMap getPrefixes() {
		return _prefixes;
	}

	public static ParliamentBridgeConfiguration readConfiguration(
		String modelConfFile, File tmpDir) throws ParliamentBridgeException {

		Model m = loadModelConfiguration(modelConfFile);
		ParliamentBridgeConfiguration config = new ParliamentBridgeConfiguration();

		config._configurationHandlers = readConfigurationHandlers(m);
		config._baselineDirs = readBaselineDirectories(m);
		config._clearDataOnStartup = readClearDataOnStartup(m);
		config._tmpDir = (tmpDir == null)
			? new File(System.getProperty("java.io.tmpdir", "."))
			: tmpDir;
		if (log.isTraceEnabled()) {
			log.trace("Set tmpDir to {} tmpDir:  '{}'",
				(tmpDir == null) ? "the system" : "Jetty's",
				config._tmpDir.getAbsolutePath());
		}
		config._deferredFileOutputStreamThreshold = readDeferredFileOutputStreamThreshold(m);

		config._prefixes = readPrefixes(m);
		return config;
	}

	private static PrefixMap readPrefixes(Model m) throws ParliamentBridgeException {
		PrefixMap map = new PrefixMapStd();
		Resource conf = getConfigResource(m);
		StmtIterator si = conf.listProperties(ConfigOnt.prefixes);
		while (si.hasNext()) {
			Statement s = si.nextStatement();
			RDFList list = s.getObject().as(RDFList.class);
			List<RDFNode> e = list.asJavaList();
			for (RDFNode mapping : e) {
				Resource pm = mapping.asResource();
				if (!pm.hasProperty(ConfigOnt.prefix) || !pm.hasProperty(ConfigOnt.uri)) {
					throw new ParliamentBridgeException("Invalid prefix mapping");
				}
				String prefix = pm.getProperty(ConfigOnt.prefix).getString();
				String uri = pm.getProperty(ConfigOnt.uri).getResource().getURI();
				map.add(prefix, uri);
			}
		}
		return map;
	}

	private static List<String> readBaselineDirectories(Model m)
		throws ParliamentBridgeException {
		List<String> baselineDirs = new ArrayList<>();
		Resource conf = getConfigResource(m);

		StmtIterator si = conf.listProperties(ConfigOnt.baselineDir);
		while (si.hasNext()) {
			Statement s = si.nextStatement();
			if (!baselineDirs.contains(s.getString())) {
				baselineDirs.add(s.getString());
			}
		}

		return baselineDirs;
	}

	private static int readDeferredFileOutputStreamThreshold(Model m)
		throws ParliamentBridgeException {
		int toReturn = DEFAULT_DEFERRED_FILE_THRESHOLD;
		Resource conf = getConfigResource(m);
		if (conf.hasProperty(ConfigOnt.deferredFileOutputStreamThreshold)) {
			toReturn = conf
				.getProperty(ConfigOnt.deferredFileOutputStreamThreshold)
				.getInt();
		}
		return toReturn;
	}

	private static boolean readClearDataOnStartup(Model m)
		throws ParliamentBridgeException {
		boolean read = false;
		Resource conf = getConfigResource(m);
		if (conf.hasProperty(ConfigOnt.clearDataOnStartup)) {
			read = conf.getProperty(ConfigOnt.clearDataOnStartup).getBoolean();
		}
		return read;
	}

	private static List<ConfigurationHandler> readConfigurationHandlers(Model m)
		throws ParliamentBridgeException {
		List<ConfigurationHandler> configurationHandlers = new ArrayList<>();

		Resource conf = getConfigResource(m);

		// get configuration handlers
		StmtIterator si = conf.listProperties(ConfigOnt.configurationHandler);
		List<Resource> handlers = new ArrayList<>();
		while (si.hasNext()) {
			Statement s = si.nextStatement();

			Resource handler = s.getResource();
			List<Resource> resources = null;
			if (handler.hasProperty(RDF.type, RDF.List)) {
				resources = convertRdfList(handler);
			} else {
				resources = new ArrayList<>();
				resources.add(handler);
			}
			for (Resource res : resources) {
				Class<? extends ConfigurationHandler> handlerClazz = getHndlrImplCls(res);
				if (handlerClazz == null) {
					continue;
				}
				if (IndexProcessorConfigurationHandler.class
					.isAssignableFrom(handlerClazz)) {
					log.warn("Configuration handler {} with implementation {} must use the {} property.",
						new Object[] { res.getURI(), handlerClazz.getName(),
							ConfigOnt.indexHandler.getURI() });
				} else {
					handlers.add(res);
				}
			}
		}

		// can only be one index handler
		// and it needs to be configured last...
		if (conf.hasProperty(ConfigOnt.indexHandler)) {
			Statement s = conf.getProperty(ConfigOnt.indexHandler);
			Class<? extends ConfigurationHandler> handlerClazz = getHndlrImplCls(s
				.getResource());
			if (IndexProcessorConfigurationHandler.class
				.isAssignableFrom(handlerClazz)) {
				handlers.add(s.getResource());
			} else {
				log.warn("The property {} must have an object of type {}",
					ConfigOnt.indexHandler.getURI(),
					ConfigOnt.IndexProcessorConfigurationHandler.getURI());
			}
		}

		// load and initialize configuration handlers
		for (Resource handler : handlers) {
			Class<? extends ConfigurationHandler> cls = getHndlrImplCls(handler);

			// Create and initialize configuration handler:
			try {
				ConfigurationHandler cHndlr = ConfigurationHandler.class.cast(
					cls.getDeclaredConstructor().newInstance());
				log.info("Initializing {}", cls.getName());
				cHndlr.initialize(handler);
				configurationHandlers.add(cHndlr);
			} catch (InstantiationException | IllegalAccessException
				| ConfigurationException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException ex) {
				log.error("Could not instantiate " + cls.getName(), ex);
				continue;
			}
		}

		return configurationHandlers;
	}

	private static Resource getConfigResource(Model m) throws ParliamentBridgeException {
		// get parliament configuration resource
		ResIterator ri = m.listResourcesWithProperty(RDF.type, ConfigOnt.Configuration);
		if (!ri.hasNext()) {
			throw new ParliamentBridgeException(
				"Could not find any configuration resources in configuration model");
		}
		Resource conf = ri.nextResource();
		if (ri.hasNext()) {
			log.warn("There is more than one configuration in model.  Using: {}",
				conf.getURI());
		}
		return conf;
	}

	/** Load the model configuration from file and ontology resources. */
	private static Model loadModelConfiguration(String fileName) {
		Model configModel = ModelFactory.createRDFSModel(ModelFactory.createDefaultModel());
		return (loadModelConfigurationResource(CONFIG_ONT_FILE, configModel)
				&& loadModelConfigurationResource(fileName, configModel))
			? configModel
			: ModelFactory.createDefaultModel();
	}

	/** Loads the given file from resources into the given model and returns true if successful. */
	private static boolean loadModelConfigurationResource(String fileName, Model destModel) {
		boolean result = false;

		if (fileName == null || fileName.isEmpty()) {
			log.warn("Model configuration filename is null or empty");
			throw new IllegalArgumentException("Model configuration filename is null or empty");
		}

		RDFFormat format = RDFFormat.parseFilename(fileName);
		if (!format.isJenaReadable()) {
			log.warn("Model configuration file \"{}\" is of unrecognized format \"{}\"", fileName, format);
		} else {
			try (InputStream strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
				if (strm == null) {
					log.warn("Unable to open input stream for model configuration file \"{}\"", fileName);
				} else {
					destModel.read(strm, null, format.toString());
					result = true;
				}
			} catch (IOException ex) {
				log.warn("Error while closing model configuration file \"{}\":  {}", fileName, ex.getMessage());
			}
		}

		return result;
	}

	/** Converts the given RDF list into a java List of resources. */
	private static List<Resource> convertRdfList(Resource listHandle) {
		List<Resource> list = new ArrayList<>();
		Resource first = listHandle.getProperty(RDF.first).getResource();
		if (!first.equals(RDF.nil)) {
			list.add(first);
		}
		if (listHandle.hasProperty(RDF.rest)) {
			Resource rest = listHandle.getProperty(RDF.rest).getResource();
			if (!(rest.equals(RDF.nil))) {
				list.addAll(convertRdfList(rest));
			}
		}
		return list;
	}

	private static Class<? extends ConfigurationHandler> getHndlrImplCls(
		Resource handler) {
		StmtIterator typeIterator = handler.listProperties(RDF.type);
		String hndlrClsNm = null;
		while (typeIterator.hasNext()) {
			Statement typeStatement = typeIterator.nextStatement();
			Resource handlerType = typeStatement.getResource();
			if (handlerType.hasProperty(ConfigOnt.implementationClass)) {
				hndlrClsNm = handlerType.getProperty(ConfigOnt.implementationClass)
					.getString();
				break;
			}
		}
		if (hndlrClsNm == null) {
			log.warn("Configuration handler {} has no implementation class",
				handler.getURI());
			return null;
		}
		Class<?> hndlrCls;
		try {
			hndlrCls = Class.forName(hndlrClsNm);
		} catch (ClassNotFoundException ex) {
			log.error("Could not find class " + hndlrClsNm, ex);
			return null;
		}
		if (!(ConfigurationHandler.class.isAssignableFrom(hndlrCls))) {
			log.error("{} is not a {}", hndlrClsNm,
				ConfigurationHandler.class.getName());
			return null;
		}

		return hndlrCls.asSubclass(ConfigurationHandler.class);
	}
}
