package dev.sunbirdrc.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FunctionExecutorTest {

    FunctionExecutor functionExecutor;
    ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        functionExecutor = new FunctionExecutor();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void shouldCallFunctionWithActualValues() throws JsonProcessingException {
        String jsonString = """
               {
                    "id": "1",
                    "name": "2",
                    "output": ""
                  }
               """;
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        String functionCallStr = "#/functionDefinitions/concat($.output, $.id, $.name)";
        FunctionDefinition functionDefinition = new FunctionDefinition();
        String functionFullStr = "arg1 = arg2 + \"-\" + arg3";
        functionDefinition.setResult(functionFullStr);
        JsonNode updatedNode = functionExecutor.execute(functionCallStr, functionDefinition, jsonNode);
        assertEquals("1-2", updatedNode.get("output").asText());
    }

    @Test
    public void shouldCallProviderFunctionWithActualValues() throws JsonProcessingException {
        String jsonString = """
               {
                    "id": "1",
                    "name": "2",
                    "output": ""
                  }
               """;
        JsonNode jsonNode = objectMapper.readValue(jsonString, JsonNode.class);
        String functionCallStr = "#/functionDefinitions/mathOperation";
        FunctionDefinition functionDefinition = new FunctionDefinition();
        functionDefinition.setProvider("dev.sunbirdrc.views.TestSampleProvider");
        JsonNode updatedNode = functionExecutor.execute(functionCallStr, functionDefinition, jsonNode);
        assertEquals("1-2", updatedNode.get("output").asText());
    }

    @Test
    public void shouldGenerateRandomUUIDNumber() throws JsonProcessingException {
        String jsonString = """
               {
                    "id": "1",
                    "name": "2",
                    "output": ""
                  }
               """;
        JsonNode jsonNode = objectMapper.readValue(jsonString, JsonNode.class);
        String functionCallStr = "#/functionDefinitions/mathOperation";
        FunctionDefinition functionDefinition = new FunctionDefinition();
        functionDefinition.setProvider("dev.sunbirdrc.provider.UUIDFunctionProvider");
        JsonNode updatedNode = functionExecutor.execute(functionCallStr, functionDefinition, jsonNode);
        System.out.println(updatedNode);
        assertTrue(isValidUUID(updatedNode.get("output").asText()));
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