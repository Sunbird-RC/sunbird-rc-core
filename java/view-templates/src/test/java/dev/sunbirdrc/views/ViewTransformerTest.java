package dev.sunbirdrc.views;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewTransformerTest {

    private static final Logger logger = LoggerFactory.getLogger(ViewTransformerTest.class);

    private ViewTransformer transformer = new ViewTransformer();

    @Test
    public void testTransformForPersonFunction() throws Exception{

        ObjectNode personNode = getPerson();
        ViewTemplate viewTemplate = getViewTemplatePerson("person_vt.json");

        JsonNode actualnode = transformer.transform(viewTemplate, personNode);
        JsonNode expectedNode = new ObjectMapper().readTree("{\"Person\":{\"NAME\":\"Ram\",\"lastName\":\"Moorthy\",\"Name in passport\":\"Moorthy, Ram\",\"Name as in DL\":\"Ram : Moorthy\"}}");

        assertEquals(expectedNode, actualnode);

    }

    @Test
    public void testTransformForMathVT() throws Exception{
        String mathProblem = "{\"Math\": " +
                "               {\"a\": 5," +
                "                \"b\": 2 }}";
        ObjectNode node = (ObjectNode) new ObjectMapper().readTree(mathProblem);

        ViewTemplate viewTemplate = getViewTemplatePerson("mathVT1.json");
        JsonNode actualnode = transformer.transform(viewTemplate, node);
        JsonNode expectedNode = new ObjectMapper().readTree("{\"Math\":{\"addend_A\":5,\"addend_B\":2,\"SUM\":7}}");
        assertEquals(expectedNode.toString(), actualnode.toString());

    }


    private ViewTemplate getViewTemplatePerson(String personJsonFileName) throws JsonProcessingException, IOException{

        String viewTemplateJson = readFileContent(personJsonFileName);
        return new ObjectMapper().readValue(viewTemplateJson, ViewTemplate.class);
    }

    private ObjectNode getPerson() throws JsonProcessingException, IOException{
        String personJson = "{\"Person\": " +
                "               {\"nationalIdentifier\":\"nid823\"," +
                "                \"firstName\":\"Ram\"," +
                "                \"lastName\":\"Moorthy\"," +
                "                \"gender\":\"MALE\"," +
                "                \"dob\":\"1990-12-10\"}}";
        return (ObjectNode) new ObjectMapper().readTree(personJson);
    }

    private static String readFileContent(String fileName) {
        InputStream in;
        try {
            in = ViewTransformer.class.getClassLoader().getResourceAsStream(fileName);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());

        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    @Test
    public void shouldRemovePathUsingRemovePathProvider() throws Exception {
        String personJson = "{\n" +
		        "  \"Person\": " +
		        "{\n" +
		        "    \"nationalIdentifier\": \"nid823\"," +
		        "\n" +
                "    \"name\": \"Ram\",\n" +
                "    \"lastName\": \"Moorthy\"," +
		        "\n" +
		        "    \"gender\": \"MALE\"," +
		        "\n" +
                "    \"dob\": \"1990-12-10\",\n" +
                "    \"address\": {\n" +
                "      \"line\": \"1st stree\",\n" +
                "      \"city\": \"bangalore\"\n" +
                "    }\n" +
                "  }\n" +
		        "}";
        ObjectNode personNode =  (ObjectNode) new ObjectMapper().readTree(personJson);
        ViewTemplate viewTemplate = getViewTemplatePerson("90986382-4745-11ea-b77f-2e728ce88124.json");

        JsonNode actualnode = transformer.transform(viewTemplate, personNode);
        JsonNode expectedNode = new ObjectMapper().readTree("{\"Person\":{\"name\":\"Ram\",\"address\":{\"city\":\"bangalore\"}}}");

        assertEquals(expectedNode.toPrettyString(), actualnode.toPrettyString());
    }
}
