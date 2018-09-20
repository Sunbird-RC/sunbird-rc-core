package io.opensaber.registry.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class JSONUtil {
    private static Type type = new TypeToken<Map<String, Object>>(){}.getType();

    public static Map<String, Object> convertObjectJsonMap(Object object) {
        Gson gson = new Gson();
        String result = gson.toJson(object);
        return gson.fromJson(result, type);
    }
    
    public static String getModifiedClaim(String payload){
		String value = "\"@id\":\"[a-z]+:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",";
		Pattern pattern = Pattern.compile(value);
		Matcher matcher = pattern.matcher(payload);
		return matcher.replaceAll(StringUtils.EMPTY);
	}

}
