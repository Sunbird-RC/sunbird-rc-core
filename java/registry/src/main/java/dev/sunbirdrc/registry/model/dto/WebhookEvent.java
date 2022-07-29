package dev.sunbirdrc.registry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookEvent {
	private String event;
	private Timestamp timestamp;
	private Object data;
	private String webhookUrl;
}
