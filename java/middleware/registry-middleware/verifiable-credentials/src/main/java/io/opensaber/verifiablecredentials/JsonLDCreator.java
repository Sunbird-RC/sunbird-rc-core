package io.opensaber.verifiablecredentials;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonLDCreator {
    private final String data;
    private final String issuer;

    public JsonLDCreator(String data, String issuer) {
        this.data = data;
        this.issuer = issuer;
    }

    public String getValue() {
        final String schema = "https://schema.org/";
        String date = getToday();

        ObjectNode contextNode = JsonNodeFactory.instance.objectNode();
        contextNode.put("schema", schema);
        contextNode.put("data", "schema:name");
        contextNode.put("issuer", "schema:url");
        contextNode.put("date", "schema:date");

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("@context", contextNode);
        result.put("data", data);
        result.put("issuer", issuer);
        result.put("date", date);
        return result.toString();
    }

    public String getToday() {
        String pattern = "dd-MM-yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }
}
