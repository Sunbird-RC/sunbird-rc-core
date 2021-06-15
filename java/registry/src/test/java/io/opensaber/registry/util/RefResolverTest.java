package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.middleware.util.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsManager.class, OSResourceLoader.class, RefResolver.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RefResolverTest {

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private ResourceLoader resourceLoader;

    private OSResourceLoader osResourceLoader;

    @Autowired
    private RefResolver refResolver;

    @Before
    public void setUp() throws Exception {
        osResourceLoader = new OSResourceLoader(resourceLoader);
    }

    @Test
    public void testShouldResolveSchemaRef() {
        JsonNode studentSchema = refResolver.getResolvedSchema("Student", "properties");
        assertTrue(studentSchema.at("/properties/Student/$ref").isMissingNode());
    }
}