package io.opensaber.registry.model;

import io.opensaber.registry.exception.audit.LabelCannotBeNullException;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

@Component
public class AuditRecordReader {
    private DatabaseProvider databaseProvider;
 
	@Value("${registry.system.base}")
	private String registrySystemContext;
    
    @Autowired
    private Environment env;
    
    @Autowired
    AuditRecord record;

    public AuditRecordReader(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }
    public AuditRecordReader() {
        this.databaseProvider = databaseProvider;
    }
    
    public void setDatabaseProvider(DatabaseProvider databaseProvider) {
		this.databaseProvider = databaseProvider;
	} 
    
    public List<AuditRecord> fetchAuditRecords(String label, String predicate) throws LabelCannotBeNullException {
       	List<AuditRecord> records = new ArrayList<AuditRecord>();
        if(label==null) throw new LabelCannotBeNullException("Label cannot be null");
        GraphTraversalSource traversalSource = databaseProvider.getGraphStore().traversal();
        GraphTraversal<Vertex, Vertex> traversal;
        if(predicate!=null){
            traversal = traversalSource.clone().V().hasLabel(getAuditLabel(label)).out(registrySystemContext+"audit").has(registrySystemContext+"predicate",predicate);
        } else {
            traversal = traversalSource.clone().V().hasLabel(getAuditLabel(label)).out(registrySystemContext+"audit");
        }
        while(traversal.hasNext()){
            Vertex auditVertex = traversal.next();
            record.subject(label);
            record.predicate(getValue(auditVertex,registrySystemContext+"predicate"));
            record.oldObject(getValue(auditVertex,registrySystemContext+"oldObject"));
            record.newObject(getValue(auditVertex,registrySystemContext+"newObject"));
            record.readOnlyAuthInfo(getValue(auditVertex,registrySystemContext+"authInfo"));
            records.add(record);
        }
        return records;
    }

    private String getValue(Vertex auditVertex, String key) {
        VertexProperty property = auditVertex.property(key);
        String value = "";
        if(property.isPresent()){
            Object object = property.value();
            if(object!=null){
                value = object.toString();
            }
        }
        return value;
    }

    private String getAuditLabel(String label) {
    	return label+"-AUDIT";
    }
}
