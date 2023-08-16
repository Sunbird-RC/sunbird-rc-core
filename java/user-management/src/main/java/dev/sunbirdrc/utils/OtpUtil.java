package dev.sunbirdrc.utils;

import dev.sunbirdrc.config.PropertiesValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class OtpUtil {

    @Autowired
    private PropertiesValueMapper propMapping;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * @param userId
     * @return
     */
    public Optional<Integer> generateAndPersistOTP(String userId, TimeUnit timeUnit) throws Exception {
        Integer otp = null;

        if (StringUtils.hasText(userId)) {
             otp = ThreadLocalRandom.current().nextInt(100000, 999999);
            redisUtil.putValueWithExpireTime(userId, String.valueOf(otp), propMapping.getOtpTtlDuration(), timeUnit);
        } else {
            throw new Exception("Invalid user id to generate OTP");
        }

        return Optional.ofNullable(otp);
    }

    /**
     * @param userId
     * @param otp
     * @return
     */
    public boolean verifyUserMailOtp(String userId, String otp) {
        if (!StringUtils.isEmpty(userId) && otp != null && !otp.equals(0)) {
            String cachedOtp = redisUtil.getValue(userId);

            return otp.equals(cachedOtp);
        }

        return false;
    }


    /**
     * @return
     * @throws Exception
     */
    public TimeUnit getOtpTimeUnit() throws Exception {
        TimeUnit timeUnit = null;

        switch (propMapping.getOtpTimeUnit()) {
            case "SECOND":
                timeUnit = TimeUnit.SECONDS;
                break;
            case "MINUTE":
                timeUnit = TimeUnit.MINUTES;
                break;
            case "HOUR":
                timeUnit = TimeUnit.HOURS;
                break;
            case "DAY":
                timeUnit = TimeUnit.DAYS;
                break;
            default:
                throw new Exception("Invalid time unit value");
        }

        return timeUnit;
    }
}
