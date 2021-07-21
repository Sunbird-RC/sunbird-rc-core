package io.opensaber.registry.util;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class OwnershipsAttributes {
    String email;
    String mobile;
    String userId;

    public boolean isEmpty() {
        return StringUtils.isEmpty(email) || StringUtils.isEmpty(mobile) || StringUtils.isEmpty(userId);
    }
}
