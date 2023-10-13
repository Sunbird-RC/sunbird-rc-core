package dev.sunbirdrc.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.util.regex.Pattern;

public class FunctionExecutorTest {
	@InjectMocks
	FunctionExecutor functionExecutor = new FunctionExecutor();

	ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void shouldCallFunctionWithActualValues() throws JsonProcessingException {

		JsonNode jsonNode = objectMapper.readValue("{\n" +
				"  \"id\": \"1\",\n" +
				"  \"name\": \"2\",\n" +
				"  \"output\": \"\"\n" +
				"}", JsonNode.class);
		String functionCallStr = "#/functionDefinitions/concat($.output, $.id, $.name)";
		FunctionDefinition functionDefinition = new FunctionDefinition();
		String functionFullStr = "arg1 = arg2 + \"-\" + arg3";
		functionDefinition.setResult(functionFullStr);
		JsonNode updatedNode = functionExecutor.execute(functionCallStr, functionDefinition, jsonNode);
		Assert.assertEquals("1-2", updatedNode.get("output").asText());
	}

	@Test
	public void shouldCallProviderFunctionWithActualValues() throws JsonProcessingException {

		JsonNode jsonNode = objectMapper.readValue("{\n" +
				"  \"id\": \"1\",\n" +
				"  \"name\": \"2\",\n" +
				"  \"output\": \"\"\n" +
				"}", JsonNode.class);
		String functionCallStr = "#/functionDefinitions/mathOperation";
		FunctionDefinition functionDefinition = new FunctionDefinition();
		functionDefinition.setProvider("dev.sunbirdrc.views.TestSampleProvider");
		JsonNode updatedNode = functionExecutor.execute(functionCallStr, functionDefinition, jsonNode);
		Assert.assertEquals("1-2", updatedNode.get("output").asText());
	}

	@Test
	public void shouldGenerateRandomUUIDNumber() throws JsonProcessingException {

		JsonNode jsonNode = objectMapper.readValue("{\n" +
				"  \"id\": \"1\",\n" +
				"  \"name\": \"2\",\n" +
				"  \"outputx\": \"\"\n" +
				"}", JsonNode.class);
		String functionCallStr = "#/functionDefinitions/mathOperation";
		FunctionDefinition functionDefinition = new FunctionDefinition();
		functionDefinition.setProvider("dev.sunbirdrc.provider.UUIDFunctionProvider");
		JsonNode updatedNode = functionExecutor.execute(functionCallStr, functionDefinition, jsonNode);
		System.out.println(updatedNode);
		Assert.assertTrue(isValidUUID(updatedNode.get("output").asText()));
	}

	private final static Pattern UUID_REGEX_PATTERN =
			Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

	boolean isValidUUID(String str) {
		if (str == null) {
			return false;
		}
		return UUID_REGEX_PATTERN.matcher(str).matches();
	}
}
