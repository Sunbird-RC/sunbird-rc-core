package dev.sunbirdrc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    @Qualifier("customRedis")
    private RedisTemplate<String, String> customRedis;

    public void putValue(String key, String value) {
        customRedis.opsForValue().set(key, value);
    }

    public void putValueWithExpireTime(String key, String value, long timeout, TimeUnit unit) {
        RedisClientInfo redisClientInfo = customRedis.getClientList().get(0);

        logger.info(">>>>>>>>>>>>>>> redis client info: " + redisClientInfo);
        logger.info(">>>>>>>>>>>>>>>>> address: " + redisClientInfo.getAddressPort() );

        customRedis.opsForValue().set(key, value, timeout, unit);
    }

    public String getValue(String key) {
        return customRedis.opsForValue().get(key);
    }
}
