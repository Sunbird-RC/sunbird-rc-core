package io.opensaber.registry.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Loads any given pattern of resources
 *
 */
@Component("definitionsReader")
public class DefinitionsReader {
    private ResourceLoader resourceLoader;

    @Autowired
    public DefinitionsReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Loads the resources with a given pattern
     * @param pattern "Example: *.json to load all json files"
     * @return
     * @throws IOException
     */
    public Resource[] getResources(String pattern) throws IOException {
        Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(pattern);
        return resources;
    }
}
