package com.bbn.parliament.odda;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class AnnotationExclusionStrategy implements ExclusionStrategy {
	@Override
	public boolean shouldSkipClass(Class<?> cls) {
		return false;
	}

	@Override
	public boolean shouldSkipField(FieldAttributes fa) {
		return fa.getAnnotation(ExcludeFromJson.class) != null;
	}
}
