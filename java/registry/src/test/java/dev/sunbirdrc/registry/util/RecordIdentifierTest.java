package dev.sunbirdrc.registry.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordIdentifierTest {

    @Test
    void testToString() {
        RecordIdentifier rid = new RecordIdentifier("shardId", "5701a670-644f-406e-902b-684b507bb89f");
        assertTrue(rid.toString().equalsIgnoreCase("shardId-5701a670-644f-406e-902b-684b507bb89f"));
    }

    @Test
    void testToStringWithNoShardId() {
        RecordIdentifier rid = new RecordIdentifier(null, "5701a670-644f-406e-902b-684b507bb89f");
        assertTrue(rid.toString().equalsIgnoreCase("5701a670-644f-406e-902b-684b507bb89f"));
    }

    @Test
    void testParse() {
        String label = "shardidentifier-5701a670-644f-406e-902b-684b507bb89f";
        RecordIdentifier resultRecordId = RecordIdentifier.parse(label);
        assertTrue(resultRecordId.getShardLabel().equalsIgnoreCase("shardidentifier"));
        assertTrue(resultRecordId.getUuid().equalsIgnoreCase("5701a670-644f-406e-902b-684b507bb89f"));
    }

    @Test
    void testParseForInvalidRecordId() {
        String label = "shardidentifier-0000x000-0000-00xx-000X-000x00xx";
        RecordIdentifier resultRecordId = RecordIdentifier.parse(label);
        assertNotNull(resultRecordId.getUuid());
    }

    @Test
    void testParseForNoShardId() {
        String label = "5701a670-644f-406e-902b-684b507bb89f";
        RecordIdentifier resultRecordId = RecordIdentifier.parse(label);
        assertTrue(resultRecordId.getUuid().equalsIgnoreCase("5701a670-644f-406e-902b-684b507bb89f"));
        assertNull(resultRecordId.getShardLabel());
    }

    @Test
    void shouldReturnOnlyUUIDIfInputHasShardLabel() {
        String uuidWithShardLabel = "1-5701a670-644f-406e-902b-684b507bb89f";
        String uuid = "5701a670-644f-406e-902b-684b507bb89f";
        assertEquals(RecordIdentifier.getUUID(uuidWithShardLabel), uuid);
    }

    @Test
    void shouldReturnJustUUIDIfShardLabelIsNotPresentInInput() {
        String uuidWithoutShardLabel = "5701a670-644f-406e-902b-684b507bb89f";
        String uuid = "5701a670-644f-406e-902b-684b507bb89f";
        assertEquals(RecordIdentifier.getUUID(uuidWithoutShardLabel), uuid);
    }
}