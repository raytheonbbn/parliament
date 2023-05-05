package com.bbn.parliament.kb_graph.index.spatial.sql.postgres;

import com.bbn.parliament.kb_graph.index.spatial.Operation;
import com.bbn.parliament.kb_graph.index.spatial.sql.SQLOp;

public abstract class QueryFactory {
	public abstract SQLOp getOperator(Operation operation);
}
