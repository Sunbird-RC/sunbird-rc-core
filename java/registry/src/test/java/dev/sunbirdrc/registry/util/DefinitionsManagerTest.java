package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class DefinitionsManagerTest {

    private DefinitionsManager definitionsManager;


    @Before
    public void setup() throws IOException {
        definitionsManager = new DefinitionsManager();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Definition> definitionMap = new HashMap<>();
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        definitionMap.put("TrainingCertificate", new Definition(objectMapper.readTree(schema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
        ReflectionTestUtils.setField(definitionsManager, "objectMapper", objectMapper);
    }

    @Test
    public void testIfResourcesCountMatchesFileDefinitions() {
        assertTrue(definitionsManager.getAllKnownDefinitions().size() == 1);
    }

    @Test
    public void testShouldReturnGetOwnershipAttributes() {
        String entity = "TrainingCertificate";
        List<OwnershipsAttributes> ownershipsAttributes = definitionsManager.getOwnershipAttributes(entity);
        assertEquals(1, ownershipsAttributes.size());
        assertEquals("/contact", ownershipsAttributes.get(0).getEmail());
        assertEquals("/contact", ownershipsAttributes.get(0).getMobile());
        assertEquals("/contact", ownershipsAttributes.get(0).getUserId());
    }

    @Test
    public void testGetOwnershipAttributesForInvalidEntity() {
        String entity = "UnknownEntity";
        List<OwnershipsAttributes> ownershipsAttributes = definitionsManager.getOwnershipAttributes(entity);
        assertEquals(0, ownershipsAttributes.size());
    }

    @Test
    public void testGetOwnershipAttributesShouldReturnEmpty() {
        String entity = "Common";
        List<OwnershipsAttributes> ownershipsAttributes = definitionsManager.getOwnershipAttributes(entity);
        assertEquals(0, ownershipsAttributes.size());
    }

    @Test
    public void testShouldReturnTrueForValidEntityName() {
        String entity = "TrainingCertificate";
        assertTrue(definitionsManager.isValidEntityName(entity));
    }

    @Test
    public void testShouldReturnFalseForInValidEntityName() {
        String entity = "XYZ";
        assertFalse(definitionsManager.isValidEntityName(entity));
    }

    @Test
    public void testShouldReturnEntitiesWithAnonymousInviteRoles() throws IOException {
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        schema = schema.replaceAll("TrainingCertificate", "SkillCertificate");
        definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
        List<String> entities = definitionsManager.getEntitiesWithAnonymousInviteRoles();
        assertEquals(Arrays.asList("TrainingCertificate", "SkillCertificate"), entities);
    }

    @Test
    public void testShouldReturnEntitiesWithAnonymousManageRoles() throws IOException {
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        schema = schema.replaceAll("TrainingCertificate", "SkillCertificate");
        schema = schema.replaceAll("admin", "anonymous");
        definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
        List<String> entities = definitionsManager.getEntitiesWithAnonymousManageRoles();
        assertEquals(Arrays.asList("SkillCertificate"), entities);
    }
}