package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DefinitionsManager.class, OSResourceLoader.class, RefResolver.class, ObjectMapper.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RefResolverTest {

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private ResourceLoader resourceLoader;

    private OSResourceLoader osResourceLoader;

    @Autowired
    private RefResolver refResolver;

    @BeforeEach
    public void setUp() throws Exception {
        osResourceLoader = new OSResourceLoader(resourceLoader);
    }

    @Test
    public void testShouldResolveSchemaRef() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Definition> definitionMap = new HashMap<>();
        String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
        String instituteSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Institute.json"), Charset.defaultCharset());
        definitionMap.put("Student", new Definition(objectMapper.readTree(schema)));
        definitionMap.put("Institute", new Definition(objectMapper.readTree(instituteSchema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
        JsonNode studentSchema = refResolver.getResolvedSchema("Student", "properties");
        assertTrue(studentSchema.at("/properties/Student/$ref").isMissingNode());
    }
}