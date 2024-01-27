package dev.sunbirdrc.registry.controller;

import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.entities.AttestationStatus;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
public class RegistryAttestationPolicyController extends AbstractController {
    private static Logger logger = LoggerFactory.getLogger(RegistryAttestationPolicyController.class);

    @Value("${registry.attestationPolicy.createAccess:''}")
    public List<String> createAttestationEntities;

    @PostMapping("/api/v1/{entityName}/attestationPolicy")
    public ResponseEntity createAttestationPolicy(@PathVariable String entityName, @RequestBody AttestationPolicy
            attestationPolicy, HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
        try {
            List<String> userEntities = registryHelper.getUserEntities(request);
            if (createAttestationEntities.containsAll(userEntities) && definitionsManager.isValidEntityName(entityName) &&
                    !registryHelper.isAttestationPolicyNameAlreadyUsed(entityName, attestationPolicy.getName())) {
                String userId = registryHelper.getUserId(entityName);
                attestationPolicy.setCreatedBy(userId);
                logger.info("Creating attestation policy for entity: {} - {}", entityName, attestationPolicy);
                attestationPolicy.setEntity(entityName);
                attestationPolicy.setStatus(AttestationStatus.DRAFT);
                response.setResult(registryHelper.createAttestationPolicy(attestationPolicy, userId));
                responseParams.setStatus(Response.Status.SUCCESSFUL);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return badRequestException(responseParams, response, "Invalid entity name");
            }
        } catch (Exception e) {
            logger.error("Failed persisting attestation policy: {}", ExceptionUtils.getStackTrace(e));
            return internalErrorResponse(responseParams, response, e);
        }
    }

    @GetMapping("/api/v1/{entityName}/attestationPolicies")
    public ResponseEntity getAttestationPolicies(@PathVariable String entityName, HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        try {
            if (definitionsManager.isValidEntityName(entityName)) {
                String userId = registryHelper.getUserId(entityName);
                List<String> userEntities = registryHelper.getUserEntities(request);
                if (userEntities.contains(entityName)) {
                    logger.info("Retrieving attestation policies for entity: {}", entityName);
                    response.setResult(registryHelper.getAttestationPolicies(entityName));
                    responseParams.setStatus(Response.Status.SUCCESSFUL);
                } else {
                    logger.info("Retrieving attestation policies created by user: {}, entity: {}", userId, entityName);
                    response.setResult(registryHelper.findAttestationPolicyByEntityAndCreatedBy(entityName, userId));
                    responseParams.setStatus(Response.Status.SUCCESSFUL);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return badRequestException(responseParams, response, "Invalid entity name");
            }
        } catch (Exception e) {
            logger.error("Failed getting attestation policy: {}", ExceptionUtils.getStackTrace(e));
            return internalErrorResponse(responseParams, response, e);
        }
    }

    @PutMapping("/api/v1/{entityName}/attestationPolicy/{policyOSID}")
    public ResponseEntity updateAttestationPolicy(@PathVariable String entityName, @PathVariable String policyOSID,
                                                  @RequestBody final AttestationPolicy attestationPolicy, HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            if (definitionsManager.isValidEntityName(entityName)) {
                String userId = registryHelper.getUserId(entityName);
                final Optional<AttestationPolicy> attestationPolicyOptional = registryHelper.findAttestationPolicyById(userId, policyOSID);
                if (attestationPolicyOptional.isPresent() && attestationPolicyOptional.get().getCreatedBy().equals(userId)) {
                    logger.info("Updating attestation policies id: {}", policyOSID);
                    response.setResult(registryHelper.updateAttestationPolicy(userId, attestationPolicy));
                    responseParams.setStatus(Response.Status.SUCCESSFUL);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
            return badRequestException(responseParams, response, "Invalid entity name");
        } catch (Exception e) {
            logger.error("Failed updating attestation policy: {}", ExceptionUtils.getStackTrace(e));
            return internalErrorResponse(responseParams, response, e);
        }
    }

    @PutMapping("/api/v1/{entityName}/attestationPolicy/{policyId}/{status}")
    public ResponseEntity updateAttestationPolicyStatus(@PathVariable String entityName, @PathVariable String policyId,
                                                        @PathVariable AttestationStatus status, HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            if (definitionsManager.isValidEntityName(entityName)) {
                String userId = registryHelper.getUserId(entityName);
                Optional<AttestationPolicy> attestationPolicyOptional = registryHelper.findAttestationPolicyById(userId, policyId);
                if (attestationPolicyOptional.isPresent() && attestationPolicyOptional.get().getCreatedBy().equals(userId)) {
                    logger.info("Updating attestation policy status of id: {}", policyId);
                    AttestationPolicy attestationPolicy = attestationPolicyOptional.get();
                    attestationPolicy.setStatus(status);
                    response.setResult(registryHelper.updateAttestationPolicy(userId, attestationPolicy));
                    responseParams.setStatus(Response.Status.SUCCESSFUL);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
            return badRequestException(responseParams, response, "Invalid entity name");
        } catch (Exception e) {
            logger.error("Failed updating attestation policy: {}", ExceptionUtils.getStackTrace(e));
            return internalErrorResponse(responseParams, response, e);
        }
    }

    @DeleteMapping("/api/v1/{entityName}/attestationPolicy/{policyId}")
    public ResponseEntity deleteAttestationPolicy(@PathVariable String entityName, @PathVariable String policyId,
                                                  HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            if (definitionsManager.isValidEntityName(entityName)) {
                String userId = registryHelper.getUserId(entityName);
                Optional<AttestationPolicy> attestationPolicyOptional = registryHelper.findAttestationPolicyById(userId, policyId);
                if (attestationPolicyOptional.isPresent() && attestationPolicyOptional.get().getCreatedBy().equals(userId)) {
                    logger.info("Updating attestation policy status of id: {}", policyId);
                    AttestationPolicy attestationPolicy = attestationPolicyOptional.get();
                    registryHelper.deleteAttestationPolicy(entityName, attestationPolicy);
                    response.setResult("deleted");
                    responseParams.setStatus(Response.Status.SUCCESSFUL);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
            return badRequestException(responseParams, response, "Invalid entity name");
        } catch (Exception e) {
            logger.error("Failed updating attestation policy: {}", ExceptionUtils.getStackTrace(e));
            return internalErrorResponse(responseParams, response, e);
        }
    }
}
