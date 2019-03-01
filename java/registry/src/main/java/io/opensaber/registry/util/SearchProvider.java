package io.opensaber.registry.util;

import io.opensaber.registry.service.ISearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchProvider {
    private static Logger logger = LoggerFactory.getLogger(SearchProvider.class);    
    private static final String DEFAULT_SEARCH_ADVISOR = "io.opensaber.registry.service.NativeSearchService"; 

    public ISearchService getInstance(String advisorProviderName) {

        ISearchService searchService = null;
        try {
            if (advisorProviderName == null || advisorProviderName.isEmpty()) {
                // default is set to native search service
                advisorProviderName = DEFAULT_SEARCH_ADVISOR;
            }
            Class<?> advisorClass = Class.forName(advisorProviderName);
            searchService = (ISearchService) advisorClass.newInstance();
            logger.info("Invoked search provider class with classname: " + advisorProviderName);

        } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException e) {
            logger.error("Search provider class {} cannot be instantiate with exception:", advisorProviderName, e);
        }

        return searchService;
    }

}
