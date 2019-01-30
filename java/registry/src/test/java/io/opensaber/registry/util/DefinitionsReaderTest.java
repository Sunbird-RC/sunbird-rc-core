package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsReader.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class DefinitionsReaderTest {
    @Autowired
    private DefinitionsReader definitionsReader;

    @Test
    public void whenAtLeastOneDefinitionPresent_Then_ok() {
        Resource[] resources = null;
        try {
            resources = definitionsReader.getResources(Constants.RESOURCE_LOCATION);
        } catch (IOException ioe) {
            assertFalse(true);
        }
        assertTrue("At least one definition must be present", resources != null && resources.length >= 1);
    }
}