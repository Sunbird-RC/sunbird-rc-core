package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.AuditInfo;
import dev.sunbirdrc.pojos.AuditRecord;
import dev.sunbirdrc.registry.exception.AuditFailedException;
import dev.sunbirdrc.registry.helper.SignatureHelper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.IAuditService;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.OSSystemFieldsHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {ObjectMapper.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class AuditServiceImplTest {

    @Value("${audit.enabled}")
    private boolean auditEnabled;

    @Value("${audit.frame.store}")
    private String auditFrameStore;

    @Value("${audit.frame.suffix}")
    private String auditSuffix;

    @Value("${audit.frame.suffixSeparator}")
    private String auditSuffixSeparator;

    @Value("${audit.vc-enabled:false}")
    private boolean auditVCEnabled;

    @Mock
    private IDefinitionsManager definitionsManager;

    @Mock
    private OSSystemFieldsHelper systemFieldsHelper;

    @Mock
    private AuditProviderFactory auditProviderFactory;

    @Mock
    private SignatureHelper signatureHelper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        setField(auditService, "auditEnabled", auditEnabled);
        setField(auditService, "auditFrameStore", auditFrameStore);
        setField(auditService, "auditSuffix", auditSuffix);
        setField(auditService, "auditSuffixSeparator", auditSuffixSeparator);
        setField(auditService, "auditVCEnabled", auditVCEnabled);
    }

    private void setField(Object targetObject, String fieldName, Object value) throws Exception {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(targetObject, value);
    }

    @Test
    void shouldAudit_ReturnsTrueWhenFileAuditEnabled() throws Exception {
        setField(auditService, "auditFrameStore", Constants.FILE);
        boolean result = auditService.shouldAudit("EntityType");
        assertTrue(result);
    }

    @Test
    void shouldAudit_ReturnsTrueWhenDBAuditEnabledAndDefinitionNotNull() throws Exception {
        when(definitionsManager.getDefinition(anyString())).thenReturn(mock(Definition.class));
        setField(auditService, "auditFrameStore", Constants.DATABASE);
        boolean result = auditService.shouldAudit("EntityType");
        assertTrue(result);
    }

    @Test
    void shouldAudit_ReturnsFalseWhenNoAuditingEnabled() throws Exception {
        setField(auditService, "auditEnabled", false);
        boolean result = auditService.shouldAudit("EntityType");
        assertFalse(result);
    }

    @Test
    void isAuditAction_ReturnsAuditActionForMatchingSuffix() {
        String entityType = auditSuffix;
        String action = auditService.isAuditAction(entityType);
        assertEquals(Constants.AUDIT_ACTION_AUDIT, action);
    }

    @Test
    void isAuditAction_ReturnsSearchActionForNonMatchingSuffix() {
        String entityType = "NonMatchingEntity";
        String action = auditService.isAuditAction(entityType);
        assertEquals(Constants.AUDIT_ACTION_SEARCH, action);
    }

    @Test
    void createAuditInfo_ReturnsAuditInfoList() {
        String auditAction = "AuditAction";
        String entityType = "EntityType";
        List<AuditInfo> auditInfoList = auditService.createAuditInfo(auditAction, entityType);

        // Then
        assertEquals(1, auditInfoList.size());
        assertEquals(auditAction, auditInfoList.get(0).getOp());
        assertEquals("/" + entityType, auditInfoList.get(0).getPath());
    }

    @Test
    void convertAuditRecordToJson_ReturnsJsonNode() throws IOException {
        // Given
        AuditRecord auditRecord = new AuditRecord();
        String vertexLabel = "VertexLabel";

        // When
        JsonNode result = auditService.convertAuditRecordToJson(auditRecord, vertexLabel);

        // Then
        assertNotNull(result);
        assertTrue(result.has(vertexLabel));
    }

    @Test
    void createAuditInfoWithJson_ReturnsAuditInfoListFromJson() throws JsonProcessingException {
        // Given
        String auditAction = "AuditAction";
        JsonNode differenceJson = mock(JsonNode.class);
        String entityType = "EntityType";
        when(objectMapper.treeToValue(any(JsonNode.class), eq(AuditInfo[].class)))
                .thenReturn(new AuditInfo[]{new AuditInfo()});

        // When
        List<AuditInfo> auditInfoList = auditService.createAuditInfoWithJson(auditAction, differenceJson, entityType);

        // Then
        assertNotNull(auditInfoList);
        assertEquals(1, auditInfoList.size());
    }

    @Test
    void testDoAudit() throws AuditFailedException {
        // Given
        AuditRecord auditRecord = mock(AuditRecord.class);
        JsonNode inputNode = mock(JsonNode.class);
        Shard shard = mock(Shard.class);
        IAuditService auditProvider = mock(IAuditService.class);

        when(auditProviderFactory.getAuditService(anyString())).thenReturn(auditProvider);

        // When
        auditService.doAudit(auditRecord, inputNode, shard);

        // Then
        verify(auditProvider, times(1)).doAudit(auditRecord, inputNode, shard);
    }
}