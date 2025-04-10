package dev.sunbirdrc.registry.middleware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DateUtilTest {

    @Test
    void getTimeStamp() {
        String date = DateUtil.getTimeStamp();
        assertNotNull(date);
    }

}