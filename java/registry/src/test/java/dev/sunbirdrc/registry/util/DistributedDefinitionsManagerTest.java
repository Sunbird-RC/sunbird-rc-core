package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {OSResourceLoader.class, ObjectMapper.class, JedisPool.class, Jedis.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class DistributedDefinitionsManagerTest {
    private static final String SCHEMA = "SCHEMA_";
    private static final String SCHEMA_WILDCARD = SCHEMA + "*";

    @InjectMocks
    @Spy
    DistributedDefinitionsManager distributedDefinitionsManager;

    @MockBean
    private ResourceLoader resourceLoader;
    @MockBean
    private ObjectMapper objectMapper;
    @MockBean
    private JedisPool jedisPool;
    @MockBean
    private Jedis jedis;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
//        when(jedisPool.getResource()).thenReturn(jedis);
    }

    @Test
    public void shouldGetAllKnownDefinitionsFromRedis() {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>();
        keys.add("SCHEMA_TrainingCertificate");
        when(jedis.keys(SCHEMA_WILDCARD)).thenReturn(keys);
        Set<String> expectedKeys = distributedDefinitionsManager.getAllKnownDefinitions();
        verify(jedis, times(1)).keys(SCHEMA_WILDCARD);
        assertEquals(1, expectedKeys.size());
        assertEquals("TrainingCertificate", expectedKeys.toArray(new String[keys.size()])[0]);
    }

    @Test
    public void shouldGetAllDefinitionsFromRedis() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>();
        keys.add("SCHEMA_TrainingCertificate");
        when(jedis.keys(SCHEMA_WILDCARD)).thenReturn(keys);
        List<String> definitionsStr = new ArrayList<>();
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        definitionsStr.add(schema);
        when(jedis.mget(keys.toArray(new String[keys.size()]))).thenReturn(definitionsStr);
        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode node = objectMapper1.readTree(schema);
        when(objectMapper.readTree(schema)).thenReturn(node);
        List<Definition> definitions = distributedDefinitionsManager.getAllDefinitions();
        verify(jedis, times(1)).keys(SCHEMA_WILDCARD);
        verify(objectMapper, times(1)).readTree(schema);
        assertEquals(1, definitions.size());
    }

    @Test
    public void shouldGetValidDefinitionIfCorrectTitlePassed() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        final String SCHEMA = "SCHEMA_";
        when(jedis.get(SCHEMA + "TrainingCertificate")).thenReturn(schema);
        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode node = objectMapper1.readTree(schema);
        when(objectMapper.readTree(schema)).thenReturn(node);
        assertNotNull(distributedDefinitionsManager.getDefinition("TrainingCertificate"));
    }

    @Test
    public void shouldReturnNullIfInvalidTitlePassed() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("Institute")).thenReturn(null);
        assertNull(distributedDefinitionsManager.getDefinition("Institute"));
    }

    @Test
    void shouldReturnPublicFieldsFromDefinition() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>();
        final String SCHEMA = "SCHEMA_";
        keys.add(SCHEMA + "TrainingCertificate");
        when(jedis.keys(SCHEMA_WILDCARD)).thenReturn(keys);
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        when(jedis.get("SCHEMA_TrainingCertificate")).thenReturn(schema);
        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode node = objectMapper1.readTree(schema);
        when(objectMapper.readTree(schema)).thenReturn(node);
        Map<String, Set<String>> expectedResult = new HashMap<>();
        expectedResult.put("trainingcertificate", new HashSet<>());
        Map<String, Set<String>> actualResult = distributedDefinitionsManager.getPublicFieldsInfoMap();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void shouldReturnExcludingFieldsFromDefinition() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>();
        final String SCHEMA = "SCHEMA_";
        keys.add("SCHEMA_TrainingCertificate");
        when(jedis.keys(SCHEMA_WILDCARD)).thenReturn(keys);
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        when(jedis.get("SCHEMA_TrainingCertificate")).thenReturn(schema);
        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode node = objectMapper1.readTree(schema);
        when(objectMapper.readTree(schema)).thenReturn(node);
        Map<String, Set<String>> expectedResult = new HashMap<>();
        expectedResult.put("trainingcertificate", new HashSet<>());
        Map<String, Set<String>> actualResult = distributedDefinitionsManager.getExcludingFields();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void shouldReturnOwnershipAttributesForKnownEntity() throws IOException {
        String entity = "TrainingCertificate";
        final String SCHEMA = "SCHEMA_";
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
        when(jedis.get(SCHEMA + "TrainingCertificate")).thenReturn(schema);
        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode node = objectMapper1.readTree(schema);
        when(objectMapper.readTree(schema)).thenReturn(node);
        when(jedisPool.getResource()).thenReturn(jedis);
        List<OwnershipsAttributes> ownershipsAttributes = distributedDefinitionsManager.getOwnershipAttributes(entity);
        assertEquals(1, ownershipsAttributes.size());
        assertEquals("/contact", ownershipsAttributes.get(0).getEmail());
        assertEquals("/contact", ownershipsAttributes.get(0).getMobile());
        assertEquals("/contact", ownershipsAttributes.get(0).getUserId());
    }

    @Test
    public void shouldReturnEmptyOwnershipAttributesForUnknownEntity() {
        when(jedisPool.getResource()).thenReturn(jedis);
        String entity = "UnknownEntity";
        when(jedis.get(entity)).thenReturn(null);
        List<OwnershipsAttributes> ownershipsAttributes = distributedDefinitionsManager.getOwnershipAttributes(entity);
        assertEquals(0, ownershipsAttributes.size());
    }

    @Test
    public void shouldReturnTrueForValidEntityName() {
        when(jedisPool.getResource()).thenReturn(jedis);
        String entity = "TrainingCertificate";
        final String SCHEMA = "SCHEMA_";
        when(jedis.exists(SCHEMA + entity)).thenReturn(true);
        assertTrue(distributedDefinitionsManager.isValidEntityName(entity));
    }

    @Test
    public void shouldReturnFalseForInValidEntityName() {
        when(jedisPool.getResource()).thenReturn(jedis);
        String entity = "UnknownEntity";
        when(jedis.exists(entity)).thenReturn(false);
        assertFalse(distributedDefinitionsManager.isValidEntityName(entity));
    }

    @Test
    public void shouldAppendNewDefinition() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        String schema = "{\n   \"$schema\": \"http://json-schema.org/draft-07/schema\",\n   \"type\": \"object\",\n   \"properties\": {\n      \"Place\": {\n         \"$ref\": \"#/definitions/Place\"\n      }\n   },\n   \"required\": [\n      \"Place\"\n   ],\n   \"title\": \"Place\",\n   \"definitions\": {\n      \"Place\": {\n         \"$id\": \"#/properties/Place\",\n         \"type\": \"object\",\n         \"title\": \"The Place Schema\",\n         \"required\": [\n            \"name\",\n            \"city\",\n            \"addressRegion\",\n            \"country\"\n         ],\n         \"properties\": {\n            \"name\": {\n               \"type\": \"string\"\n            },\n            \"city\": {\n               \"type\": \"string\"\n            },\n            \"addressLocality\": {\n               \"type\": \"string\"\n            },\n            \"addressRegion\": {\n               \"type\": \"string\"\n            },\n            \"country\": {\n               \"type\": \"string\"\n            },\n            \"postalCode\": {\n               \"type\": \"string\"\n            },\n            \"contact\": {\n               \"type\": \"string\"\n            },\n            \"email\": {\n               \"type\": \"string\"\n            }\n         }\n      }\n   },\n   \"_osConfig\": {\n      \"privateFields\": [\n         \"name\"\n      ],\n      \"signedFields\": [\n         \"country\"\n      ],\n      \"roles\": [\n         \"anonymous\"\n      ],\n      \"ownershipAttributes\": [],\n      \"attestationPolicies\": [\n         {\n            \"name\": \"schemaAttestation\",\n            \"conditions\": \"(ATTESTOR#$.[*]#.contains('board-cbse'))\",\n            \"type\": \"AUTOMATED\",\n            \"attestorPlugin\": \"did:internal:ClaimPluginActor?entity=board-cbse\",\n            \"attestationProperties\": {\n               \"country\": \"$.country\",\n               \"contact\": \"$.contact\"\n            }\n         }\n      ],\n      \"credentialTemplate\": {\n         \"@context\": [\n            \"https://www.w3.org/2018/credentials/v1\",\n            \"https://gist.githubusercontent.com/varadeth/c781559f8d3954fda040d1be0fb2187d/raw/7e951447b3aaf670d407068274fe3ace814c55a4/gistfile1.json\"\n         ],\n         \"type\": [\n            \"VerifiableCredential\",\n            \"AttestationCertificate\"\n         ],\n         \"issuer\": \"http://www.india.gov.in\",\n         \"issuanceDate\": \"2022-08-08T12:00:00Z\",\n         \"credentialSubject\": {\n            \"type\": \"Place\",\n            \"name\": \"{{name}}\",\n            \"country\": \"{{country}}\"\n         },\n         \"evidence\": {\n            \"type\": \"Affiliation\",\n            \"postalCode\": \"{{postalCode}}\",\n            \"contact\": \"{{contact}}\"\n         }\n      },\n      \"certificateTemplates\": {\n        \"html\": \"https://gist.githubusercontent.com/varadeth/2e8adcddfd377c22e8bcd95bcc929d68/raw/e063ec3e2de3d90d2a9c1763fd417cd922135a96/something.html\"\n      }\n   }\n}";
        TextNode node = new TextNode(schema);
        ObjectMapper objectMapper1 = new ObjectMapper();
        distributedDefinitionsManager.appendNewDefinition(node);
        verify(jedis, times(1)).set("SCHEMA_Place", objectMapper1.readTree(node.textValue()).toString());
    }

    @Test
    public void shouldRemoveDefinition() throws JsonProcessingException {
        when(jedisPool.getResource()).thenReturn(jedis);
        String schema = "{\n   \"$schema\": \"http://json-schema.org/draft-07/schema\",\n   \"type\": \"object\",\n   \"properties\": {\n      \"Place\": {\n         \"$ref\": \"#/definitions/Place\"\n      }\n   },\n   \"required\": [\n      \"Place\"\n   ],\n   \"title\": \"Place\",\n   \"definitions\": {\n      \"Place\": {\n         \"$id\": \"#/properties/Place\",\n         \"type\": \"object\",\n         \"title\": \"The Place Schema\",\n         \"required\": [\n            \"name\",\n            \"city\",\n            \"addressRegion\",\n            \"country\"\n         ],\n         \"properties\": {\n            \"name\": {\n               \"type\": \"string\"\n            },\n            \"city\": {\n               \"type\": \"string\"\n            },\n            \"addressLocality\": {\n               \"type\": \"string\"\n            },\n            \"addressRegion\": {\n               \"type\": \"string\"\n            },\n            \"country\": {\n               \"type\": \"string\"\n            },\n            \"postalCode\": {\n               \"type\": \"string\"\n            },\n            \"contact\": {\n               \"type\": \"string\"\n            },\n            \"email\": {\n               \"type\": \"string\"\n            }\n         }\n      }\n   },\n   \"_osConfig\": {\n      \"privateFields\": [\n         \"name\"\n      ],\n      \"signedFields\": [\n         \"country\"\n      ],\n      \"roles\": [\n         \"anonymous\"\n      ],\n      \"ownershipAttributes\": [],\n      \"attestationPolicies\": [\n         {\n            \"name\": \"schemaAttestation\",\n            \"conditions\": \"(ATTESTOR#$.[*]#.contains('board-cbse'))\",\n            \"type\": \"AUTOMATED\",\n            \"attestorPlugin\": \"did:internal:ClaimPluginActor?entity=board-cbse\",\n            \"attestationProperties\": {\n               \"country\": \"$.country\",\n               \"contact\": \"$.contact\"\n            }\n         }\n      ],\n      \"credentialTemplate\": {\n         \"@context\": [\n            \"https://www.w3.org/2018/credentials/v1\",\n            \"https://gist.githubusercontent.com/varadeth/c781559f8d3954fda040d1be0fb2187d/raw/7e951447b3aaf670d407068274fe3ace814c55a4/gistfile1.json\"\n         ],\n         \"type\": [\n            \"VerifiableCredential\",\n            \"AttestationCertificate\"\n         ],\n         \"issuer\": \"http://www.india.gov.in\",\n         \"issuanceDate\": \"2022-08-08T12:00:00Z\",\n         \"credentialSubject\": {\n            \"type\": \"Place\",\n            \"name\": \"{{name}}\",\n            \"country\": \"{{country}}\"\n         },\n         \"evidence\": {\n            \"type\": \"Affiliation\",\n            \"postalCode\": \"{{postalCode}}\",\n            \"contact\": \"{{contact}}\"\n         }\n      },\n      \"certificateTemplates\": {\n        \"html\": \"https://gist.githubusercontent.com/varadeth/2e8adcddfd377c22e8bcd95bcc929d68/raw/e063ec3e2de3d90d2a9c1763fd417cd922135a96/something.html\"\n      }\n   }\n}";
        TextNode node = new TextNode(schema);
        ObjectMapper objectMapper1 = new ObjectMapper();
        when(objectMapper.readTree(schema)).thenReturn(objectMapper1.readTree(schema));
        distributedDefinitionsManager.removeDefinition(node);
        verify(jedis, times(1)).del("SCHEMA_Place");
    }
}