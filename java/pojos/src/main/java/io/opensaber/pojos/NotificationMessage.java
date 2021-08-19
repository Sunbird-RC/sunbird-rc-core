package io.opensaber.pojos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@JsonSerialize
@Data
@RequiredArgsConstructor
public class NotificationMessage {
    String to;
    String message;
    String subject;
}
