package io.opensaber.registry.util;

import io.opensaber.registry.sink.DatabaseProvider;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class helps to create index of unique or non-unique type. Must set the
 * values for unique index & non-unique index fields
 *
 */
public class Indexer {
    private static Logger logger = LoggerFactory.getLogger(Indexer.class);

    /**
     * Unique index fields
     */
    private List<String> indexUniqueFields;
    /**
     * Composite index fields
     */
    private List<String> compositeIndexFields;
    /**
     * Single index fields
     */
    private List<String> singleIndexFields;
    private DatabaseProvider databaseProvider;

    public Indexer(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    /**
     * Required to set unique fields to create
     * 
     * @param indexUniqueFields
     */
    public void setUniqueIndexFields(List<String> indexUniqueFields) {
        this.indexUniqueFields = indexUniqueFields;
    }
    /**
     * Required to set single fields to create
     * 
     * @param indexUniqueFields
     */
    public void setSingleIndexFields(List<String> singleIndexFields) {
        this.singleIndexFields = singleIndexFields;
    }
    /**
     * Required to set composite fields to create
     * 
     * @param indexUniqueFields
     */
    public void setCompositeIndexFields(List<String> compositeIndexFields) {
        this.compositeIndexFields = compositeIndexFields;
    }

    /**
     * Creates index for a given label
     * 
     * @param graph
     * @param label     type vertex label (example:Teacher) and table in rdbms           
     * @param parentVertex
     */
    public void createIndex(Graph graph, String label, Vertex parentVertex) throws Exception{

        if (label != null && !label.isEmpty()) {
            createUniqueIndex(graph, label, parentVertex);
            createSingleIndex(graph, label, parentVertex);
            createCompositeIndex(graph, label, parentVertex);
        } else {
            logger.info("label is required for creating indexing");
        }
    }
    /**
     * Creates single indices
     * @param graph
     * @param label
     * @param parentVertex
     * @throws NoSuchElementException
     */
    private void createSingleIndex(Graph graph, String label, Vertex parentVertex) throws NoSuchElementException {
        databaseProvider.createIndex(graph, label, singleIndexFields);        

    }
    /**
     * Creates composite indices
     * @param graph
     * @param label
     * @param parentVertex
     * @throws NoSuchElementException
     */
    private void createCompositeIndex(Graph graph, String label, Vertex parentVertex) throws NoSuchElementException {
        databaseProvider.createCompositeIndex(graph, label, compositeIndexFields);
    }
    
    /**
     * Creates only unique indices
     * @param graph
     * @param label
     * @param parentVertex
     * @throws NoSuchElementException
     */
    private void createUniqueIndex(Graph graph, String label, Vertex parentVertex) throws NoSuchElementException {
        databaseProvider.createUniqueIndex(graph, label, indexUniqueFields);
    }
    
}
