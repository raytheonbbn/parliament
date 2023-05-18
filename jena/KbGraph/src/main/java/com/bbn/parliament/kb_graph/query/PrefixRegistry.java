package com.bbn.parliament.kb_graph.query;

import java.util.Map;

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
			map.getMapping().entrySet().stream()
				.forEach(entry -> prefixes.add(entry.getKey(), entry.getValue()));
		}
	}

	public void removePrefix(String prefix) {
		synchronized (lock) {
			prefixes.delete(prefix);
		}
	}

	public void removePrefixes(PrefixMap map) {
		synchronized (lock) {
			map.getMapping().keySet().stream()
				.forEach(this::removePrefix);
		}
	}

	public Map<String, String> getPrefixes() {
		synchronized (lock) {
			return prefixes.getMappingCopy();
		}
	}
}
