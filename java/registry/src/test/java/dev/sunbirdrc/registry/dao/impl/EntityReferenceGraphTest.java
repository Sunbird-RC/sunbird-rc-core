package dev.sunbirdrc.registry.dao.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.Filter;
import dev.sunbirdrc.pojos.FilterOperators;
import dev.sunbirdrc.pojos.SearchQuery;
import dev.sunbirdrc.registry.dao.RegistryDaoImpl;
import dev.sunbirdrc.registry.dao.SearchDaoImpl;
import dev.sunbirdrc.registry.dao.VertexReader;
import dev.sunbirdrc.registry.dao.VertexWriter;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.sink.DBProviderFactory;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.OSResourceLoader;
import dev.sunbirdrc.registry.util.ReadConfigurator;
import dev.sunbirdrc.registry.util.ReadConfiguratorFactory;
import dev.sunbirdrc.registry.util.RefLabelHelper;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_LIST;
import static org.junit.Assert.*;

/**
 * Graph-level tests for entityReferences: reverse read (expand), cascade delete and search.
 * The graph is TinkerGraph, so the SqlG per-label table problem that motivated the
 * ref_<DeclaringType>_<field> edge label cannot be reproduced here.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DefinitionsManager.class, ObjectMapper.class, DBProviderFactory.class, DBConnectionInfoMgr.class, OSResourceLoader.class })
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EntityReferenceGraphTest {
    private static final String UUID_PROPERTY = "tid";
    private static final String FIELD = "targetOsid";

    private Graph graph;
    private DatabaseProvider databaseProvider;
    private VertexWriter vertexWriter;
    private RegistryDaoImpl registryDao;

    @Autowired
    private DefinitionsManager definitionsManager;
    @Autowired
    private DBProviderFactory dbProviderFactory;
    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    @Before
    public void setUp() throws IOException {
        dbConnectionInfoMgr.setUuidPropertyName(UUID_PROPERTY);
        databaseProvider = dbProviderFactory.getInstance(null);
        graph = databaseProvider.getOSGraph().getGraphStore();
        vertexWriter = new VertexWriter(graph, databaseProvider, UUID_PROPERTY);
        registryDao = new RegistryDaoImpl(databaseProvider, definitionsManager, UUID_PROPERTY, false);
        register("RefTarget", "{}");
        register("RefChild", "{\"entityReferences\":{\"" + FIELD + "\":\"RefTarget\"},\"cascadeDeleteReferences\":[\"" + FIELD + "\"]}");
        register("RefLoose", "{\"entityReferences\":{\"" + FIELD + "\":\"RefTarget\"}}");
        register("RefSingle", "{\"entityReferences\":{\"" + FIELD + "\":\"RefTarget\"},\"oneToOneReferences\":[\"" + FIELD + "\"]}");
    }

    @Test
    public void readWithoutExpandInlinesNothing() throws Exception {
        Vertex target = entity("RefTarget");
        reference(entity("RefChild"), target);

        JsonNode read = read(target);

        assertFalse(read.has("RefChild"));
    }

    @Test
    public void readWithExpandInlinesReferencersAsArray() throws Exception {
        Vertex target = entity("RefTarget");
        reference(entity("RefChild"), target);
        reference(entity("RefChild"), target);

        JsonNode read = read(target, "RefChild");

        assertTrue(read.get("RefChild").isArray());
        assertEquals(2, read.get("RefChild").size());
    }

    @Test
    public void readInlinesOnlyTheRequestedTypes() throws Exception {
        Vertex target = entity("RefTarget");
        reference(entity("RefChild"), target);
        reference(entity("RefLoose"), target);

        JsonNode read = read(target, "RefChild");

        assertTrue(read.has("RefChild"));
        assertFalse(read.has("RefLoose"));
    }

    @Test
    public void oneToOneReferencerIsInlinedAsObject() throws Exception {
        Vertex target = entity("RefTarget");
        reference(entity("RefSingle"), target);

        JsonNode read = read(target, "RefSingle");

        assertTrue(read.get("RefSingle").isObject());
    }

    @Test
    public void readingTheReferencerDoesNotPullInItsTarget() throws Exception {
        Vertex target = entity("RefTarget");
        Vertex child = entity("RefChild");
        reference(child, target);

        JsonNode read = read(child, "RefTarget");

        assertFalse(read.has("RefTarget"));
    }

    @Test
    public void deletingTargetCascadesOnlyToCascadeDeleteReferencers() {
        Vertex target = entity("RefTarget");
        Vertex cascading = entity("RefChild");
        Vertex loose = entity("RefLoose");
        reference(cascading, target);
        reference(loose, target);

        registryDao.deleteEntity(target);

        assertTrue(isInactive(target));
        assertTrue(isInactive(cascading));
        assertFalse(isInactive(loose));
    }

    @Test
    public void deletingReferencerNeverDeletesItsTarget() {
        Vertex target = entity("RefTarget");
        Vertex child = entity("RefChild");
        reference(child, target);

        registryDao.deleteEntity(child);

        assertTrue(isInactive(child));
        assertFalse(isInactive(target));
    }

    @Test
    public void hardDeletingTargetRemovesCascadeReferencersOnly() {
        Vertex target = entity("RefTarget");
        Vertex cascading = entity("RefChild");
        Vertex loose = entity("RefLoose");
        String cascadingId = databaseProvider.getId(cascading);
        String looseId = databaseProvider.getId(loose);
        reference(cascading, target);
        reference(loose, target);

        registryDao.hardDeleteEntity(target);

        assertFalse(graph.traversal().V().has(UUID_PROPERTY, cascadingId).hasNext());
        assertTrue(graph.traversal().V().has(UUID_PROPERTY, looseId).hasNext());
    }

    @Test
    public void cascadeStopsOnReferenceCycles() {
        Vertex a = entity("RefChild");
        Vertex b = entity("RefChild");
        // Two cascading entities pointing at each other must not recurse forever.
        reference(a, b);
        reference(b, a);

        registryDao.deleteEntity(a);

        assertTrue(isInactive(a));
        assertTrue(isInactive(b));
    }

    @Test
    public void searchInlinesReferencersOnlyWhenExpanded() {
        String name = "target-" + UUID.randomUUID();
        Vertex target = entity("RefTarget");
        target.property("name", name);
        reference(entity("RefChild"), target);
        SearchDaoImpl searchDao = new SearchDaoImpl(registryDao);
        SearchQuery query = new SearchQuery(Collections.singletonList("RefTarget"), 0, 100);
        query.setFilters(Collections.singletonList(new Filter("name", FilterOperators.eq, name)));

        JsonNode plain = searchDao.search(graph, query, true).get("RefTarget").get(ENTITY_LIST).get(0);
        JsonNode expanded = searchDao.search(graph, query, true, Collections.singleton("RefChild"))
                .get("RefTarget").get(ENTITY_LIST).get(0);

        assertFalse(plain.has("RefChild"));
        assertEquals(1, expanded.get("RefChild").size());
    }

    private void register(String title, String osConfig) throws IOException {
        if (definitionsManager.isValidEntityName(title)) {
            return;
        }
        definitionsManager.appendNewDefinition(new Definition(new ObjectMapper().readTree(
                "{\"$schema\":\"http://json-schema.org/draft-07/schema\",\"type\":\"object\","
                        + "\"properties\":{\"" + title + "\":{\"$ref\":\"#/definitions/" + title + "\"}},"
                        + "\"required\":[\"" + title + "\"],\"title\":\"" + title + "\","
                        + "\"definitions\":{\"" + title + "\":{\"$id\":\"#/properties/" + title + "\",\"type\":\"object\","
                        + "\"properties\":{\"name\":{\"type\":\"string\"}}}},"
                        + "\"_osConfig\":" + osConfig + "}")));
    }

    private Vertex entity(String label) {
        Vertex vertex = vertexWriter.createVertex(label);
        vertex.property("name", label + "-" + UUID.randomUUID());
        return vertex;
    }

    /** referencer -> target, marked and labelled the way RegistryServiceImpl writes it. */
    private void reference(Vertex referencer, Vertex target) {
        Edge edge = referencer.addEdge(RefLabelHelper.getReferenceEdgeLabel(referencer.label(), FIELD), target);
        edge.property(RefLabelHelper.REFERENCE_EDGE_MARKER, true);
    }

    private JsonNode read(Vertex vertex, String... expand) throws Exception {
        ReadConfigurator configurator = ReadConfiguratorFactory.getOne(false);
        configurator.setExpandReferencers(new HashSet<>(Arrays.asList(expand)));
        VertexReader reader = new VertexReader(databaseProvider, graph, configurator, UUID_PROPERTY, definitionsManager, false);
        return reader.read(vertex.label(), databaseProvider.getId(vertex)).get(vertex.label());
    }

    private boolean isInactive(Vertex vertex) {
        return vertex.property(Constants.STATUS_KEYWORD).isPresent()
                && Constants.STATUS_INACTIVE.equals(vertex.property(Constants.STATUS_KEYWORD).value());
    }
}
