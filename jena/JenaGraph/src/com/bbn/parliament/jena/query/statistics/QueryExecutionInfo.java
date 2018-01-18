package com.bbn.parliament.jena.query.statistics;

import com.hp.hpl.jena.query.Query;

public class QueryExecutionInfo {
   private long creationTime;
   private Query query;

   public QueryExecutionInfo(Query query) {
      this.query = query;
      this.creationTime = System.currentTimeMillis();
   }

   public long getCreationTime() {
      return creationTime;
   }

   public Query getQuery() {
      return query;
   }

   public long getAge() {
      return System.currentTimeMillis() - creationTime;
   }
}
