package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class RequestParams {

    private String did;
    private String key;
    private String msgid;

    public RequestParams() {
        this.msgid = UUID.randomUUID().toString();
        this.did = "";
        this.key = "";
    }


}
