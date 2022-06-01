package dev.sunbirdrc.registry.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.model.dto.CreateEntityMessage;
import dev.sunbirdrc.registry.model.dto.CreateEntityStatus;
import dev.sunbirdrc.registry.model.dto.PostCreateEntityMessage;
import dev.sunbirdrc.registry.model.dto.WebhookEvent;
import dev.sunbirdrc.registry.service.RegistryService;
import dev.sunbirdrc.registry.service.WebhookService;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

import static dev.sunbirdrc.registry.Constants.SUNBIRD_RC;
import static dev.sunbirdrc.registry.Constants.createEntityGroupId;

@Component
public class CreateEntityConsumer {
    private static final Logger logger = LoggerFactory.getLogger(CreateEntityConsumer.class);
    private final ObjectMapper objectMapper;
    private final ShardManager shardManager;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RegistryService registryService;

    private final WebhookService webhookService;
    @Value("${kafka.postCreateEntityTopic:post_create_entity}")
    String postCreateEntityTopic;

    @Autowired
    public CreateEntityConsumer(ObjectMapper objectMapper, ShardManager shardManager, KafkaTemplate<String, String> kafkaTemplate, @Qualifier("sync") RegistryService registryService, WebhookService webhookService) {
        this.objectMapper = objectMapper;
        this.shardManager = shardManager;
        this.kafkaTemplate = kafkaTemplate;
        this.registryService = registryService;
        this.webhookService = webhookService;
    }

    @KafkaListener(topics = "#{'${kafka.createEntityTopic}'}", groupId = createEntityGroupId)
    @SendTo("#{'${kafka.postCreateEntityTopic}'}")
    public void createEntityConsumer(@Payload String message, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
        PostCreateEntityMessage postCreateEntityMessage = PostCreateEntityMessage.builder().build();
        try {
            logger.debug("Received message: {}, key: {}", message, key);
            CreateEntityMessage createEntityMessage = objectMapper.readValue(message, CreateEntityMessage.class);
            JsonNode inputJson = createEntityMessage.getInputJson();
            String entityType = inputJson.fields().next().getKey();
            Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
            String entityOsid = registryService.addEntity(shard, createEntityMessage.getUserId(), inputJson, createEntityMessage.isSkipSignature());
            postCreateEntityMessage = PostCreateEntityMessage.builder().entityType(entityType).osid(entityOsid).transactionId(key).userId(createEntityMessage.getUserId()).status(CreateEntityStatus.SUCCESSFUL).message("").build();

        } catch (Exception e) {
            logger.error("Creating entity failed, {}", e.getMessage(), e);
            postCreateEntityMessage = PostCreateEntityMessage.builder().status(CreateEntityStatus.FAILED).message(e.getMessage()).build();
        } finally {
            try {
                kafkaTemplate.send(postCreateEntityTopic, key, objectMapper.writeValueAsString(postCreateEntityMessage));
                webhookService.postEvent(WebhookEvent.builder().event(String.format("%s-create", SUNBIRD_RC))
                        .data(postCreateEntityMessage)
                        .timestamp(Timestamp.from(Instant.now())).build());
            } catch (JsonProcessingException e) {
                logger.error("Sending message to {} topic failed: {}", postCreateEntityTopic, e.getMessage(), e);
            }
        }
    }
}
