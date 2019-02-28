package io.opensaber.registry.util;

import io.opensaber.registry.service.ISearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchAdvisor {
    private static Logger logger = LoggerFactory.getLogger(SearchAdvisor.class);    
    private static final String DEFAULT_SEARCH_ADVISOR = "io.opensaber.registry.service.impl.NativeSearchService"; 

    public ISearchService getInstance(String advisorClassName) {

        ISearchService searchService = null;
        try {
            if (advisorClassName == null || advisorClassName.isEmpty()) {
                // default is set to native search service
                advisorClassName = DEFAULT_SEARCH_ADVISOR;
            }
            Class<?> advisorClass = Class.forName(advisorClassName);
            searchService = (ISearchService) advisorClass.newInstance();
            logger.info("Invoked search advisor class with classname: " + advisorClassName);

        } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException e) {
            logger.error("Search advisor class {} cannot be instantiate with exception:", advisorClassName, e);
        }

        return searchService;
    }

}
