package io.opensaber.registry.operators;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.APIMessage;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.util.KeycloakAdminUtil;
import io.opensaber.validators.IValidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InviteOperator implements Operator {
    private static Logger logger = LoggerFactory.getLogger(InviteOperator.class);
    @Autowired
    private IValidate validationService;

    @Autowired
    private KeycloakAdminUtil keycloakAdminUtil;

    @Autowired
    private RegistryHelper registryHelper;

    @Override
    public String execute(APIMessage apiMessage) throws Exception {
        String entityType = apiMessage.getRequest().getEntityType();
        JsonNode rootNode = apiMessage.getRequest().getRequestMapNode();
        String entitySubject = validationService.getEntitySubject(entityType, rootNode);
        String userID = keycloakAdminUtil.createUser(entitySubject, "facility admin");
        logger.info("Owner user_id : " + userID);
        return registryHelper.inviteEntity(rootNode, apiMessage.getUserID(), userID);
    }
}
