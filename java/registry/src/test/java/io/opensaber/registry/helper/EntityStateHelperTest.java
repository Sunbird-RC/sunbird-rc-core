package io.opensaber.registry.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RuleEngineService;
import io.opensaber.registry.util.ClaimRequestClient;
import io.opensaber.registry.util.DefinitionsManager;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ObjectMapper.class, DefinitionsManager.class, EntityStateHelper.class,
        RuleEngineService.class, ConditionResolverService.class, ClaimRequestClient.class})
@Import(EntityStateHelperTestConfiguration.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EntityStateHelperTest {

    @Mock
    ConditionResolverService conditionResolverService;

    @Mock
    ClaimRequestClient claimRequestClient;

    @Autowired
    DefinitionsManager definitionsManager;

    @Autowired
    RuleEngineService ruleEngineService;

    @Autowired @InjectMocks
    EntityStateHelper entityStateHelper;

    private static final String TEST_DIR = "src/test/resources/entityStateHelper/";

    ObjectMapper m = new ObjectMapper();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    private void runTest(JsonNode existing, JsonNode updated, JsonNode expected) {
        entityStateHelper.changeStateAfterUpdate(existing, updated);
        assertEquals(expected, updated);
    }

    @Test
    public void shouldMarkAsDraftWhenThereIsNewEntry() throws IOException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldMarkAsDraftWhenThereIsNewEntry.json"));
        runTest(test.get("existing"),  test.get("updated"), test.get("afterStateChange"));
    }

    @Test
    public void shouldMarkAsDraftIfThereIsAChange() throws IOException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldMarkAsDraftIfThereIsAChange.json"));
        runTest(test.get("existing"),  test.get("updated"), test.get("afterStateChange"));
    }

    @Test
    public void shouldBeNoStateChangeIfTheDataDidNotChange() throws IOException {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldBeNoStateChangeIfTheDataDidNotChange.json"));
        JsonNode beforeUpdate = test.get("updated").deepCopy();
        runTest(test.get("existing"),  test.get("updated"), beforeUpdate);
    }

    @Test
    public void shouldRaiseClaimWhenSentForAttestation() throws Exception {
        JsonNode test = m.readTree(new File(TEST_DIR + "shouldRaiseClaimWhenSentForAttestation.json"));
        String propertyURI = "educationDetails/fgyuhij";
        HashMap<String, Object> mockRaiseClaimResponse = new HashMap<>();
        mockRaiseClaimResponse.put("id", "raised_claim_id");

        Mockito.when(conditionResolverService.resolve(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn("");
        Mockito.when(claimRequestClient.riseClaimRequest(ArgumentMatchers.any()))
                .thenReturn(mockRaiseClaimResponse);

        assertEquals(test.get("expected"), entityStateHelper.sendForAttestation(test.get("existing"), propertyURI));

    }
}