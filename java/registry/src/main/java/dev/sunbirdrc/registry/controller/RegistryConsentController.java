package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.registry.exception.ConsentForbiddenException;
import dev.sunbirdrc.registry.exception.RecordNotFoundException;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.util.ConsentRequestClient;
import org.jetbrains.annotations.Nullable;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
public class RegistryConsentController extends AbstractController {

    @Autowired
    private ConsentRequestClient consentRequestClient;
    @Autowired
    private RegistryHelper registryHelper;
    @Autowired
    private ObjectMapper objectMapper;
    private static Logger logger = LoggerFactory.getLogger(RegistryConsentController.class);

    private ArrayList<String> getConsentFields(HttpServletRequest request) {
        ArrayList<String> fields = new ArrayList<>();
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        try {
            Map<String, Object> otherClaims = ((KeycloakPrincipal) principal.getPrincipal()).getKeycloakSecurityContext().getToken().getOtherClaims();
            if (otherClaims.keySet().contains(dev.sunbirdrc.registry.Constants.KEY_CONSENT) && otherClaims.get(dev.sunbirdrc.registry.Constants.KEY_CONSENT) instanceof Map) {
                Map consentFields = (Map) otherClaims.get(dev.sunbirdrc.registry.Constants.KEY_CONSENT);
                for (Object key : consentFields.keySet()) {
                    fields.add(key.toString());
                }
            }
        } catch (Exception ex) {
            logger.error("Error while extracting other claims", ex);
        }
        return fields;
    }

    @Nullable
    private ResponseEntity<Object> getJsonNodeResponseEntity(JsonNode userInfoFromRegistry, String entityName, ArrayList<String> consentFields) {
        JsonNode jsonNode = userInfoFromRegistry.get(entityName);
        if (jsonNode instanceof ArrayNode) {
            ArrayNode values = (ArrayNode) jsonNode;
            if (values.size() > 0) {
                JsonNode node = values.get(0);
                if (node instanceof ObjectNode) {
                    ObjectNode entityNode = copyWhiteListedFields(consentFields, node);
                    return new ResponseEntity<>(entityNode, HttpStatus.OK);
                }
            }
        }
        return null;
    }

    private boolean isConsentTimeExpired(String createdAt, String expirationTime) {
        OffsetDateTime currentDateTime = OffsetDateTime.now( ZoneOffset.UTC );
        OffsetDateTime odt = OffsetDateTime.parse(createdAt);
        odt = odt.plus(Long.parseLong(expirationTime), ChronoUnit.SECONDS);
        return odt.compareTo(currentDateTime) < 0;
    }
    private ObjectNode copyWhiteListedFields(ArrayList<String> fields, JsonNode dataNode) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (String key : fields) {
            node.set(key, dataNode.get(key));
        }
        return node;
    }

    @PostMapping(value = "/api/v1/consent")
    public ResponseEntity<Object> createConsent(@RequestBody ObjectNode objectNode, HttpServletRequest request) throws Exception {
        ResponseParams responseParams = new ResponseParams();
        consentRequestClient.addConsent(objectNode, request);
        responseParams.setStatus(Response.Status.SUCCESSFUL);
        return new ResponseEntity<>(responseParams, HttpStatus.OK);
    }

    @GetMapping("/api/v1/consent/{consentId}")
    public ResponseEntity<Object> getConsent(@PathVariable String consentId, HttpServletRequest request) throws Exception {
        JsonNode consent = null;
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        try {
            consent = consentRequestClient.getConsent(consentId);
        } catch (Exception exception) {
            return internalErrorResponse(responseParams, response, exception);
        }
        boolean status = consent.get("status").asText().equals("true");
        String keycloakUserId = registryHelper.getKeycloakUserId(request);
        boolean isOwner = keycloakUserId.equals(consent.get("requestorId").asText());
        try {
            if(isConsentTimeExpired(consent.get("createdAt").asText(), consent.get("expirationTime").asText())) {
                final String consentTimeExpired = "Consent Time Expired";
                throw new ConsentForbiddenException(consentTimeExpired);
            }
            if(!isOwner) {
                final String forbidden = "You are not authorized to access this consent";
                throw new ConsentForbiddenException(forbidden);
            }
        } catch (ConsentForbiddenException consentForbiddenException) {
            return forbiddenExceptionResponse(consentForbiddenException);
        }
        if(status) {
            JsonNode userInfoFromRegistry = null;
            String[] osOwners = consent.get("osOwner").asText().split(",");
            String entityName = consent.get("entityName").asText();
            for(String owner : osOwners) {
                userInfoFromRegistry = consentRequestClient.searchUser(entityName, owner);
                if (userInfoFromRegistry != null)
                    break;
            }
            ArrayList<String> consentFields = new ArrayList<>(objectMapper.convertValue(consent.get("consentFields"), Map.class).keySet());
            ResponseEntity<Object> entityNode = getJsonNodeResponseEntity(userInfoFromRegistry, entityName, consentFields);
            if (entityNode != null) return entityNode;
        }
        final String consentNotGranted = "Consent is rejected or not granted until now";
        return new ResponseEntity<>(consentNotGranted, HttpStatus.OK);
    }

    @RequestMapping(value = "/partner/api/v1/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntityWithConsent(
            @PathVariable String entityName,
            HttpServletRequest request) {
        try {
            ArrayList<String> fields = getConsentFields(request);
            JsonNode userInfoFromRegistry = registryHelper.getRequestedUserDetails(request, entityName);
            ResponseEntity responseEntity = getJsonNodeResponseEntity(userInfoFromRegistry, entityName, fields);
            if(responseEntity != null)
                return responseEntity;
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RecordNotFoundException ex) {
            logger.error("Error in finding the entity", ex);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error in partner api access", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/api/v1/consent/{consentId}")
    public ResponseEntity<Object> grantOrRejectClaim(@PathVariable String consentId, @RequestBody JsonNode jsonNode, HttpServletRequest request) throws Exception {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        JsonNode consent;
        try {
            consent = consentRequestClient.getConsent(consentId);
        } catch (Exception e) {
            return internalErrorResponse(responseParams, response, e);
        }
        String userId = registryHelper.getUserId(request, consent.get("entityName").asText());
        String[] osOwners = consent.get("osOwner").asText().split(",");
        boolean isOwner = Arrays.stream(osOwners).filter(owner -> owner.equals(userId)) != null;
        if(isOwner) {
            return consentRequestClient.grantOrRejectClaim(consentId, jsonNode);
        }
        return forbiddenExceptionResponse(new ConsentForbiddenException("You are not authorized to update this consent"));
    }
}
