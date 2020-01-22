package io.opensaber.registry.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.registry.exception.audit.AuditException;
import io.opensaber.registry.exception.audit.EntityTypeMissingException;
import io.opensaber.registry.exception.audit.InvalidArguementException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ObjectMapper.class, AuditHelper.class})
public class AuditHelperTest {

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private AuditHelper auditHelper;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Test
	public void getSearchQueryNodeForAuditTest() throws IOException, AuditException {
		String inputJson = "{ \"entityType\": \"Teacher\", \"id\":\"09d9c84a-0696-400f-8e5a-65fb30333ce5\", \"action\":\"ADD\", \"startDate\":1578393274000, \"limit\": 3, \"offset\": 0 }";

		JsonNode jsonNode = null;
		jsonNode = objectMapper.readTree(inputJson);
		JsonNode result = auditHelper.getSearchQueryNodeForAudit(jsonNode, "osid");
		assertEquals("ADD", result.get("filters").get("action").get("eq").asText());
	}
	
	@Test
	public void getAuditLogEntitytypeMissing() throws Exception {
		String inputJson = "{ \"id\":\"09d9c84a-0696-400f-8e5a-65fb30333ce5\", \"action\":\"ADD\", \"startDate\":1578393274000, \"limit\": 3, \"offset\": 0 }";
		JsonNode jsonNode = objectMapper.readTree(inputJson);
		exception.expect(EntityTypeMissingException.class);
		exception.expectMessage("entityType cannot be null");
		auditHelper.getSearchQueryNodeForAudit(jsonNode,"tid");

	}

	

	@Test
	public void getAuditLogEntitytypeEmpty() throws Exception {
		String inputJson = "{\"entityType\": \"\", \"id\":\"09d9c84a-0696-400f-8e5a-65fb30333ce5\", \"action\":\"ADD\", \"startDate\":1578393274000, \"limit\": 3, \"offset\": 0 }";
		JsonNode jsonNode = objectMapper.readTree(inputJson);
		exception.expect(InvalidArguementException.class);
		exception.expectMessage("entityType should not be an empty");
		auditHelper.getSearchQueryNodeForAudit(jsonNode,"tid");

	}

}
