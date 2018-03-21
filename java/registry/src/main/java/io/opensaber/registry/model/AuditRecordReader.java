package io.opensaber.registry.model;

import io.opensaber.registry.exception.audit.LabelCannotBeNullException;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AuditRecordReader {
    private final DatabaseProvider databaseProvider;

    public AuditRecordReader(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    public List<AuditRecord> fetchAuditRecords(String label, String predicate) throws LabelCannotBeNullException {
        System.out.println("FETCH AUDIT RECORD "+label);
        List<AuditRecord> records = new ArrayList<AuditRecord>();
        if(label==null) throw new LabelCannotBeNullException("Label cannot be null");
        GraphTraversalSource traversalSource = databaseProvider.getGraphStore().traversal();
        GraphTraversal<Vertex, Vertex> traversal;
        if(predicate!=null){
            traversal = traversalSource.clone().V().hasLabel(getAuditLabel(label)).out("audit").has("predicate",predicate);
        } else {
            traversal = traversalSource.clone().V().hasLabel(getAuditLabel(label)).out("audit");
        }
        while(traversal.hasNext()){
            Vertex auditVertex = traversal.next();
            AuditRecord record = new AuditRecord();
            record.subject(label);
            record.predicate(auditVertex.property("predicate").value().toString());
            record.oldObject(auditVertex.property("oldObject").value().toString());
            record.newObject(auditVertex.property("newObject").value().toString());
            System.out.println(record);
            records.add(record);
        }
        return records;
    }

    private String getAuditLabel(String label) {
        return label+"-AUDIT";
    }
}
