package io.opensaber.registry.model;

import com.google.gson.Gson;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AuditRecord {
	private String subject;
    private String predicate;
    private Object oldObject;
    private Object newObject;
    private String readOnlyAuthInfo;
    
    @Value("${registry.system.base}")
	private String registrySystemContext;

    @Value("${authentication.enabled}")
    private boolean authenticationEnabled;

    @Autowired
    private Gson gson;
	
	public AuditRecord subject(String label) {
    	this.subject = label+"-AUDIT";
        return this;
    }

    public AuditRecord predicate(String key) {
        this.predicate = key;
        return this;
    }

    public AuditRecord oldObject(Object oldValue) {
    	if(oldValue instanceof List){
    		this.oldObject = (List)oldValue;
    	}else {
    		this.oldObject = String.valueOf(oldValue);
    	}
        return this;
    }

    public AuditRecord newObject(Object newValue) {
        if(newValue instanceof List){
    		this.newObject = (List)newValue;
    	}else {
    		this.newObject = String.valueOf(newValue);
    	}
        return this;
    }

    @Override
    public String toString() {
        return "AuditRecord{" +
        	    "  subject='" + subject + '\'' +
                ", predicate='" + predicate + '\'' +
                ", oldObject=" + oldObject +
                ", newObject=" + newObject +
                ", readOnlyAuthInfo=" + readOnlyAuthInfo +
                '}';
    }

    public void record(DatabaseProvider provider) throws AuditFailedException {

        GraphTraversalSource _source = provider.getGraphStore().traversal().clone();
        boolean rootNodeExists = _source.V().hasLabel(subject).hasNext();
        Vertex rootVertex;
        if(!rootNodeExists){
        	/***"AUDIT ROOT NOT FOUND - CREATING"***/
            rootVertex = _source.addV(subject).next();
            updateUserInfo(rootVertex);
        } else {	
        	/***AUDIT ROOT FOUND - NOT CREATING"**/
            rootVertex = _source.V().hasLabel(subject).next();
            rootVertex.property("@audit","true");
        }
     
        String uuid=UUID.randomUUID().toString();
        String auditLabel=registrySystemContext+uuid;
        String predicate=registrySystemContext+ "predicate";
        String oldObject=registrySystemContext+"oldObject";
        String newObject=registrySystemContext+"newObject";
                 
        Vertex recordVertex = _source.addV(auditLabel).next();
        recordVertex.property(predicate,this.predicate);
        recordVertex.property(oldObject,this.oldObject);
        recordVertex.property(newObject,this.newObject);
        recordVertex.property("@audit","true");
        recordVertex.property("@auditRecord","true");
        updateUserInfo(recordVertex);
      
        String edgeLabel=registrySystemContext+"audit";
        
        rootVertex.addEdge(edgeLabel,recordVertex).property("@audit",true);	
     }

    private void updateUserInfo(Vertex vertex) {
	    if(authenticationEnabled) {
            String authinfo = gson.toJson(getCurrentUserInfo());
            String authInfoLabel = registrySystemContext + "authInfo";
            vertex.property(authInfoLabel, authinfo);
        }
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

