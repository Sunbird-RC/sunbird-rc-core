package io.opensaber.registry.middleware.utils;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opensaber.registry.middleware.util.Constants.JsonldConstants;
import io.opensaber.registry.middleware.util.JSONUtil;

public class JSONUtilsTest {

	private static final String ACTUAL_DATA = "actual_data.json";
	private static final String EXPECTED_DATA = "expected_data.json";
	private static final String EXPECTED_REPLACE_FIELD_DATA = "expected_replace_field_data.json";
	private static final String EXPECTED_ADDED_NODE_DATA = "expected_added_node_data.json";
	private static final String EXPECTED_ADDED_PREFIX_DATA = "expected_add_prefix_data.json";

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testRemoveNode() throws IOException {
		ObjectNode actualNode = (ObjectNode) mapper.readTree(getContent(ACTUAL_DATA));
		ObjectNode expectedNode = (ObjectNode) mapper.readTree(getContent(EXPECTED_DATA));

		JSONUtil.removeNode(actualNode, JsonldConstants.ID);
		assertEquals(actualNode, expectedNode);
	}

	@Test
	public void testReplaceField() throws IOException {
		ObjectNode actualNode = (ObjectNode) mapper.readTree(getContent(ACTUAL_DATA));
		ObjectNode expectedNode = (ObjectNode) mapper.readTree(getContent(EXPECTED_REPLACE_FIELD_DATA));

		JSONUtil.replaceField(actualNode, JsonldConstants.ID, "newValue");
		assertEquals(actualNode, expectedNode);
	}

	@Test
	public void testAddNode() throws IOException {
		ObjectNode actualNode = (ObjectNode) mapper.readTree(getContent(ACTUAL_DATA));
		ObjectNode expectedNode = (ObjectNode) mapper.readTree(getContent(EXPECTED_ADDED_NODE_DATA));

		ObjectNode nodeToAdd = JsonNodeFactory.instance.objectNode();
		nodeToAdd.put("newNodeKey", "newNodeValue");

		JSONUtil.addNode(actualNode, "addedKey", nodeToAdd);
		assertEquals(actualNode, expectedNode);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNodeException() throws IOException {
		ObjectNode actualNode = (ObjectNode) mapper.readTree(getContent(ACTUAL_DATA));
		JSONUtil.addNode(actualNode, "", JsonNodeFactory.instance.objectNode());
	}

	@Test
	public void testAddPrefix() throws IOException {
		ObjectNode actualNode = (ObjectNode) mapper.readTree(getContent(ACTUAL_DATA));
		ObjectNode expectedNode = (ObjectNode) mapper.readTree(getContent(EXPECTED_ADDED_PREFIX_DATA));
		List<String> keys = new ArrayList<String>();
		keys.add(JsonldConstants.ID);

		JSONUtil.addPrefix(actualNode, "prefix-", keys);
		assertEquals(actualNode, expectedNode);
	}

	private String getContent(String fileName) {
		InputStream in;
		try {
			in = this.getClass().getClassLoader().getResourceAsStream(fileName);
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			return result.toString(StandardCharsets.UTF_8.name());

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();

		}
		return null;
	}

}
