package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.AsyncRequest;
import dev.sunbirdrc.registry.model.dto.CreateEntityMessage;
import dev.sunbirdrc.registry.service.RegistryService;
import dev.sunbirdrc.registry.sink.shard.Shard;
import org.jetbrains.annotations.NotNull;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@Service
@Qualifier("async")
public class RegistryAsyncServiceImpl extends RegistryServiceImpl implements RegistryService {
	private static final Logger logger = LoggerFactory.getLogger(RegistryAsyncServiceImpl.class);
	@Value("${kafka.createEntityTopic:create_entity}")
	String createEntityTopic;
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AsyncRequest asyncRequest;

	@Override
	public String addEntity(Shard shard, String userId, JsonNode inputJson, boolean skipSignature) throws Exception {
		KeycloakAuthenticationToken keycloakAuthenticationToken = (KeycloakAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		CreateEntityMessage createEntityMessage = CreateEntityMessage.builder().userId(userId).inputJson(inputJson)
				.skipSignature(skipSignature).webhookUrl(asyncRequest.getWebhookUrl())
				.emailId(keycloakAuthenticationToken.getAccount().getKeycloakSecurityContext().getToken().getEmail())
				.build();
		String message = objectMapper.writeValueAsString(createEntityMessage);
		String transactionId = UUID.randomUUID().toString();
		ListenableFuture<SendResult<String, String>> future =
				kafkaTemplate.send(createEntityTopic, transactionId, message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

			@Override
			public void onSuccess(SendResult<String, String> result) {
				logger.debug("Sent message=[{}] with offset=[{}]", message, result.getRecordMetadata().offset());
			}

			@Override
			public void onFailure(@NotNull Throwable ex) {
				logger.error("Unable to send message=[{}] with offset=[{}] due to : {}", message, ex.getMessage(), ex);
			}
		});
		return transactionId;
	}


}
