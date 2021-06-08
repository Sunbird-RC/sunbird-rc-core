package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.repository.ClaimRepository;
import io.opensaber.pojos.attestation.AttestationPolicy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.*;
import static io.opensaber.claim.model.ClaimStatus.CLOSED;
import static io.opensaber.claim.model.ClaimStatus.OPEN;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClaimServiceTest {
    private ClaimService claimService;
    private final HttpHeaders dummyHeader = new HttpHeaders();
    private Claim claim;
    private final String claimId = "123";
    private final String  role = "bo";

    @Mock
    ClaimRepository claimRepository;
    @Mock
    OpenSaberClient openSaberClient;
    @Captor
    ArgumentCaptor<Claim> argumentCaptor;

    @Before
    public void setUp() {
        claimService = new ClaimService(claimRepository, openSaberClient);
        claim = new Claim();
        claim.setId(claimId);
        claim.setStatus(OPEN.name());
        String boRole = "bo";
    }

    @Test
    public void shouldAbleToSetTheNotes() {
        Optional<String> optionalNotes = Optional.of("Burning keyboard with java");
        assertNotes(optionalNotes, optionalNotes.get());
    }

    @Test
    public void shouldAbleToSetTheEmptyNotes() {
        Optional<String> optionalNotes = Optional.empty();
        assertNotes(optionalNotes, "");
    }

    private void assertNotes(Optional<String> optionalNotes, String expectedNotes) {
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        claimService.updateNotes(claimId, optionalNotes, dummyHeader, Collections.emptyList());
        verify(claimRepository).save(argumentCaptor.capture());
        Claim actualValue = argumentCaptor.getValue();
        assertEquals(expectedNotes, actualValue.getNotes());
        assertEquals(CLOSED.name(), actualValue.getStatus());
        verify(openSaberClient, atLeastOnce()).updateAttestedProperty(actualValue, dummyHeader);
    }

    @Test
    public void shouldAbleToGrantClaim() throws Exception {
        claim.setProperty("education");
        claim.setPropertyId("1-faaa0c01-c77f-4906-90ed-9f4b853eaaac");
        String expectedAttestedData = "{\"start\":\"2014\",\"end\":\"2015\",\"institute\":\"AHSS\",\"class\":\"12th\",\"studentName\":\"Muthu raman\"}";
        assertGrantClaim(expectedAttestedData);
    }

    private void assertGrantClaim(String expectedAttestedData) throws Exception {
        AttestationPropertiesDTO attestationPropertiesDTO = getAttestationPropertiesDTO();
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(openSaberClient.getAttestationProperties(claim)).thenReturn(attestationPropertiesDTO);
        claimService.grantClaim(claimId, Collections.singletonList(role), dummyHeader);
        verify(claimRepository).save(argumentCaptor.capture());
        Claim actualClaim = argumentCaptor.getValue();
        assertNull(actualClaim.getNotes());
        assertEquals(CLOSED.name(), actualClaim.getStatus());
        verify(openSaberClient).updateAttestedProperty(actualClaim, expectedAttestedData, dummyHeader);
    }

    @Test
    public void shouldAbleToGrantClaimForSingleValueAttribute() throws Exception {
        claim.setProperty("nationalIdentifier");
        String expectedAttestedData =  "{\"nationalIdentifier\":\"123456\"}";
        assertGrantClaim(expectedAttestedData);
    }

    private AttestationPropertiesDTO getAttestationPropertiesDTO() throws IOException {
        String studentEntity = getStudentEntity();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(studentEntity);
        Map<String, Object> entity = objectMapper.convertValue(node, HashMap.class);
        AttestationPolicy attestationPolicy1 = new AttestationPolicy();
        attestationPolicy1.setPaths(Arrays.asList("$.education[?(@.osid == 'PROPERTY_ID')]['start', 'end', 'institute']", "$['studentName', 'class']"));
        attestationPolicy1.setProperty("education");
        AttestationPolicy attestationPolicy2 = new AttestationPolicy();
        attestationPolicy2.setPaths(Collections.singletonList("nationalIdentifier"));
        attestationPolicy2.setProperty("nationalIdentifier");

        AttestationPropertiesDTO attestationPropertiesDTO = new AttestationPropertiesDTO();
        attestationPropertiesDTO.setEntity(entity);
        attestationPropertiesDTO.setAttestationPolicies(Arrays.asList(attestationPolicy1, attestationPolicy2));
        return attestationPropertiesDTO;
    }

    private String getStudentEntity() {
        return "{\n" +
                "    \"gender\": \"Male\",\n" +
                "    \"studentName\": \"Muthu raman\",\n" +
                "    \"osid\": \"1-2b2a4d9a-1301-4580-8ca1-114e6dbda1c4\",\n" +
                "    \"nationalIdentifier\": \"123456\",\n" +
                "    \"education\": [\n" +
                "        {\n" +
                "            \"_osState\": \"ATTESTATION_REQUESTED\",\n" +
                "            \"degree\": \"HSC\",\n" +
                "            \"start\": \"2014\",\n" +
                "            \"end\": \"2015\",\n" +
                "            \"institute\": \"AHSS\",\n" +
                "            \"osid\": \"1-faaa0c01-c77f-4906-90ed-9f4b853eaaac\",\n" +
                "            \"_osClaimId\": \"3ec8e8f1-f5fa-470b-ac8e-97aefcc8a470\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"_osState\": \"REJECTED\",\n" +
                "            \"degree\": \"12th\",\n" +
                "            \"start\": \"2010\",\n" +
                "            \"end\": \"2012\",\n" +
                "            \"institute\": \"90001\",\n" +
                "            \"osid\": \"1-48e69a5a-02fd-4320-b5f5-4b7b31bd5385\",\n" +
                "            \"_osClaimId\": \"a0db1d81-115b-433b-985e-76ce97bb1373\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"class\": \"12th\"\n" +
                "}";
    }
}
