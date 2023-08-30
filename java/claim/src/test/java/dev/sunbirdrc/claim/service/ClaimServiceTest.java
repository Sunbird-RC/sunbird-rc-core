package dev.sunbirdrc.claim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.claim.entity.Claim;
import dev.sunbirdrc.claim.entity.ClaimNote;
import dev.sunbirdrc.claim.exception.ClaimAlreadyProcessedException;
import dev.sunbirdrc.claim.exception.ResourceNotFoundException;
import dev.sunbirdrc.claim.exception.UnAuthorizedException;
import dev.sunbirdrc.claim.repository.ClaimNoteRepository;
import dev.sunbirdrc.claim.repository.ClaimRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static dev.sunbirdrc.claim.contants.AttributeNames.*;
import static dev.sunbirdrc.claim.model.ClaimStatus.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClaimServiceTest {
    private ClaimService claimService;
    @Mock
    ClaimRepository claimRepository;
    @Mock
    ClaimNoteRepository claimNoteRepository;
    @Mock
    SunbirdRCClient sunbirdRCClient;
    @Mock
    ClaimsAuthorizer claimsAuthorizer;

    @Before
    public void setUp() {
        claimService = new ClaimService(claimRepository, claimNoteRepository, sunbirdRCClient, claimsAuthorizer);
    }

    @Test
    public void shouldReturnOnlyAuthorizedClaims() {
        Claim claim1 = getClaim("1");
        Claim claim2 = getClaim("2");
        Claim claim3 = getClaim("3");
        List<Claim> allClaimsForEntity = Arrays.asList(claim1, claim2, claim3);
        Pageable pageable = PageRequest.of(0, 3);
        String entity = "Teacher";
        JsonNode dummyNode = new ObjectMapper().nullNode();
        when(claimRepository.findByAttestorEntity(entity)).thenReturn(allClaimsForEntity);
        when(claimsAuthorizer.isAuthorizedAttestor(claim1, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim2, dummyNode)).thenReturn(false);
        when(claimsAuthorizer.isAuthorizedAttestor(claim3, dummyNode)).thenReturn(true);
        Map<String, Object> actualClaims = new HashMap<>();
        actualClaims.put(CONTENT, Arrays.asList(claim1, claim3));
        actualClaims.put(TOTAL_PAGES, 1);
        actualClaims.put(TOTAL_ELEMENTS, 2);
        assertEquals(claimService.findClaimsForAttestor(entity, dummyNode, pageable), actualClaims);
    }

    @Test
    public void shouldReturnAppropriateClaimsInPaginationFormat() {
        Claim claim1 = getClaim("1");
        Claim claim2 = getClaim("2");
        Claim claim3 = getClaim("3");
        Claim claim4 = getClaim("4");
        List<Claim> allClaimsForEntity = Arrays.asList(claim1, claim2, claim3, claim4);
        Pageable pageable = PageRequest.of(1, 2);
        String entity = "Teacher";
        JsonNode dummyNode = new ObjectMapper().nullNode();
        when(claimRepository.findByAttestorEntity(entity)).thenReturn(allClaimsForEntity);
        when(claimsAuthorizer.isAuthorizedAttestor(claim1, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim2, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim3, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim4, dummyNode)).thenReturn(true);
        Map<String, Object> actualClaims = new HashMap<>();
        actualClaims.put(CONTENT, Arrays.asList(claim3, claim4));
        actualClaims.put(TOTAL_PAGES, 2);
        actualClaims.put(TOTAL_ELEMENTS, 4);
        assertEquals(claimService.findClaimsForAttestor(entity, dummyNode, pageable), actualClaims);
    }

    @Test
    public void shouldReturnEmptyClaimsIfOffsetGreaterThanClaimSize() {
        Claim claim1 = getClaim("1");
        Claim claim2 = getClaim("2");
        Claim claim3 = getClaim("3");
        Claim claim4 = getClaim("4");
        List<Claim> allClaimsForEntity = Arrays.asList(claim1, claim2, claim3, claim4);
        Pageable pageable = PageRequest.of(2, 2);
        String entity = "Teacher";
        JsonNode dummyNode = new ObjectMapper().nullNode();
        when(claimRepository.findByAttestorEntity(entity)).thenReturn(allClaimsForEntity);
        when(claimsAuthorizer.isAuthorizedAttestor(claim1, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim2, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim3, dummyNode)).thenReturn(true);
        when(claimsAuthorizer.isAuthorizedAttestor(claim4, dummyNode)).thenReturn(true);
        Map<String, Object> actualClaims = new HashMap<>();
        actualClaims.put(CONTENT, new ArrayList<>());
        actualClaims.put(TOTAL_PAGES, 2);
        actualClaims.put(TOTAL_ELEMENTS, 4);
        assertEquals(claimService.findClaimsForAttestor(entity, dummyNode, pageable), actualClaims);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void attestClaimShouldThrowExceptionIfTheClaimIsNotFound() {
        String id = "1";
        when(claimRepository.findById(id)).thenReturn(Optional.empty());
        JsonNode dummyNode = new ObjectMapper().nullNode();
        claimService.attestClaim(id, dummyNode);
    }
    @Test(expected = ClaimAlreadyProcessedException.class)
    public void attestClaimShouldThrowExceptionIfTheClaimIsAlreadyProcessed() {
        String id = "1";
        Claim claim = getClaim(id);
        claim.setStatus(APPROVED.name());
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        JsonNode dummyNode = new ObjectMapper().nullNode();
        claimService.attestClaim(id, dummyNode);
    }

    @Test(expected = UnAuthorizedException.class)
    public void attestClaimShouldThrowExceptionIfTheUserIsNotAuthorized() {
        String id = "1";
        Claim claim = getClaim(id);
        claim.setStatus(OPEN.name());
        JsonNode dummyNode = new ObjectMapper().nullNode();
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        claimService.attestClaim(id, dummyNode);
    }

    @Test
    public void shouldAbleToAttestTheClaim() throws JsonProcessingException {
        String id = "1";
        String addedBy = "Rogers";
        String notes = "what ?";
        String entityId = "8658cc54-2c9d-42c9-8093-31e8c9e6e090";
        String propertyURI = "educationDetails/1-8d6dfb25-7789-44da-a6d4-eacf93e3a7bb";

        Claim claim = getClaim(id);
        claim.setStatus(OPEN.name());
        claim.setEntityId(entityId);
        claim.setPropertyURI(propertyURI);
        ObjectNode requestBody = new ObjectMapper().createObjectNode();

        JsonNode dummyNode = new ObjectMapper().readTree(getStudentEntity());
        requestBody.set(ATTESTOR_INFO, dummyNode);
        requestBody.put(NOTES, notes);

        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(claimsAuthorizer.isAuthorizedAttestor(claim, dummyNode)).thenReturn(true);

        ClaimNote expectedClaimNote = new ClaimNote();
        expectedClaimNote.setNotes(notes);
        expectedClaimNote.setPropertyURI(propertyURI);
        expectedClaimNote.setEntityId(entityId);
        expectedClaimNote.setAddedBy(addedBy);

        claimService.attestClaim(id, requestBody);
        verify(claimRepository, atLeastOnce()).save(any());
        verify(claimNoteRepository, atLeastOnce()).save(expectedClaimNote);
    }
    private Claim getClaim(String id) {
        Claim claim = new Claim();
        claim.setId(id);
        return claim;
    }

    private String getStudentEntity() {
        return "{\n" +
                "        \"educationDetails\": [\n" +
                "            {\n" +
                "                \"graduationYear\": \"2022\",\n" +
                "                \"institute\": \"CD universe\",\n" +
                "                \"osid\": \"1-8d6dfb25-7789-44da-a6d4-eacf93e3a7bb\",\n" +
                "                \"program\": \"8th\",\n" +
                "                \"marks\": \"99\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"graduationYear\": \"2021\",\n" +
                "                \"institute\": \"DC universe\",\n" +
                "                \"osid\": \"1-7d9dfb25-7789-44da-a6d4-eacf93e3a7aa\",\n" +
                "                \"program\": \"test123\",\n" +
                "                \"marks\": \"78\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"contactDetails\": {\n" +
                "            \"osid\": \"1-096cd663-6ba9-49f8-af31-1ace9e31bc31\",\n" +
                "            \"mobile\": \"9000090000\",\n" +
                "            \"osOwner\": \"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\",\n" +
                "            \"email\": \"ram@gmail.com\"\n" +
                "        },\n" +
                "        \"osid\": \"1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb\",\n" +
                "        \"identityDetails\": {\n" +
                "            \"osid\": \"1-9f50f1b3-99cc-4fcb-9e51-e0dbe0be19f9\",\n" +
                "            \"gender\": \"Male\",\n" +
                "            \"identityType\": \"\",\n" +
                "            \"dob\": \"1999-01-01\",\n" +
                "            \"fullName\": \"Rogers\",\n" +
                "            \"identityValue\": \"\",\n" +
                "            \"osOwner\": \"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\"\n" +
                "        },\n" +
                "        \"osOwner\": \"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\"\n" +
                "    }";
    }
}
