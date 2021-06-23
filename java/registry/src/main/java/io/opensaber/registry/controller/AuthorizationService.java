package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.helper.RegistryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class AuthorizationService {
    private final RegistryHelper registryHelper;
    private final String uuidPropertyName;

    @Autowired
    public AuthorizationService(RegistryHelper registryHelper, @Value("${database.uuidPropertyName}") String uuidPropertyName) {
        this.registryHelper = registryHelper;
        this.uuidPropertyName = uuidPropertyName;
    }

    public void authorize(String entityName, String entityId, HttpServletRequest request) throws Exception {
        JsonNode userDetails = registryHelper.getRequestedUserDetails(request, entityName);
        if(!userDetails.get(uuidPropertyName).get(uuidPropertyName).asText().equals(entityId)) {
            throw new Exception("User is trying to operate someone's data");
        }
    }
}
