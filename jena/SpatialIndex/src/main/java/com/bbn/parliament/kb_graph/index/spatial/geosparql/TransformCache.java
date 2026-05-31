package com.bbn.parliament.kb_graph.index.spatial.geosparql;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;

public class TransformCache {
	private static final int CACHE_SIZE = 10;

	private LinkedHashMap<CoordinateReferenceSystem, LinkedHashMap<CoordinateReferenceSystem, MathTransform>> cache;

	public TransformCache() {
		this.cache = new LinkedHashMap<>(CACHE_SIZE) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(
				Entry<CoordinateReferenceSystem, LinkedHashMap<CoordinateReferenceSystem, MathTransform>> eldest) {
				return (size() > CACHE_SIZE);
			}
		};
	}

	public MathTransform getTransform(CoordinateReferenceSystem source, CoordinateReferenceSystem target) throws FactoryException {
		LinkedHashMap<CoordinateReferenceSystem, MathTransform> transforms = cache.get(source);
		if (null == transforms) {
			transforms = new LinkedHashMap<>(CACHE_SIZE) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(
					Entry<CoordinateReferenceSystem, MathTransform> eldest) {
					return (size() > CACHE_SIZE);
				}
			};
			cache.put(source, transforms);
		}
		MathTransform transform = transforms.get(target);
		if (null == transform) {
			transform = CRS.findMathTransform(source, target);
			transforms.put(target, transform);
		}
		return transform;
	}
}
