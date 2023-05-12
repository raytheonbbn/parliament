// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.server.configuration.vocab.ConfigOnt;
import com.bbn.parliament.server.exception.ConfigurationException;
import com.bbn.parliament.server.exception.ParliamentBridgeException;
import com.bbn.parliament.util.JavaResource;
import com.bbn.parliament.util.StreamUtil;

public class ParliamentBridgeConfiguration {
	/** Default threshold above which DeferredFileOutputStream stores to disk. Set to 10 MB. */
	public static final int DEFAULT_DEFERRED_FILE_THRESHOLD = 10 * 1024 * 1024;
	private static final String CONFIG_ONT_FILE = "config-ont.ttl";
	private static final Logger LOG = LoggerFactory.getLogger(ParliamentBridgeConfiguration.class);

	private List<ConfigurationHandler> _configurationHandlers;
	private boolean _clearDataOnStartup;
	private List<String> _baselineDirs;
	private int _deferredFileOutputStreamThreshold;
	private PrefixMap _prefixes;
	private File _tmpDir;

	public ParliamentBridgeConfiguration(String modelConfFile, File tmpDir) throws ParliamentBridgeException {
		Model m = loadModelConfiguration(modelConfFile);

		_configurationHandlers = readConfigurationHandlers(m);
		_baselineDirs = readBaselineDirectories(m);
		_clearDataOnStartup = readClearDataOnStartup(m);
		_deferredFileOutputStreamThreshold = readDeferredFileOutputStreamThreshold(m);
		_prefixes = readPrefixes(m);

		_tmpDir = (tmpDir == null)
			? new File(System.getProperty("java.io.tmpdir", "."))
			: tmpDir;
		if (LOG.isTraceEnabled()) {
			LOG.trace("Set tmpDir to {} tmpDir:  '{}'",
				(tmpDir == null) ? "the system" : "Jetty's",
				_tmpDir.getAbsolutePath());
		}
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

	public int getDeferredFileOutputStreamThreshold() {
		return _deferredFileOutputStreamThreshold;
	}

	public PrefixMap getPrefixes() {
		return _prefixes;
	}

	public File getTmpDir() {
		return _tmpDir;
	}

	private static PrefixMap readPrefixes(Model m) throws ParliamentBridgeException {
		Resource conf = getConfigResource(m);
		var prefixes = StreamUtil.asStream(conf.listProperties(ConfigOnt.prefixes))
			.map(stmt -> stmt.getObject().as(RDFList.class))
			.map(RDFList::asJavaList)
			.flatMap(List::stream)
			.map(RDFNode::asResource)
			// pm is short for prefix mapping:
			.filter(pm -> pm.hasProperty(ConfigOnt.prefix) && pm.hasProperty(ConfigOnt.uri))
			.collect(Collectors.toMap(
				pm -> pm.getProperty(ConfigOnt.prefix).getString(),			// key mapper
				pm -> pm.getProperty(ConfigOnt.uri).getResource().getURI(),	// value mapper
				(v1, v2) -> {																// merge function
					LOG.warn("Namespaces <{}> and <{}> have the same prefix", v1, v2);
					return v1;
				},
				TreeMap::new));															// map factory
		PrefixMap map = new PrefixMapStd();
		map.putAll(prefixes);
		return PrefixMapFactory.unmodifiablePrefixMap(map);
	}

	private static List<String> readBaselineDirectories(Model m)
			throws ParliamentBridgeException {
		Resource conf = getConfigResource(m);
		var baselineDirs = StreamUtil.asStream(conf.listProperties(ConfigOnt.baselineDir))
			.map(Statement::getString)
			.collect(Collectors.toUnmodifiableSet());
		return Collections.unmodifiableList(new ArrayList<>(baselineDirs));
	}

	private static int readDeferredFileOutputStreamThreshold(Model m)
			throws ParliamentBridgeException {
		Resource conf = getConfigResource(m);
		return conf.hasProperty(ConfigOnt.deferredFileOutputStreamThreshold)
			? conf.getProperty(ConfigOnt.deferredFileOutputStreamThreshold).getInt()
			: DEFAULT_DEFERRED_FILE_THRESHOLD;
	}

	private static boolean readClearDataOnStartup(Model m) throws ParliamentBridgeException {
		Resource conf = getConfigResource(m);
		return conf.hasProperty(ConfigOnt.clearDataOnStartup)
			? conf.getProperty(ConfigOnt.clearDataOnStartup).getBoolean()
			: false;
	}

	private static List<ConfigurationHandler> readConfigurationHandlers(Model m)
			throws ParliamentBridgeException {
		Resource conf = getConfigResource(m);

		// get configuration handlers
		List<Resource> handlers = StreamUtil.asStream(conf.listProperties(ConfigOnt.configurationHandler))
			.map(Statement::getResource)
			.map(handler -> handler.hasProperty(RDF.type, RDF.List)
				? convertRdfList(handler)
				: Collections.singletonList(handler))
			.flatMap(List::stream)
			.map(handler -> Pair.of(handler, getHndlrImplCls(handler)))
			.filter(pair -> isValidConfigHandler(pair.getLeft(), pair.getRight()))
			.map(Pair::getLeft)
			.collect(Collectors.toCollection(ArrayList::new));

		// There can only be one index handler and it needs to be configured last...
		if (conf.hasProperty(ConfigOnt.indexHandler)) {
			Resource handler = conf.getProperty(ConfigOnt.indexHandler).getResource();
			Class<? extends ConfigurationHandler> handlerClazz = getHndlrImplCls(handler);
			if (IndexProcessorConfigurationHandler.class.isAssignableFrom(handlerClazz)) {
				handlers.add(handler);
			} else {
				LOG.warn("The property {} must have an object of type {}",
					ConfigOnt.indexHandler.getURI(),
					ConfigOnt.IndexProcessorConfigurationHandler.getURI());
			}
		}

		return Collections.unmodifiableList(handlers.stream()
			.map(ParliamentBridgeConfiguration::newConfigurationHandler)
			.filter(Objects::nonNull)
			.toList());
	}

	private static boolean isValidConfigHandler(Resource res, Class<? extends ConfigurationHandler> handlerClazz) {
		if (handlerClazz == null) {
			return false;
		} else if (IndexProcessorConfigurationHandler.class.isAssignableFrom(handlerClazz)) {
			LOG.warn("Configuration handler {} with implementation {} must use the {} property.",
				res.getURI(), handlerClazz.getName(), ConfigOnt.indexHandler.getURI());
			return false;
		} else {
			return true;
		}
	}

	private static ConfigurationHandler newConfigurationHandler(Resource handler) {
		Class<? extends ConfigurationHandler> cls = getHndlrImplCls(handler);
		try {
			ConfigurationHandler cHndlr = ConfigurationHandler.class.cast(
				cls.getDeclaredConstructor().newInstance());
			cHndlr.initialize(handler);
			return cHndlr;
		} catch (ConfigurationException | RuntimeException | ReflectiveOperationException ex) {
			LOG.error("Could not instantiate " + cls.getName(), ex);
			return null;
		}
	}

	private static Resource getConfigResource(Model m) throws ParliamentBridgeException {
		ResIterator ri = m.listResourcesWithProperty(RDF.type, ConfigOnt.Configuration);
		if (!ri.hasNext()) {
			throw new ParliamentBridgeException("No configuration resource in configuration");
		}
		Resource conf = ri.nextResource();
		if (ri.hasNext()) {
			LOG.warn("There is more than one configuration resource. Using: {}", conf.getURI());
		}
		return conf;
	}

	/** Load the model configuration from file and ontology resources. */
	private static Model loadModelConfiguration(String fileName) {
		Model configModel = ModelFactory.createRDFSModel(ModelFactory.createDefaultModel());
		loadConfigRsrc(CONFIG_ONT_FILE, configModel);
		loadConfigRsrc(fileName, configModel);
		return configModel;
	}

	/** Loads the given file from resources into the given model and returns true if successful. */
	private static boolean loadConfigRsrc(String fileName, Model destModel) {
		boolean result = false;

		if (fileName == null || fileName.isEmpty()) {
			LOG.warn("Model configuration filename is null or empty");
			throw new IllegalArgumentException("Model configuration filename is null or empty");
		}

		var lang = RDFLanguages.pathnameToLang(fileName);
		if (lang == null) {
			LOG.warn("Model configuration file \"{}\" is of unrecognized format \"{}\"",
				fileName, lang);
		} else {
			try (InputStream strm = JavaResource.getAsStream(fileName)) {
				destModel.read(strm, null, lang.getName());
				result = true;
			} catch (IOException ex) {
				LOG.warn("Error while reading model configuration file \"{}\":  {}",
					fileName, ex.getMessage());
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

	private static Class<? extends ConfigurationHandler> getHndlrImplCls(Resource handler) {
		String hndlrClsNm = StreamUtil.asStream(handler.listProperties(RDF.type))
			.map(Statement::getResource)
			.filter(handlerType -> handlerType.hasProperty(ConfigOnt.implementationClass))
			.map(handlerType -> handlerType.getProperty(ConfigOnt.implementationClass).getString())
			.findFirst()
			.orElse(null);
		if (hndlrClsNm == null) {
			LOG.warn("Configuration handler {} has no implementation class", handler.getURI());
			return null;
		}
		try {
			Class<?> hndlrCls = Class.forName(hndlrClsNm);
			if (ConfigurationHandler.class.isAssignableFrom(hndlrCls)) {
				return hndlrCls.asSubclass(ConfigurationHandler.class);
			} else {
				LOG.error("{} is not a {}", hndlrClsNm, ConfigurationHandler.class.getName());
				return null;
			}
		} catch (ClassNotFoundException ex) {
			LOG.error("Could not find class " + hndlrClsNm, ex);
			return null;
		}
	}
}
