package com.bbn.parliament.jena.query.statistics;

import java.util.HashMap;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.Index;

public class IndexExecutionInfo {
	private static Map<Index<?>, IndexExecutionInfo> registry = new HashMap<>();

	public static IndexExecutionInfo register(Index<?> index) {
		IndexExecutionInfo info = new IndexExecutionInfo(index);
		registry.put(index, info);
		return info;
	}

	public static IndexExecutionInfo getInfo(Index<?> index) {
		IndexExecutionInfo info = registry.get(index);
		if (null == info) {
			info = register(index);
		}
		return info;
	}

	private long queryCount;
	private long executeTime;
	private long hasNextTime;
	private long nextTime;
	private Index<?> index;

	private IndexExecutionInfo(Index<?> index) {
		this.index = index;
		this.queryCount = 0;
		this.executeTime = 0;
		this.hasNextTime = 0;
		this.nextTime = 0;
	}

	public long getQueryCount() {
		return queryCount;
	}

	public long getExecuteTime() {
		return executeTime;
	}

	public long getHasNextTime() {
		return hasNextTime;
	}

	public long getNextTime() {
		return nextTime;
	}

	public Index<?> getIndex() {
		return index;
	}

	public void incrementQueryCount() {
		queryCount++;
	}

	public void addExecutionTime(long time) {
		executeTime += time;
	}

	public void addHasNextTime(long time) {
		hasNextTime += time;
	}

	public void addNextTime(long time) {
		nextTime += time;
	}
}
