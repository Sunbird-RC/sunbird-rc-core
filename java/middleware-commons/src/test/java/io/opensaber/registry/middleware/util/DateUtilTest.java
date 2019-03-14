package io.opensaber.registry.middleware.util;

import org.junit.Assert;
import org.junit.Test;

public class DateUtilTest {

    @Test
    public void getTimeStamp() {
        String date = DateUtil.getTimeStamp();
        Assert.assertNotNull(date);
    }

}