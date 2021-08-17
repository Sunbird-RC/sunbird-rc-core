package io.opensaber.registry.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class RecordIdentifierTest {

	@Test
	public void testToString() {
		RecordIdentifier rid = new RecordIdentifier("shardId", "5701a670-644f-406e-902b-684b507bb89f");
		assertTrue(rid.toString().equalsIgnoreCase("shardId-5701a670-644f-406e-902b-684b507bb89f"));
	}

	@Test
	public void testToStringWithNoShardId() {
		RecordIdentifier rid = new RecordIdentifier(null, "5701a670-644f-406e-902b-684b507bb89f");
		assertTrue(rid.toString().equalsIgnoreCase("5701a670-644f-406e-902b-684b507bb89f"));
	}

	@Test
	public void testParse() {
		String label = "shardidentifier-5701a670-644f-406e-902b-684b507bb89f";
		RecordIdentifier resultRecordId = RecordIdentifier.parse(label);
		assertTrue(resultRecordId.getShardLabel().equalsIgnoreCase("shardidentifier"));
		assertTrue(resultRecordId.getUuid().equalsIgnoreCase("5701a670-644f-406e-902b-684b507bb89f"));
	}

	@Test
	public void testParseForInvalidRecordId() {
		String label = "shardidentifier-0000x000-0000-00xx-000X-000x00xx";
		RecordIdentifier resultRecordId = RecordIdentifier.parse(label);
		assertNotNull(resultRecordId.getUuid());
	}

	@Test
	public void testParseForNoShardId() {
		String label = "5701a670-644f-406e-902b-684b507bb89f";
		RecordIdentifier resultRecordId = RecordIdentifier.parse(label);
		assertTrue(resultRecordId.getUuid().equalsIgnoreCase("5701a670-644f-406e-902b-684b507bb89f"));
		assertNull(resultRecordId.getShardLabel());
	}

	@Test
	public void shouldReturnOnlyUUIDIfInputHasShardLabel() {
		String uuidWithShardLabel = "1-5701a670-644f-406e-902b-684b507bb89f";
		String uuid= "5701a670-644f-406e-902b-684b507bb89f";
		assertEquals(RecordIdentifier.getUUID(uuidWithShardLabel), uuid);
	}

	@Test
	public void shouldReturnJustUUIDIfShardLabelIsNotPresentInInput() {
		String uuidWithoutShardLabel = "5701a670-644f-406e-902b-684b507bb89f";
		String uuid= "5701a670-644f-406e-902b-684b507bb89f";
		assertEquals(RecordIdentifier.getUUID(uuidWithoutShardLabel), uuid);
	}

}
