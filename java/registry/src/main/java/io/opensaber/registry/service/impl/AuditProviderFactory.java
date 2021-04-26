package io.opensaber.registry.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.registry.service.IAuditService;

/**
 * Audit service implementation for audit layer in the application
 */
@Component
public class AuditProviderFactory {

    private static Logger logger = LoggerFactory.getLogger(AuditProviderFactory.class);
   
    private Map<String, IAuditService> auditServiceMap  = new HashMap<String, IAuditService>();
    
	@Autowired
	List<IAuditService> auditServiceList ;
	
	@Autowired
    public void setAuditServiceList(List<IAuditService> auditServiceList){
        this.auditServiceList = auditServiceList;
    }

	@PostConstruct
	private void init(){
		for (IAuditService auditService : auditServiceList) {
			auditServiceMap.put(auditService.getAuditProvider(), auditService);
		}
	}
	public IAuditService getAuditService(String auditProvider) {
		IAuditService auditService =auditServiceMap.get(auditProvider);
		return auditService;
	}
  
}
