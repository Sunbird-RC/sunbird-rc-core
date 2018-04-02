package io.opensaber.registry.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class JSONUtil {

    public static Map<String, Object> convertObjectJsonMap(Object object) {
        Gson gson = new Gson();
        String result = gson.toJson(object);
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(result, type);
    }

}
