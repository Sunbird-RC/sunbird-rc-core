package org.egov.id.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class IdFormatResponse {

    private ResponseInfo responseInfo;

    private List<String> errorMsgs;

}
