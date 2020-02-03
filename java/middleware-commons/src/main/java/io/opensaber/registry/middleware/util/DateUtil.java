package io.opensaber.registry.middleware.util;

import java.sql.Timestamp;
import java.time.Instant;

public class DateUtil {

    public static String getTimeStamp() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }
    public static long getTimeStampLong() {
        return System.currentTimeMillis();
    }
    
    public static String instantTimeStamp() {
        return Instant.now().toString();
    }
}
