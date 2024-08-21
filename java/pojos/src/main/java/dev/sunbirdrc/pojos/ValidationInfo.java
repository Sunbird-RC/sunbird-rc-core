package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ValidationInfo {

    private String node;
    private String shape;
    private String status;
    private String appInfo;
    private String reason;

}
