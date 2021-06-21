package io.opensaber.registry.model.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AttestationPathTest {

    private static final ObjectMapper m = new ObjectMapper();
    private JsonNode node;
    private static final String UUID_PROP = "osid";

    private Set<String> convertToJsonPaths(Set<EntityPropertyURI> pointers) {
        return pointers.stream().map(EntityPropertyURI::getPropertyURI).collect(Collectors.toSet());
    }

    @Before
    public void setUp() throws IOException {
        node = m.readTree(new File("src/test/resources/attestationPathTest/example.json"));
    }

    @Test
    public void testGetPointers_nestedArray() throws Exception {
        List<String> expectedUUIDPaths = Arrays.asList(
                "/education/1-324a-123/courses/0/0",
                "/education/1-324a-123/courses/0/1",
                "/education/1-324a-123/courses/0/2",
                "/education/1-324a-123/courses/1/0",
                "/education/1-324a-123/courses/1/1",
                "/education/1-324a-123/courses/1/2",
                "/education/1-324a-123/courses/2/0",
                "/education/1-324a-123/courses/2/1",
                "/education/1-324a-123/courses/3/0",
                "/education/1-324a-121/courses/0/0",
                "/education/1-324a-121/courses/0/1",
                "/education/1-324a-121/courses/1/0",
                "/education/1-324a-121/courses/1/1",
                "/education/1-324a-121/courses/2/0",
                "/education/1-324a-121/courses/3/0"
        );

        Set<EntityPropertyURI> pointers = new AttestationPath("education/[]/courses/[]/[]").getEntityPropertyURIs(node, UUID_PROP);
        assertTrue(convertToJsonPaths(pointers).containsAll(expectedUUIDPaths));
    }
    @Test
    public void testGetPointers_fieldPath() throws Exception {
        Set<EntityPropertyURI> pointers = new AttestationPath("education").getEntityPropertyURIs(node, UUID_PROP);
        assertEquals(1, pointers.size());
        assertTrue(convertToJsonPaths(pointers).contains("/education"));
    }

    @Test
    public void testGetPointers_fieldInsideArray() throws Exception {
        Set<EntityPropertyURI> pointers = new AttestationPath("education/[]/courses").getEntityPropertyURIs(node, UUID_PROP);
        assertEquals(2, pointers.size());
    }

    @Test
    public void testGetPointers_twoArrayAncestors() throws Exception {
        List<String> expectedUUIDPaths = Arrays.asList(
                "/education/1-324a-123/awards/1-awd-001",
                "/education/1-324a-123/awards/1-awd-002",
                "/education/1-324a-121/awards/1-awd-0021"
        );

        Set<EntityPropertyURI> pointers = new AttestationPath("education/[]/awards/[]").getEntityPropertyURIs(node, UUID_PROP);
        assertTrue(convertToJsonPaths(pointers).containsAll(expectedUUIDPaths));
    }
}