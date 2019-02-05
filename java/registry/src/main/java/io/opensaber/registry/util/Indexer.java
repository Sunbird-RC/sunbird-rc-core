package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;
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
     * Non unique index fields 
     */
    private List<String> indexFields;
    /**
     * Unique index fields
     */
    private List<String> indexUniqueFields;
    private DatabaseProvider databaseProvider;

    public Indexer(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    /**
     * Required to set non-unique fields to create
     * 
     * @param indexFields
     */
    public void setIndexFields(List<String> indexFields) {
        this.indexFields = indexFields;
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
     * Creates index for a given label
     * 
     * @param graph
     * @param label     type vertex label (example:Teacher) and table in rdbms           
     * @param parentVertex
     */
    public void createIndex(Graph graph, String label, Vertex parentVertex) throws NoSuchElementException {
        if (label != null && !label.isEmpty()) {
            createNonUniqueIndex(graph, label, parentVertex);
            createUniqueIndex(graph, label, parentVertex);
        } else {
            logger.info("label is required for creating indexing");
        }
    }

    private void createNonUniqueIndex(Graph graph, String label, Vertex parentVertex) throws NoSuchElementException {

        databaseProvider.createIndex(graph, label, indexFields);
        updateIndices(parentVertex, indexFields, false);

    }

    private void createUniqueIndex(Graph graph, String label, Vertex parentVertex) throws NoSuchElementException {

        databaseProvider.createUniqueIndex(graph, label, indexUniqueFields);
        updateIndices(parentVertex, indexUniqueFields, true);

    }

    /**
     * Append the values to parent vertex INDEX_FIELDS and UNIQUE_INDEX_FIELDS
     * property
     * 
     * @param parentVertex
     * @param values
     * @param isUnique
     */
    private void updateIndices(Vertex parentVertex, List<String> values, boolean isUnique) {
        String propertyName = isUnique ? Constants.UNIQUE_INDEX_FIELDS : Constants.INDEX_FIELDS;

        if (values.size() > 0) {
            String existingValue = (String) parentVertex.property(propertyName).value();
            for (String value : values) {
                existingValue = existingValue.isEmpty() ? value : (existingValue + "," + value);
                parentVertex.property(propertyName, existingValue);
            }
            logger.info("parent vertex property {}:{}", propertyName, existingValue);
        } else {
            logger.info("no values to set for parent vertex property for {}", propertyName);
        }
    }

}
