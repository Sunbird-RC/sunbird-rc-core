package dev.sunbirdrc.registry.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.pojos.AsyncRequest;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.exception.RecordNotFoundException;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.service.ICertificateService;
import dev.sunbirdrc.registry.transform.ConfigurationHelper;
import dev.sunbirdrc.registry.transform.Transformer;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.ViewTemplateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

    @MockBean
    private ViewTemplateManager viewTemplateManager;

    @Before
    public void setUp() {
        abstractController = new RegistryEntityController();
    }

    @Test
    public void testGetAttestationCertificate_success() throws Exception {
        String response = "{\"Institute\": {\"instituteAffiliation\": [{\"osid\": \"456\", \"_osAttestedData\": {\"instituteName\": \"te\", \"affiliationNumber\": \"901\"}}]}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(response);
        Mockito.when(registryHelper.readEntity("anonymous", "Institute", "123", false, null, false))
                .thenReturn(node);
        Mockito.when(certificateService.getCertificate(node, "Institute", "123", "application/pdf", "http://dummy.com", node))
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
                .andExpect(status().isOk());
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
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetAttestationCertificate_failureIfRecordNotFound() throws Exception {
        String response = "{\"Institute\": {\"instituteAffiliation\": [{\"osid\": \"457\", \"_osAttestedData\": {\"instituteName\": \"te\", \"affiliationNumber\": \"901\"}}]}}";
        JsonNodeFactory.instance.objectNode();
        JsonNode node = new ObjectMapper().readTree(response);
        Mockito.when(registryHelper.readEntity("anonymous", "Institute", "123", false, null, false))
                .thenReturn(node);
        Mockito.when(certificateService.getCertificate(node, "Institute", "123", "application/pdf", "http://dummy.com", node))
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
                .andExpect(status().isBadRequest());
    }
}
