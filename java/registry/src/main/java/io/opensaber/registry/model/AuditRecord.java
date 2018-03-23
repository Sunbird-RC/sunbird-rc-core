package io.opensaber.registry.model;

import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.json.JSONObject;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component

public class AuditRecord {
    private String subject;
    private String predicate;
    private String oldObject;
    private String newObject;
    private String readOnlyAuthInfo;

    public AuditRecord subject(String label) {
        this.subject = label+"-AUDIT";
        return this;
    }

    public AuditRecord predicate(String key) {
        this.predicate = key;
        return this;
    }

    public AuditRecord oldObject(Object oldValue) {
        this.oldObject = String.valueOf(oldValue);
        return this;
    }

    public AuditRecord newObject(Object newValue) {
        this.newObject = String.valueOf(newValue);
        return this;
    }

    @Override
    public String toString() {
        return "AuditRecord{" +
                "subject='" + subject + '\'' +
                ", predicate='" + predicate + '\'' +
                ", oldObject=" + oldObject +
                ", newObject=" + newObject +
                ", readOnlyAuthInfo=" + readOnlyAuthInfo +
                '}';
    }

    public void record(DatabaseProvider provider) throws AuditFailedException {
//        System.out.println("AUDITING as "+subject);
        GraphTraversalSource _source = provider.getGraphStore().traversal().clone();
        boolean rootNodeExists = _source.V().hasLabel(subject).hasNext();
        Vertex rootVertex;
        if(!rootNodeExists){
//            System.out.println("AUDIT ROOT NOT FOUND - CREATING");
            rootVertex = _source.addV(subject).next();
            updateUserInfo(rootVertex);
        } else {
//            System.out.println("AUDIT ROOT FOUND - NOT CREATING");
            rootVertex = _source.V().hasLabel(subject).next();
            rootVertex.property("@audit","true");
        }
        Vertex recordVertex = _source.addV("auditRecord").next();
        recordVertex.property("predicate",this.predicate);
        recordVertex.property("oldObject",this.oldObject);
        recordVertex.property("newObject",this.newObject);
        recordVertex.property("@audit",true);
        recordVertex.property("@auditRecord",true);
        updateUserInfo(recordVertex);
        rootVertex.addEdge("audit",recordVertex).property("@audit",true);
        System.out.println(this);
    }

    private void updateUserInfo(Vertex vertex) {
        String authinfo = new JSONObject( getCurrentUserInfo() ).toString();
        vertex.property("authInfo",authinfo);
    }

    public String getPredicate() {
        return predicate;
    }

    public Object getOldObject() {
        return oldObject;
    }

    public Object getNewObject() {
        return newObject;
    }

    public String getSubject() {
        return subject;
    }

    private AuthInfo getCurrentUserInfo(){
        return (AuthInfo) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public void readOnlyAuthInfo(String authInfo) {
        this.readOnlyAuthInfo=authInfo;
    }
}

