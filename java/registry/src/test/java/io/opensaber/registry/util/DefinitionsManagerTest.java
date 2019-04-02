package io.opensaber.registry.util;

import static org.junit.Assert.assertTrue;

import io.opensaber.registry.middleware.util.Constants;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsManager.class, OSResourceLoader.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class DefinitionsManagerTest {

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private ResourceLoader resourceLoader;
    
    private OSResourceLoader osResourceLoader;

    @Test
    public void testIfResourcesCountMatchesFileDefinitions() {
        boolean flag = false;
        osResourceLoader = new OSResourceLoader(resourceLoader);
        try {
            int nDefinitions = definitionsManager.getAllKnownDefinitions().size();
            int nResources = osResourceLoader.getResources(Constants.RESOURCE_LOCATION).length;
            flag = (2*nDefinitions == nResources);
        } catch (IOException ioe) {

        }
        assertTrue(flag);
    }
}