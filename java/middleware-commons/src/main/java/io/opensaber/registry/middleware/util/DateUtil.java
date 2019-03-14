package io.opensaber.registry.middleware.util;

import java.sql.Timestamp;

public class DateUtil {

    public static String getTimeStamp() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }
}
