package dev.sunbirdrc.registry.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.pojos.AsyncRequest;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import org.mockito.ArgumentMatchers;
import dev.sunbirdrc.registry.exception.RecordNotFoundException;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.service.ICertificateService;
import dev.sunbirdrc.registry.transform.*;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import dev.sunbirdrc.registry.util.ViewTemplateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.springframework.http.HttpHeaders;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest({RegistryEntityController.class})
@ContextConfiguration(classes = {RegistryEntityController.class})
@AutoConfigureMockMvc(addFilters = false)
public class RegistryEntityControllerTest {

    @MockBean
    ObjectMapper objectMapper;
    @MockBean
    SunbirdRCInstrumentation watch;
    @MockBean
    RegistryHelper registryHelper;
    @MockBean
    DBConnectionInfoMgr dbConnectionInfoMgr;
    @MockBean
    Transformer transformer;
    @MockBean
    ConfigurationHelper configurationHelper;
    @MockBean
    DefinitionsManager definitionsManager;
    @MockBean
    private ICertificateService certificateService;
    @MockBean
    private FileStorageService fileStorageService;
    @MockBean
    private AsyncRequest asyncRequest;
    @Autowired
    private MockMvc mockMvc;
    private AbstractController abstractController;
    @InjectMocks
    private RegistryEntityController registryEntityController;

    @MockBean
    private ViewTemplateManager viewTemplateManager;

    @Before
    public void setUp() {
        abstractController = new RegistryEntityController();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttestationCertificate_success() throws Exception {
        String response = "{\"Institute\": {\"instituteAffiliation\": [{\"osid\": \"456\", \"_osAttestedData\": {\"instituteName\": \"te\", \"affiliationNumber\": \"901\"}}]}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(response);
        Mockito.when(registryHelper.readEntity("anonymous", "Institute", "123", false, null, false))
                .thenReturn(node);
        Mockito.when(certificateService.getCertificate(node, "Institute", "123", "application/pdf", "http://dummy.com", node,"", false))
                .thenReturn("");
        mockMvc.perform(
                    MockMvcRequestBuilders
                    .get("/api/v1/Institute/123/attestation/instituteAffiliation/456")
                            .with(mockHttpServletRequest -> {
                                mockHttpServletRequest.addHeader("accept", "application/pdf");
                                mockHttpServletRequest.addHeader("template", "http://dummy.com");
                                try {
                                    Mockito.when(registryHelper.getUserId(mockHttpServletRequest, "Institute")).thenReturn("anonymous");
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return mockHttpServletRequest;
                            })
                )
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAttestationCertificate_failureWhenClaimNotAttested() throws Exception {
        String response = "{\"Institute\": {\"instituteAffiliation\": [{\"osid\": \"456\"}]}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(response);
        Mockito.when(registryHelper.readEntity("anonymous", "Institute", "123", false, null, false))
                .thenReturn(node);
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/v1/Institute/123/attestation/instituteAffiliation/456")
                                .with(mockHttpServletRequest -> {
                                    try {
                                        Mockito.when(registryHelper.getUserId(mockHttpServletRequest, "Institute")).thenReturn("anonymous");
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return mockHttpServletRequest;
                                })
                )
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAttestationCertificate_failureWhenUserNotAllowed() throws Exception {
        String response = "{\"Institute\": {\"instituteAffiliation\": [{\"osid\": \"456\", \"_osAttestedData\": {\"instituteName\": \"te\", \"affiliationNumber\": \"901\"}}]}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(response);
        Mockito.when(registryHelper.readEntity("anonymous", "Institute", "123", false, null, false))
                .thenReturn(node);
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/v1/Institute/123/attestation/instituteAffiliation/456")
                                .with(mockHttpServletRequest -> {
                                    try {
                                        Mockito.when(registryHelper.getUserId(mockHttpServletRequest, "Institute")).thenThrow(new Exception("Forbidden"));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return mockHttpServletRequest;
                                })
                )
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAttestationCertificate_failureIfRecordNotFound() throws Exception {
        String response = "{\"Institute\": {\"instituteAffiliation\": [{\"osid\": \"457\", \"_osAttestedData\": {\"instituteName\": \"te\", \"affiliationNumber\": \"901\"}}]}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(response);
        Mockito.when(registryHelper.readEntity("anonymous", "Institute", "123", false, null, false))
                .thenReturn(node);
        Mockito.when(certificateService.getCertificate(node, "Institute", "123", "application/pdf", "http://dummy.com", node,"", false))
                .thenThrow(new NullPointerException(""));
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/v1/Institute/123/attestation/instituteAffiliation/456")
                                .with(mockHttpServletRequest -> {
                                    mockHttpServletRequest.addHeader("accept", "application/pdf");
                                    mockHttpServletRequest.addHeader("template", "http://dummy.com");
                                    try {
                                        Mockito.when(registryHelper.getUserId(mockHttpServletRequest, "Institute")).thenThrow(new RecordNotFoundException("Invalid id"));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return mockHttpServletRequest;
                                })
                )
                .andExpect(status().isNotFound());
    }

    @Test
    public void testRevokeACredential_RecordNotFound() throws Exception {
        // Mock HttpServletRequest and HttpHeaders
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(registryHelper.doesEntityOperationRequireAuthorization(anyString())).thenReturn(false);
        when(registryHelper.revokeAnEntity(anyString(), anyString(), anyString(), any(JsonNode.class))).thenReturn(null);
        ResponseEntity<Object> response = registryEntityController.revokeACredential(request, "mockEntityName", "mockEntityId", headers);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(registryHelper, never()).revokeExistingCredentials(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testRevokeACredential_UnauthorizedAccess() throws Exception {
        // Mock HttpServletRequest and HttpHeaders
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(registryHelper.doesEntityOperationRequireAuthorization(anyString())).thenReturn(true);
        when(registryHelper.authorize(anyString(), anyString(), any(HttpServletRequest.class))).thenThrow(new Exception("Unauthorized"));
        ResponseEntity<Object> response = registryEntityController.revokeACredential(request, "mockEntityName", "mockEntityId", headers);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(registryHelper, never()).revokeAnEntity(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(JsonNode.class));
        verify(registryHelper, never()).revokeExistingCredentials(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testRevokeACredential_CredentialAlreadyRevoked() throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        String testData= "{\"ScholarshipForTopClassStudents\":{\"osUpdatedAt\":\"2023-06-15T11:33:24.563Z\",\"nameofScheme\":\"SSC\",\"gender\":\"Male\",\"osUpdatedBy\":\"4c440ca5-312f-45f2-a11b-6b551f22eec0\",\"rollNo\":\"1341341323\",\"dateofaward\":\"2023-03-01\",\"osid\":\"1-03c0316f-e05d-4a9d-bd55-d8efb2e5fbac\",\"_osSignedData\":{\"context\":\"sometextContext\"},\"osOwner\":[\"6efae99a-327f-4044-a5ff-e81180818f11\",\"4c440ca5-312f-45f2-a11b-6b551f22eec0\"],\"validupto\":\"2025-05-01\",\"academicYear\":\"2020\",\"osCreatedAt\":\"2023-06-15T11:33:24.563Z\",\"contact\":\"9876543240\",\"name\":\"Sample6\",\"osCreatedBy\":\"4c440ca5-312f-45f2-a11b-6b551f22eec0\",\"institute\":\"IITBOMBAY\",\"email\":\"test2@gmail.com\"}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(testData);
        when(registryHelper.doesEntityOperationRequireAuthorization(anyString())).thenReturn(true);
        when(registryHelper.authorize(anyString(), anyString(), any(HttpServletRequest.class))).thenReturn("userId");
        Data<Object> mockDataObject = mock(Data.class);
        JsonNode mockEntityNode = mock(JsonNode.class);
        when(registryHelper.readEntity(anyString(), anyString(), anyString(), anyBoolean(), any(), anyBoolean())).thenReturn(mockEntityNode);
        when(registryHelper.revokeAnEntity(anyString(),anyString(), anyString(),any(JsonNode.class))).thenReturn(mock(JsonNode.class));
        Configuration mockConfig = mock(Configuration.class);
        doReturn(mockConfig).when(configurationHelper).getResponseConfiguration(anyBoolean());
        ITransformer<Object> mockResponseTransformer = mock(ITransformer.class);
        doReturn(mockResponseTransformer).when(transformer).getInstance(mockConfig);
        Data<Object> mockData = new Data<Object>(node);
        doReturn(mockData).when(mockResponseTransformer).transform(any(Data.class));
        ResponseEntity<Object> response = registryEntityController.revokeACredential(request, "ScholarshipForTopClassStudents", "entityId", headers);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

    }

    @Test
    public void testRevokeACredential_SuccessfulRevocation() throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);

        String testData= "{\"ScholarshipForTopClassStudents\":{\"osUpdatedAt\":\"2023-06-15T11:33:24.563Z\",\"nameofScheme\":\"SSC\",\"gender\":\"Male\",\"osUpdatedBy\":\"4c440ca5-312f-45f2-a11b-6b551f22eec0\",\"rollNo\":\"1341341323\",\"dateofaward\":\"2023-03-01\",\"osid\":\"1-03c0316f-e05d-4a9d-bd55-d8efb2e5fbac\",\"_osSignedData\":\"{\\\"@context\\\":[\\\"https://www.w3.org/2018/credentials/v1\\\",\\\"https://gist.githubusercontent.com/varadeth/10f24d680a923f3d40eafbd6b058edaf/raw/d63b4f785adbb40e324e8b95dfbcaadaff09f721/scholarshipfortopclass.json\\\"],\\\"type\\\":[\\\"VerifiableCredential\\\"],\\\"issuanceDate\\\":\\\"2023-06-15T11:33:30.801Z\\\",\\\"credentialSubject\\\":{\\\"type\\\":\\\"Student\\\",\\\"name\\\":\\\"Sample7\\\",\\\"gender\\\":\\\"Male\\\",\\\"institute\\\":\\\"IITBOMBAY\\\",\\\"academicYear\\\":\\\"2020\\\",\\\"rollNo\\\":\\\"1341341323\\\"},\\\"evidence\\\":{\\\"type\\\":\\\"Scholarship\\\",\\\"nameofScheme\\\":\\\"SSC\\\",\\\"dateofaward\\\":\\\"2023-03-01\\\",\\\"validupto\\\":\\\"2025-05-01\\\"},\\\"issuer\\\":\\\"did:web:sunbirdrc.dev/vc/scholarship\\\",\\\"proof\\\":{\\\"type\\\":\\\"RsaSignature2018\\\",\\\"created\\\":\\\"2023-06-15T11:33:30Z\\\",\\\"verificationMethod\\\":\\\"did:india\\\",\\\"proofPurpose\\\":\\\"assertionMethod\\\",\\\"jws\\\":\\\"eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..a5vQRl5GO7s0ozAM0STMJ4F5wkoUIJjKdkszVwpd1TaSz-kVJfkWHLxVaBNPaN5hvNqRorNlDUiuSz7WvdOorv87VS0mj95GOt7JKj6vp-rYaiakX3efVbt2jW0qCUui6-kglPxIKUf1lpoYNSMYrCVJi_Z2AxGV-BQgjaGBZDQN-h-YzOTRViuKGQcRkrbYJR3zZsrWwQAeNTloUNYew0xR2zSKJ8LHY7NTiuUYW36y0xLMiVvrJIGJPZMDl-8aEw8NcInPpuxEbE8InC5sLqKPNkioj4zDf3tWMnVuA-duoCK3KZG-IwoQ8yt3QJw_6NIuGVha-VrV7E0fJcHczA\\\"}}\",\"osOwner\":[\"6efae99a-327f-4044-a5ff-e81180818f11\",\"4c440ca5-312f-45f2-a11b-6b551f22eec0\"],\"validupto\":\"2025-05-01\",\"academicYear\":\"2020\",\"osCreatedAt\":\"2023-06-15T11:33:24.563Z\",\"contact\":\"9876543240\",\"name\":\"Sample6\",\"osCreatedBy\":\"4c440ca5-312f-45f2-a11b-6b551f22eec0\",\"institute\":\"IITBOMBAY\",\"email\":\"test2@gmail.com\"}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(testData);
        when(registryHelper.doesEntityOperationRequireAuthorization(anyString())).thenReturn(true);
        when(registryHelper.authorize(anyString(), anyString(), any(HttpServletRequest.class))).thenReturn("userId");
        Data<Object> mockDataObject = mock(Data.class);
        JsonNode mockEntityNode = mock(JsonNode.class);
        when(registryHelper.readEntity(anyString(), anyString(), anyString(), anyBoolean(), any(), anyBoolean())).thenReturn(mockEntityNode);
        when(registryHelper.revokeAnEntity(anyString(), anyString(), anyString(), any(JsonNode.class))).thenReturn(mock(JsonNode.class));
        Configuration mockConfig = mock(Configuration.class);
        doReturn(mockConfig).when(configurationHelper).getResponseConfiguration(anyBoolean());
        ITransformer<Object> mockResponseTransformer = mock(ITransformer.class);
        doReturn(mockResponseTransformer).when(transformer).getInstance(mockConfig);
        Data<Object> mockData = new Data<Object>(node);
        doReturn(mockData).when(mockResponseTransformer).transform(any(Data.class));
        ResponseEntity<Object> response = registryEntityController.revokeACredential(request, "ScholarshipForTopClassStudents", "entityId", headers);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
