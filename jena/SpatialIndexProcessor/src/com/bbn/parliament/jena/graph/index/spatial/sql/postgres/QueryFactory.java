package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.sql.SQLOp;

public abstract class QueryFactory {
   public abstract SQLOp getOperator(Operation operation);
}
