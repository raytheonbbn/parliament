package com.bbn.parliament.jena.graph.index;

import java.io.File;
import java.util.Properties;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.util.FileUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

/**
 * A factory for creating {@link Index}es. The <code>IndexFactory</code> is used
 * when Parliament wants to create an index for a given graph.
 *
 * @author rbattle
 *
 * @param <I>
 *           The type of <code>Index</code>
 * @param <T>
 *           The type of object that is indexed
 */
public abstract class IndexFactory<I extends Index<T>, T> {

   private String label;

   /**
    * Create a new instance.
    *
    * @param label
    *           a label describing the type of index.
    */
   public IndexFactory(String label) {
      this.label = label;
   }

   /**
    * Get the label.
    *
    * @return the label.
    */
   public String getLabel() {
      return label;
   }

   /**
    * Configure this instance.
    *
    * @param configuration
    *           configuration properties.
    */
   public abstract void configure(Properties configuration);

   /**
    * Create an index. The returned index may be closed. This uses the
    * {@link IndexFactoryHelper} to get the root index directory.
    *
    * @param graph
    *           the graph to index
    * @param graphName
    *           the name of the graph
    *
    * @return an index.
    */
   public I createIndex(Graph graph, Node graphName) {
      String indexDir = IndexFactoryHelper.getIndexDirectory(graph, graphName);
      return createIndex(graph, graphName, indexDir);
   }

   /**
    * Create an index. The returned index may be closed.
    *
    * @param graph
    *           the graph to index
    * @param graphName
    *           the name of the graph
    * @param indexDir
    *           the directory to store the index
    * @return an index.
    */
   public abstract I createIndex(Graph graph, Node graphName, String indexDir);

   /**
    * A helper class for indexes.
    *
    * @author rbattle
    *
    */
   public static final class IndexFactoryHelper {
      public IndexFactoryHelper() {

      }

      /**
       * Get the directory that contains all of the indexes for the graph. In
       * the case of {@link KbGraph}s, the location of the graph is use as the
       * base for the index directory. In all other cases, the URI of the graph
       * is formatted as a directory name. The current working directory is used
       * for the default graph.
       *
       * @param graph
       *           a graph
       * @param graphName
       *           the name of the graph
       * @return the root index directory.
       */
      public static String getIndexDirectory(Graph graph, Node graphName) {
         String dir = null;
         if (graph instanceof KbGraph) {
            KbGraph kbg = (KbGraph) graph;
//            String relativeDir = kbg.getRelativeDirectory();
            dir = kbg.getConfig().m_kbDirectoryPath; // m_kbDirectoryPath includes the relative directory...
//            dir = kbg.getConfig().m_kbDirectoryPath + File.separator
//                  + ((null == relativeDir) ? "" : relativeDir);
         } else if (null == graphName
               || graphName.getURI().equals(KbGraphStore.DEFAULT_GRAPH_URI)) {
            dir = ".";
         } else {
            dir = FileUtil.encodeStringForFilename(graphName.getURI());
         }

         if (null == dir || dir.isEmpty()) {
            dir = ".";
         }

         if (dir.endsWith(File.separator)) {
            dir = dir + "indexes";
         } else {
            dir = String.format("%s%cindexes", dir, File.separatorChar,
                                "indexes");
         }

         return dir;
      }
   }
}