package dev.sunbirdrc.config.validation;

import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBConnectionInfoMgrTest {

    private final static String NOT_EMPTY = "not empty value";
    private final static String EMPTY = "";
    private final String[] DUPLICATE_SHARD_VALUES = {"shardval", "shardval"};

    private Validator validator;

    @BeforeEach
    void setUp() throws Exception {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void testEmptyUuidProperty() {
        DBConnectionInfoMgr mgr = new DBConnectionInfoMgr();
        mgr.setProvider(NOT_EMPTY);
        mgr.setUuidPropertyName(EMPTY);
        Set<ConstraintViolation<DBConnectionInfoMgr>> violations = validator.validate(mgr);
        assertEquals(1, violations.size());
    }

    @Test
    void testEmptyProvider() {
        DBConnectionInfoMgr mgr = new DBConnectionInfoMgr();
        mgr.setProvider(EMPTY);
        mgr.setUuidPropertyName(NOT_EMPTY);
        Set<ConstraintViolation<DBConnectionInfoMgr>> violations = validator.validate(mgr);
        assertEquals(1, violations.size());
    }

    @Test
    void testEmptyShardId() {
        List<DBConnectionInfo> connectionInfos = new ArrayList<>();
        DBConnectionInfo ci = new DBConnectionInfo();
        ci.setShardId(EMPTY);
        ci.setShardLabel(NOT_EMPTY);
        ci.setUri(NOT_EMPTY);
        connectionInfos.add(ci);
        DBConnectionInfoMgr mgr = new DBConnectionInfoMgr();
        mgr.setProvider(NOT_EMPTY);
        mgr.setUuidPropertyName(NOT_EMPTY);
        mgr.setConnectionInfo(connectionInfos);
        Set<ConstraintViolation<DBConnectionInfoMgr>> violations = validator.validate(mgr);
        assertEquals(1, violations.size());
    }

    @Test
    void testEmptyShardLabel() {
        List<DBConnectionInfo> connectionInfos = new ArrayList<>();
        DBConnectionInfo ci0 = new DBConnectionInfo();
        ci0.setShardId(NOT_EMPTY);
        ci0.setShardLabel(NOT_EMPTY);
        ci0.setUri(NOT_EMPTY);
        connectionInfos.add(ci0);
        DBConnectionInfo ci1 = new DBConnectionInfo();
        ci1.setShardId(NOT_EMPTY);
        ci1.setShardLabel(EMPTY);
        ci1.setUri(NOT_EMPTY);
        connectionInfos.add(ci1);
        DBConnectionInfoMgr mgr = new DBConnectionInfoMgr();
        mgr.setProvider(NOT_EMPTY);
        mgr.setUuidPropertyName(NOT_EMPTY);
        mgr.setConnectionInfo(connectionInfos);
        Set<ConstraintViolation<DBConnectionInfoMgr>> violations = validator.validate(mgr);
        assertEquals(1, violations.size());
    }

    @Test
    void testDuplicateShardValue() {
        List<DBConnectionInfo> connectionInfosWithDuplicateShardLabelValues = getDBConnectionInfoList(
                DUPLICATE_SHARD_VALUES);
        DBConnectionInfoMgr mgr = new DBConnectionInfoMgr();
        mgr.setConnectionInfo(connectionInfosWithDuplicateShardLabelValues);
        mgr.setProvider(NOT_EMPTY);
        mgr.setUuidPropertyName(NOT_EMPTY);
        Set<ConstraintViolation<DBConnectionInfoMgr>> violations = validator.validate(mgr);
        assertEquals(1, violations.size());
    }

    private List<DBConnectionInfo> getDBConnectionInfoList(String[] values) {
        List<DBConnectionInfo> connectionInfos = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            DBConnectionInfo ci = new DBConnectionInfo();
            ci.setShardId(values[i]);
            ci.setShardLabel(values[i]);
            ci.setUri(NOT_EMPTY);
            connectionInfos.add(ci);
        }
        return connectionInfos;
    }
}