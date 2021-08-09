package io.opensaber.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipsAttributes {
    String email;
    String mobile;
    String userId;

    public boolean isValid() {
        if (StringUtils.isEmpty(userId)) {
            return true;
        }
        return StringUtils.isEmpty(email) && StringUtils.isEmpty(mobile);
    }
}
