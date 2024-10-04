package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ComponentHealthInfo {

    private String name;
    private boolean healthy;
    private String err;
    private String errmsg;

    public ComponentHealthInfo(String name, boolean healthy) {
        this.name = name;
        this.healthy = healthy;
        this.err = "";
        this.errmsg = "";
    }

}
