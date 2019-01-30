package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsReader.class, DefinitionsManager.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class DefinitionsManagerTest {

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private DefinitionsReader definitionsReader;

    @Test
    public void testIfResourcesCountMatchesFileDefinitions() {
        boolean flag = false;
        try {
            int nDefinitions = definitionsManager.getAllKnownDefinitions().size();
            int nResources = definitionsReader.getResources(Constants.RESOURCE_LOCATION).length;
            flag = (nDefinitions == nResources);
        } catch (IOException ioe) {

        }
        assertTrue(flag);
    }
}