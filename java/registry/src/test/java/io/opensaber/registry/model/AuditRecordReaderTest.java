package io.opensaber.registry.model;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.audit.LabelCannotBeNullException;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.jena.reasoner.IllegalParameterException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuditRecordReaderTest {

    private AuditRecordReader auditRecordReader;
    private DatabaseProvider databaseProviderMock;
    private Graph graphMock;
    private GraphTraversalSource graphTraversalSourceMock;
    private GraphTraversalSource graphTraversalSourceCloneMock;
    private GraphTraversal VMock;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    private GraphTraversal hasLabelMock;
    private GraphTraversal traversalMock;

    @Before
    public void setUp() throws Exception {
        this.databaseProviderMock = mock(DatabaseProvider.class);
        this.auditRecordReader = new AuditRecordReader(databaseProviderMock);
        this.graphMock = mock(Graph.class);
        this.graphTraversalSourceMock = mock(GraphTraversalSource.class);
        this.graphTraversalSourceCloneMock = mock(GraphTraversalSource.class);
        this.VMock = mock(GraphTraversal.class);
        this.hasLabelMock = mock(GraphTraversal.class);
        this.traversalMock = mock(GraphTraversal.class);
        when(databaseProviderMock.getGraphStore()).thenReturn(graphMock);
        when(graphMock.traversal()).thenReturn(graphTraversalSourceMock);
        when(graphTraversalSourceMock.clone()).thenReturn(graphTraversalSourceCloneMock);
        when(graphTraversalSourceCloneMock.V()).thenReturn(VMock);
        auditRecordReader = new AuditRecordReader(databaseProviderMock);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testNullLabel() throws LabelCannotBeNullException {
        expectedEx.expect(LabelCannotBeNullException.class);
        auditRecordReader.fetchAuditRecords(null,null);
    }

    @Test
    public void testFetchingUnMatchedLabel() throws LabelCannotBeNullException {
        when(VMock.hasLabel("X-AUDIT")).thenReturn(hasLabelMock);
        when(hasLabelMock.out("record")).thenReturn(traversalMock);
        when(traversalMock.hasNext()).thenReturn(false);
        List<AuditRecord> auditRecords = auditRecordReader.fetchAuditRecords("X",null);
        assertNotNull(auditRecords);
        assertEquals(0,auditRecords.size());
    }

    @Test
    public void testSingleAuditRecordMatch() throws LabelCannotBeNullException {
        when(VMock.hasLabel("X-AUDIT")).thenReturn(hasLabelMock);
        when(hasLabelMock.out("record")).thenReturn(traversalMock);
        Vertex auditVertex1 = mock(Vertex.class);
        VertexProperty predicate1 = mock(VertexProperty.class);
        VertexProperty oldObject1 = mock(VertexProperty.class);
        VertexProperty newObject1 = mock(VertexProperty.class);
        when(auditVertex1.property("predicate")).thenReturn(predicate1);
        when(predicate1.value()).thenReturn("PREDICATE1");
        when(auditVertex1.property("oldObject")).thenReturn(oldObject1);
        when(oldObject1.value()).thenReturn("OLDOBJECT1");
        when(auditVertex1.property("newObject")).thenReturn(newObject1);
        when(newObject1.value()).thenReturn("NEWOBJECT1");
        when(traversalMock.hasNext()).thenReturn(true,false);
        when(traversalMock.next()).thenReturn(auditVertex1);
        List<AuditRecord> auditRecords = auditRecordReader.fetchAuditRecords("X",null);
        assertEquals(1,auditRecords.size());
        AuditRecord record = auditRecords.get(0);
        assertEquals(record.getSubject(),"X-AUDIT");
        assertEquals(record.getPredicate(),"PREDICATE1");
        assertEquals(record.getOldObject(),"OLDOBJECT1");
        assertEquals(record.getNewObject(),"NEWOBJECT1");
    }

    @Test
    public void testPredicateButNoMatch() throws LabelCannotBeNullException {
        when(VMock.hasLabel("X-AUDIT")).thenReturn(hasLabelMock);
        GraphTraversal tempTraversalMock = mock(GraphTraversal.class);
        when(hasLabelMock.out("record")).thenReturn(tempTraversalMock);
        when(tempTraversalMock.has("predicate","P")).thenReturn(traversalMock);
        List<AuditRecord> auditRecords = auditRecordReader.fetchAuditRecords("X","P");
        when(traversalMock.hasNext()).thenReturn(false);
        assertEquals(0,auditRecords.size());
    }

    @Test
    public void testPredicateMatchButOneRecord() throws LabelCannotBeNullException {
        when(VMock.hasLabel("X-AUDIT")).thenReturn(hasLabelMock);
        GraphTraversal tempTraversalMock = mock(GraphTraversal.class);
        when(hasLabelMock.out("record")).thenReturn(tempTraversalMock);
        when(tempTraversalMock.has("predicate","PREDICATE1")).thenReturn(traversalMock);
        Vertex auditVertex1 = mock(Vertex.class);
        VertexProperty predicate1 = mock(VertexProperty.class);
        VertexProperty oldObject1 = mock(VertexProperty.class);
        VertexProperty newObject1 = mock(VertexProperty.class);
        when(auditVertex1.property("predicate")).thenReturn(predicate1);
        when(predicate1.value()).thenReturn("PREDICATE1");
        when(auditVertex1.property("oldObject")).thenReturn(oldObject1);
        when(oldObject1.value()).thenReturn("OLDOBJECT1");
        when(auditVertex1.property("newObject")).thenReturn(newObject1);
        when(newObject1.value()).thenReturn("NEWOBJECT1");
        when(traversalMock.hasNext()).thenReturn(true,false);
        when(traversalMock.next()).thenReturn(auditVertex1);
        List<AuditRecord> auditRecords = auditRecordReader.fetchAuditRecords("X","PREDICATE1");
        assertEquals(1,auditRecords.size());
    }
}