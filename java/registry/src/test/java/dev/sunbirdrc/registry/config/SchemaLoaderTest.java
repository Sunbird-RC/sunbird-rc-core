package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.ISearchService;
import dev.sunbirdrc.registry.service.SchemaService;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SchemaLoaderTest {

    SchemaService schemaService = new SchemaService();

    @InjectMocks
    SchemaLoader schemaLoader;

    @Mock
    ISearchService searchService;

    ObjectMapper objectMapper = new ObjectMapper();

    DefinitionsManager definitionsManager;

    @BeforeEach
    public void setUp() throws Exception {
        definitionsManager = new DefinitionsManager();
        ReflectionTestUtils.setField(schemaLoader, "schemaService", schemaService);
        ReflectionTestUtils.setField(schemaService, "definitionsManager", definitionsManager);
    }

    @Test
    public void shouldLoadSchemasToDefinitionManager() throws IOException {
        when(searchService.search(any(), anyString())).thenReturn(objectMapper.readTree("{\n" +
            "  \"status\": \"PUBLISHED\",\n" +
            "  \"osOwner\": [\n" +
            "    \"d9e68be4-205a-4b44-8301-1fea2557f1cf\"\n" +
            "  ],\n" +
            "  \"osCreatedAt\": \"2022-12-16T11:12:23.347Z\",\n" +
            "  \"osUpdatedAt\": \"2022-12-16T11:12:23.347Z\",\n" +
            "  \"osCreatedBy\": \"d9e68be4-205a-4b44-8301-1fea2557f1cf\",\n" +
            "  \"osUpdatedBy\": \"d9e68be4-205a-4b44-8301-1fea2557f1cf\",\n" +
            "  \"osid\": \"1-e6042101-c6c7-4a62-a448-68e663b0c3c9\"\n" +
            "}"));
        schemaLoader.onApplicationEvent(new ContextRefreshedEvent(new GenericApplicationContext()));
        assertEquals(1, definitionsManager.getAllDefinitions().size());
    }
}