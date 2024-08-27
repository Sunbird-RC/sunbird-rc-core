package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class DefinitionsManagerTest {

    private DefinitionsManager definitionsManager;

    @BeforeEach
    void setup() throws IOException {
        definitionsManager = new DefinitionsManager();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Definition> definitionMap = new HashMap<>();
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        definitionMap.put("TrainingCertificate", new Definition(objectMapper.readTree(schema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
    }

    @Test
    void testIfResourcesCountMatchesFileDefinitions() {
        assertTrue(definitionsManager.getAllKnownDefinitions().size() == 1);
    }

    @Test
    void testShouldReturnGetOwnershipAttributes() {
        String entity = "TrainingCertificate";
        List<OwnershipsAttributes> ownershipsAttributes = definitionsManager.getOwnershipAttributes(entity);
        assertEquals(1, ownershipsAttributes.size());
        assertEquals("/contact", ownershipsAttributes.get(0).getEmail());
        assertEquals("/contact", ownershipsAttributes.get(0).getMobile());
        assertEquals("/contact", ownershipsAttributes.get(0).getUserId());
    }

    @Test
    void testGetOwnershipAttributesForInvalidEntity() {
        String entity = "UnknownEntity";
        List<OwnershipsAttributes> ownershipsAttributes = definitionsManager.getOwnershipAttributes(entity);
        assertEquals(0, ownershipsAttributes.size());
    }

    @Test
    void testGetOwnershipAttributesShouldReturnEmpty() {
        String entity = "Common";
        List<OwnershipsAttributes> ownershipsAttributes = definitionsManager.getOwnershipAttributes(entity);
        assertEquals(0, ownershipsAttributes.size());
    }

    @Test
    void testShouldReturnTrueForValidEntityName() {
        String entity = "TrainingCertificate";
        assertTrue(definitionsManager.isValidEntityName(entity));
    }

    @Test
    void testShouldReturnFalseForInValidEntityName() {
        String entity = "XYZ";
        assertFalse(definitionsManager.isValidEntityName(entity));
    }

    @Test
    void testShouldReturnEntitiesWithAnonymousInviteRoles() throws IOException {
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        schema = schema.replaceAll("TrainingCertificate", "SkillCertificate");
        definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
        List<String> entities = definitionsManager.getEntitiesWithAnonymousInviteRoles();
        assertEquals(Arrays.asList("TrainingCertificate", "SkillCertificate"), entities);
    }

    @Test
    void testShouldReturnEntitiesWithAnonymousManageRoles() throws IOException {
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        schema = schema.replaceAll("TrainingCertificate", "SkillCertificate");
        schema = schema.replaceAll("admin", "anonymous");
        definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
        List<String> entities = definitionsManager.getEntitiesWithAnonymousManageRoles();
        assertEquals(Arrays.asList("SkillCertificate"), entities);
    }
}