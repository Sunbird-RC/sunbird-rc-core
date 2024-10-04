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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static dev.sunbirdrc.claim.contants.AttributeNames.*;
import static dev.sunbirdrc.claim.model.ClaimStatus.CLOSED;
import static dev.sunbirdrc.claim.model.ClaimStatus.OPEN;
import static dev.sunbirdrc.registry.middleware.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {
    @Mock
    ClaimRepository claimRepository;
    @Mock
    ClaimNoteRepository claimNoteRepository;
    @MockBean
    SunbirdRCClient sunbirdRCClient;
    @Mock
    ClaimsAuthorizer claimsAuthorizer;
    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, claimNoteRepository, sunbirdRCClient, claimsAuthorizer);
    }

    @Test
    void shouldReturnOnlyAuthorizedClaims() {
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
    void shouldReturnAppropriateClaimsInPaginationFormat() {
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
    void shouldReturnEmptyClaimsIfOffsetGreaterThanClaimSize() {
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

    @Test
    void attestClaimShouldThrowExceptionIfTheClaimIsNotFound() {
        String id = "1";
        when(claimRepository.findById(id)).thenReturn(Optional.empty());
        JsonNode dummyNode = new ObjectMapper().nullNode();
        assertThrows(ResourceNotFoundException.class, () -> claimService.attestClaim(id, dummyNode));
    }

    @Test
    void attestClaimShouldThrowExceptionIfTheClaimIsAlreadyProcessed() {
        String id = "1";
        Claim claim = getClaim(id);
        claim.setStatus(CLOSED.name());
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        JsonNode dummyNode = new ObjectMapper().nullNode();
        assertThrows(ClaimAlreadyProcessedException.class, () -> claimService.attestClaim(id, dummyNode));
    }

    @Test
    void attestClaimShouldThrowExceptionIfTheUserIsNotAuthorized() {
        String id = "1";
        Claim claim = getClaim(id);
        claim.setStatus(OPEN.name());
        JsonNode dummyNode = new ObjectMapper().nullNode();
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        assertThrows(UnAuthorizedException.class, () -> claimService.attestClaim(id, dummyNode));
    }

    @Test
    void shouldAbleToAttestTheClaim() throws JsonProcessingException {
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
        requestBody.put(USER_ID, 1);

        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(claimsAuthorizer.isAuthorizedAttestor(claim, dummyNode)).thenReturn(true);

        ClaimNote expectedClaimNote = new ClaimNote();
        expectedClaimNote.setNotes(notes);
        expectedClaimNote.setPropertyURI(propertyURI);
        expectedClaimNote.setEntityId(entityId);
        expectedClaimNote.setAddedBy(addedBy);

        claimService.attestClaim(id, requestBody);
        verify(claimRepository, atLeastOnce()).save(any());
        verify(claimNoteRepository, atLeastOnce()).save(argThat(note ->
                note.getNotes().equals(expectedClaimNote.getNotes()) &&
                        note.getPropertyURI().equals(expectedClaimNote.getPropertyURI()) &&
                        note.getEntityId().equals(expectedClaimNote.getEntityId()) &&
                        note.getAddedBy().equals(expectedClaimNote.getAddedBy())
        ));
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