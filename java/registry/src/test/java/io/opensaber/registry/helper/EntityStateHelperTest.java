package io.opensaber.registry.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.keycloak.KeycloakAdminUtil;
import io.opensaber.keycloak.OwnerCreationException;
import io.opensaber.workflow.KieConfiguration;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.workflow.RuleEngineService;
import io.opensaber.registry.util.ClaimRequestClient;
import io.opensaber.registry.util.DefinitionsManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieContainer;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ObjectMapper.class, DefinitionsManager.class,
        ConditionResolverService.class, ClaimRequestClient.class, KeycloakAdminUtil.class, KieConfiguration.class})
@Import(EntityStateHelperTestConfiguration.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EntityStateHelperTest {

    @Mock
    ConditionResolverService conditionResolverService;

    @Mock
    ClaimRequestClient claimRequestClient;

    @Mock
    KeycloakAdminUtil keycloakAdminUtil;

    @Autowired
    DefinitionsManager definitionsManager;

    @Autowired
    KieContainer kieContainer;

    private static final String TEST_DIR = "src/test/resources/entityStateHelper/";

    ObjectMapper m = new ObjectMapper();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    private void runTest(JsonNode existing, JsonNode updated, JsonNode expected) {
        RuleEngineService ruleEngineService = new RuleEngineService(kieContainer, keycloakAdminUtil);
        EntityStateHelper entityStateHelper = new EntityStateHelper(definitionsManager, ruleEngineService, conditionResolverService, claimRequestClient);
        ReflectionTestUtils.setField(entityStateHelper, "uuidPropertyName", "osid");
        entityStateHelper.applyWorkflowTransitions(existing, updated);
        assertEquals(expected, updated);
    }

    @Test
    public void shouldMarkAsDraftWhenThereIsNewEntry() throws IOException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldMarkAsDraftWhenThereIsNewEntry.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("afterStateChange"));
    }

    @Test
    public void shouldMarkAsDraftIfThereIsAChange() throws IOException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldMarkAsDraftIfThereIsAChange.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("afterStateChange"));
    }

    @Test
    public void shouldBeNoStateChangeIfTheDataDidNotChange() throws IOException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldBeNoStateChangeIfTheDataDidNotChange.json"));
        JsonNode beforeUpdate = test.get("updated").deepCopy();
        runTest(test.get("existing"), test.get("updated"), test.get("existing"));
    }

    @Test
    public void shouldRaiseClaimWhenSentForAttestation() throws Exception {
        RuleEngineService ruleEngineService = new RuleEngineService(kieContainer, keycloakAdminUtil);
        EntityStateHelper entityStateHelper = new EntityStateHelper(definitionsManager, ruleEngineService, conditionResolverService, claimRequestClient);
        ReflectionTestUtils.setField(entityStateHelper, "uuidPropertyName", "osid");
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldRaiseClaimWhenSentForAttestation.json"));
        String propertyURI = "educationDetails/fgyuhij";
        HashMap<String, Object> mockRaiseClaimResponse = new HashMap<>();
        mockRaiseClaimResponse.put("id", "raised_claim_id");

        when(conditionResolverService.resolve(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn("");
        when(claimRequestClient.riseClaimRequest(ArgumentMatchers.any()))
                .thenReturn(mockRaiseClaimResponse);
        String notes = "";
        assertEquals(test.get("expected"), entityStateHelper.sendForAttestation(test.get("existing"), propertyURI, notes));

    }

    @Test
    public void shouldCreateNewOwnersForNewlyAddedOwnerFields() throws IOException, DuplicateRecordException, EntityCreationException, OwnerCreationException {
        when(keycloakAdminUtil.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn("456");
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldAddNewOwner.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"));
    }

    @Test
    public void shouldNotCreateNewOwners() throws IOException, DuplicateRecordException, EntityCreationException, OwnerCreationException {
        when(keycloakAdminUtil.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn("456");
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldNotAddNewOwner.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"));
    }

    @Test
    public void shouldNotModifyExistingOwners() throws IOException, DuplicateRecordException, EntityCreationException, OwnerCreationException {
        when(keycloakAdminUtil.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn("456");
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldNotModifyExistingOwner.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"));
    }

    @Test
    public void shouldNotAllowUserModifyingOwnerFields() throws IOException, DuplicateRecordException, EntityCreationException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldNotModifyOwnerDetails.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"));
    }

    @Test
    public void shouldNotAllowUserModifyingSystemFields() throws IOException, DuplicateRecordException, EntityCreationException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldNotModifyOsStateByUser.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"));
    }

}