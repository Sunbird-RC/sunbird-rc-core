package dev.sunbirdrc.registry.util;

import dev.sunbirdrc.registry.exception.IndexException;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

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
    private List<String> compositeUniqueIndexFields;
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
     * @param singleIndexFields
     */
    public void setSingleIndexFields(List<String> singleIndexFields) {
        this.singleIndexFields = singleIndexFields;
    }
    /**
     * Required to set composite fields to create
     *
     * @param compositeIndexFields
     */
    public void setCompositeIndexFields(List<String> compositeIndexFields) {
        this.compositeIndexFields = compositeIndexFields;
    }

    public void setCompositeUniqueIndexFields(List<String> compositeUniqueIndexFields) {
        this.compositeUniqueIndexFields = compositeUniqueIndexFields;
    }

    /**
     * Creates index for a given label
     *
     * @param graph
     * @param label     type vertex label (example:Teacher) and table in rdbms
     */
    public void createIndex(Graph graph, String label) throws IndexException.LabelNotFoundException {
        if (label != null && !label.isEmpty()) {
            databaseProvider.createUniqueIndex(graph, label, indexUniqueFields);
            databaseProvider.createIndex(graph, label, singleIndexFields);
            databaseProvider.createCompositeIndex(graph, label, compositeIndexFields);
            databaseProvider.createCompositeUniqueIndex(graph, label, compositeUniqueIndexFields);
        } else {
            throw new IndexException.LabelNotFoundException(label);
        }
    }
}
