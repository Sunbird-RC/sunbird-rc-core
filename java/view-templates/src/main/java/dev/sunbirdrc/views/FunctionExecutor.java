package dev.sunbirdrc.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionExecutor {

	private static final String FUNCTION_ARGS_REGEX = "\\(\\s*([^)]+?)\\s*\\)";
	private final ObjectMapper objectMapper = new ObjectMapper();

	public JsonNode execute(String functionCallStr, FunctionDefinition functionDefinition, JsonNode jsonNode) throws JsonProcessingException {
		DocumentContext documentContext = JsonPath.parse(jsonNode.toString());
		List<Object> arguments = extractArguments(functionCallStr, documentContext);
		IEvaluator<Object> instance = EvaluatorFactory.getInstance(functionDefinition, arguments, getArgumentsPath(functionCallStr));
		Object evaluatedValue = instance.evaluate();
		documentContext = assignEvaluatedValue(evaluatedValue, functionDefinition, documentContext, getArgumentsPath(functionCallStr));
		return objectMapper.readTree(documentContext.jsonString());
	}

	private DocumentContext assignEvaluatedValue(Object evaluatedValue, FunctionDefinition functionDefinition,
	                                             DocumentContext documentContext, String[] argumentsPath) {
		if (!StringUtils.isEmpty(functionDefinition.getResult())) {
			String result = functionDefinition.getResult();
			if (result.indexOf("=") > 0) {
				String assignmentStr = result.split("=")[0];
				int assignmentIndex = Integer.parseInt(assignmentStr
						.replace("arg", "").
						replace(" ", "")
						.trim()) - 1;
				String assignmentPath = argumentsPath[assignmentIndex];
				documentContext.set(assignmentPath, evaluatedValue);
			}
		} else {
			documentContext = JsonPath.parse(evaluatedValue.toString());
		}
		return documentContext;
	}

	private List<Object> extractArguments(String functionCallStr, DocumentContext documentContext) {
		String[] argumentsPath = getArgumentsPath(functionCallStr);
		List<Object> arguments = new ArrayList<>();
		for (String path : argumentsPath) {
			arguments.add(objectMapper.convertValue(documentContext.read(path), JsonNode.class).asText());
		}
		arguments.add(documentContext.read("$"));
		return arguments;
	}

	private String[] getArgumentsPath(String functionCallStr) {
		Pattern pattern = Pattern.compile(FUNCTION_ARGS_REGEX);
		Matcher matcher = pattern.matcher(functionCallStr);
		if (matcher.find()) {
			return matcher.group(0)
					.replace("(", "")
					.replace(")", "")
					.split(",");
		}
		return new String[]{};
	}
}
