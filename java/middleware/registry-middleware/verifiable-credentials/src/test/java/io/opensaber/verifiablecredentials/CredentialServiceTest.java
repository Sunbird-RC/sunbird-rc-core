package io.opensaber.verifiablecredentials;


import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.jsonld.JsonLDException;
import info.weboftrust.ldsignatures.LdProof;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    private CredentialService credentialService;
    private String privateKey = "984b589e121040156838303f107e13150be4a80fc5088ccba0b0bdc9b1d89090de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580";
    private String publicKey = "de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580";

    @Before
    public void setup() {
        credentialService = new CredentialService(privateKey, publicKey, "", "", "");
    }

    @Test
    public void testSignData() throws GeneralSecurityException, JsonLDException, ParseException, IOException {
        LdProof ldProof = credentialService.sign("{\n" +
                "  \"@context\": {\n" +
                "    \"schema\": \"http://schema.org/\",\n" +
                "    \"data\": \"schema:name\",\n" +
                "    \"issuer\": \"schema:url\",\n" +
                "    \"date\": \"schema:date\"\n" +
                "  },\n" +
                "  \"data\": \"XXX-123456-QWERTY\",\n" +
                "  \"issuer\": \"did:issuer:cowin\",\n" +
                "  \"date\": \"28-09-2021\"\n" +
                "}");
        System.out.println(ldProof);
        assertFalse(ldProof.getJws().isEmpty());
    }

    @Test
    public void testVerifySignedData() throws GeneralSecurityException, JsonLDException, ParseException, IOException {
        String input = "{\n" +
                "  \"@context\": {\n" +
                "    \"schema\": \"http://schema.org/\",\n" +
                "    \"data\": \"schema:name\",\n" +
                "    \"issuer\": \"schema:url\",\n" +
                "    \"date\": \"schema:date\"\n" +
                "  },\n" +
                "  \"data\": \"XXX-123456-QWERTY\",\n" +
                "  \"issuer\": \"did:issuer:cowin\",\n" +
                "  \"date\": \"28-09-2021\"\n" +
                "}";
        String data = "{\"type\":\"Ed25519Signature2018\",\"creator\":\"\",\"created\":\"2017-10-24T05:33:31Z\",\"domain\":\"\",\"nonce\":\"\",\"proofPurpose\":\"AssertionProofPurpose\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..8Wgsb1tmGylq_ts6P2oK4d5NYy-pZI3_YianCiwle53C6E0p8N5ti-zYr8bwmeS4NSCuvf0ClhDp55j0PowLCw\"}" ;
        LdProof ldProof = new ObjectMapper().readValue(data, LdProof.class);
        assertTrue(credentialService.verify(input, ldProof));
    }

    @Test
    public void testShouldFailForModifiedInputData() throws GeneralSecurityException, JsonLDException, ParseException, IOException {
        String input = "{\n" +
                "  \"@context\": {\n" +
                "    \"schema\": \"http://schema.org/\",\n" +
                "    \"data\": \"schema:name\",\n" +
                "    \"issuer\": \"schema:url\",\n" +
                "    \"date\": \"schema:date\"\n" +
                "  },\n" +
                "  \"data\": \"XXX-123456-QWERT-1Y\",\n" +
                "  \"issuer\": \"did:issuer:cowin\",\n" +
                "  \"date\": \"28-09-2021\"\n" +
                "}";
        String data = "{\"type\":\"Ed25519Signature2018\",\"creator\":\"\",\"created\":\"2017-10-24T05:33:31Z\",\"domain\":\"\",\"nonce\":\"\",\"proofPurpose\":\"AssertionProofPurpose\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..8Wgsb1tmGylq_ts6P2oK4d5NYy-pZI3_YianCiwle53C6E0p8N5ti-zYr8bwmeS4NSCuvf0ClhDp55j0PowLCw\"}" ;
        LdProof ldProof = new ObjectMapper().readValue(data, LdProof.class);
        assertFalse(credentialService.verify(input, ldProof));
    }
}