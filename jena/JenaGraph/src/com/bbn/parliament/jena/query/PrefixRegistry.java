package com.bbn.parliament.jena.query;

import java.util.Collections;
import java.util.Map;

import org.apache.jena.iri.IRI;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;

public class PrefixRegistry {
	private static final PrefixRegistry INSTANCE = new PrefixRegistry();

	private final Object lock;
	private final PrefixMap prefixes;

	public static PrefixRegistry getInstance() {
		return INSTANCE;
	}

	private PrefixRegistry() {
		lock = new Object();
		prefixes = new PrefixMapStd();
	}

	public void registerPrefix(String prefix, String uri) {
		synchronized (lock) {
			prefixes.add(prefix, uri);
		}
	}

	public void registerPrefixes(PrefixMap map) {
		synchronized (lock) {
			Map<String, IRI> mapping = map.getMapping();
			for (Map.Entry<String, IRI> entry : mapping.entrySet()) {
				prefixes.add(entry.getKey(), entry.getValue());
			}
		}
	}

	public void removePrefixes(PrefixMap map) {
		synchronized (lock) {
			Map<String, IRI> mapping = map.getMapping();
			for (Map.Entry<String, IRI> entry : mapping.entrySet()) {
				removePrefix(entry.getKey());
			}
		}
	}

	public void removePrefix(String prefix) {
		synchronized (lock) {
			prefixes.delete(prefix);
		}
	}

	public Map<String, IRI> getPrefixes() {
		synchronized (lock) {
			return Collections.unmodifiableMap(prefixes.getMapping());
		}
	}
}
