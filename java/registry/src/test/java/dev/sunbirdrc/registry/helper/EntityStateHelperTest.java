package dev.sunbirdrc.registry.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.keycloak.KeycloakAdminUtil;
import dev.sunbirdrc.keycloak.OwnerCreationException;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.workflow.KieConfiguration;
import dev.sunbirdrc.registry.exception.DuplicateRecordException;
import dev.sunbirdrc.registry.exception.EntityCreationException;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.workflow.RuleEngineService;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
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
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ObjectMapper.class,
        ConditionResolverService.class, ClaimRequestClient.class, KieConfiguration.class})
@Import(EntityStateHelperTestConfiguration.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EntityStateHelperTest {

    @Mock
    ConditionResolverService conditionResolverService;

    @Mock
    ClaimRequestClient claimRequestClient;

    @Mock
    KeycloakAdminUtil keycloakAdminUtil;

    DefinitionsManager definitionsManager;

    @Autowired
    KieContainer kieContainer;

    ObjectMapper m = new ObjectMapper();

    @Before
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);
        definitionsManager = new DefinitionsManager();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Definition> definitionMap = new HashMap<>();
        String studentSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
        String instituteSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Institute.json"), Charset.defaultCharset());
        definitionMap.put("Student", new Definition(objectMapper.readTree(studentSchema)));
        definitionMap.put("Institute", new Definition(objectMapper.readTree(instituteSchema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
    }

    private void runTest(JsonNode existing, JsonNode updated, JsonNode expected, List<AttestationPolicy> attestationPolicies) {
        RuleEngineService ruleEngineService = new RuleEngineService(kieContainer, keycloakAdminUtil);
        EntityStateHelper entityStateHelper = new EntityStateHelper(definitionsManager, ruleEngineService, conditionResolverService, claimRequestClient);
        ReflectionTestUtils.setField(entityStateHelper, "uuidPropertyName", "osid");
        entityStateHelper.applyWorkflowTransitions(existing, updated, attestationPolicies);
        assertEquals(expected, updated);
    }

    public void shouldMarkAsDraftWhenThereIsNewEntry() throws IOException {
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldMarkAsDraftWhenThereIsNewEntry.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("afterStateChange"),
                definitionsManager.getDefinition("Student").getOsSchemaConfiguration().getAttestationPolicies());
    }

    @NotNull
    private String getBaseDir() {
        return this.getClass().getResource("../../../../").getPath() + "entityStateHelper/";
    }

    public void shouldMarkAsDraftIfThereIsAChange() throws IOException {
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldMarkAsDraftIfThereIsAChange.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("afterStateChange"), Collections.emptyList());
    }

    @Test
    public void shouldBeNoStateChangeIfTheDataDidNotChange() throws IOException {
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldBeNoStateChangeIfTheDataDidNotChange.json"));
        JsonNode beforeUpdate = test.get("updated").deepCopy();
        runTest(test.get("existing"), test.get("updated"), test.get("existing"), Collections.emptyList());
    }

    @Test
    public void shouldCreateNewOwnersForNewlyAddedOwnerFields() throws IOException, DuplicateRecordException, EntityCreationException, OwnerCreationException {
        when(keycloakAdminUtil.createUser(anyString(), anyString(), anyString(), anyString(), any())).thenReturn("456");
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldAddNewOwner.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"), Collections.emptyList());
    }

    @Test
    public void shouldNotCreateNewOwners() throws IOException, DuplicateRecordException, EntityCreationException, OwnerCreationException {
        when(keycloakAdminUtil.createUser(anyString(), anyString(), anyString(), anyString(), any())).thenReturn("456");
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldNotAddNewOwner.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"), Collections.emptyList());
    }

    @Test
    public void shouldNotModifyExistingOwners() throws IOException, DuplicateRecordException, EntityCreationException, OwnerCreationException {
        when(keycloakAdminUtil.createUser(anyString(), anyString(), anyString(), anyString(),any())).thenReturn("456");
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldNotModifyExistingOwner.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"), Collections.emptyList());
    }

    @Test
    public void shouldNotAllowUserModifyingOwnerFields() throws IOException, DuplicateRecordException, EntityCreationException {
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldNotModifyOwnerDetails.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"), Collections.emptyList());
    }

    @Test
    public void shouldNotAllowUserModifyingSystemFields() throws IOException, DuplicateRecordException, EntityCreationException {
        JsonNode test = m.readTree(new File(getBaseDir() + "shouldNotModifyOsStateByUser.json"));
        runTest(test.get("existing"), test.get("updated"), test.get("expected"), Collections.emptyList());
    }

}