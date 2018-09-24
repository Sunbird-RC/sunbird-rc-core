package io.opensaber.registry.util;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.ext.com.google.common.io.ByteStreams;

public class JSONUtil {
    private static Type type = new TypeToken<Map<String, Object>>() {
    }.getType();

    public static Map<String, Object> convertObjectJsonMap(Object object) {
        Gson gson = new Gson();
        String result = gson.toJson(object);
        return gson.fromJson(result, type);
    }
    
    public static String getStringWithReplacedText(String payload, String value, String replacement){
		//String value = "\"@id\"\\s*:\\s*\"[a-z]+:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",";
		Pattern pattern = Pattern.compile(value);
		Matcher matcher = pattern.matcher(payload);
		return matcher.replaceAll(replacement);
	}
    
    public static Map<String,Object> frameJsonAndRemoveIds(String regex, String json, Gson gson, String frame) throws JsonLdError, IOException{
    	Map<String, Object> reqMap = gson.fromJson(json, type);
    	JsonObject jsonObj = gson.fromJson(json, JsonObject.class);
    	String rootType = null;
    	if(jsonObj.get("@graph")!=null){
    		rootType = jsonObj.get("@graph").getAsJsonArray().get(0).getAsJsonObject().get("@type").getAsString();
    	}else{
    		rootType = jsonObj.get("@type").getAsString();
    	}
    	frame = frame.replace("<@type>", rootType);
    	//JsonUtils.fromString(frame)
    	JsonLdOptions options = new JsonLdOptions();
    	options.setCompactArrays(true);
    	Map<String,Object> framedJsonLD = JsonLdProcessor.frame(reqMap, JsonUtils.fromString(frame), options);
    	json = gson.toJson(framedJsonLD);
    	json = JSONUtil.getStringWithReplacedText(json, regex, StringUtils.EMPTY);
    	System.out.println(json);
    	return gson.fromJson(json, type);
    }

}
