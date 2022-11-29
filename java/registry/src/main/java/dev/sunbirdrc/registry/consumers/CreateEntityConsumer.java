package dev.sunbirdrc.registry.consumers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.Constants;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.model.dto.CreateEntityMessage;
import dev.sunbirdrc.registry.model.dto.CreateEntityStatus;
import dev.sunbirdrc.registry.model.dto.PostCreateEntityMessage;
import dev.sunbirdrc.registry.model.dto.WebhookEvent;
import dev.sunbirdrc.registry.service.RegistryService;
import dev.sunbirdrc.registry.service.WebhookService;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import org.apache.commons.lang3.StringUtils;
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
    @Autowired
    private final RegistryHelper registryHelper;

    private final WebhookService webhookService;
    @Value("${kafka.postCreateEntityTopic:post_create_entity}")
    String postCreateEntityTopic;

    @Value("${webhook.url}")
    private String webhookUrl;

    @Autowired
    public CreateEntityConsumer(ObjectMapper objectMapper, ShardManager shardManager, KafkaTemplate<String, String> kafkaTemplate,
                                @Qualifier("sync") RegistryService registryService, RegistryHelper registryHelper, WebhookService webhookService) {
        this.objectMapper = objectMapper;
        this.shardManager = shardManager;
        this.kafkaTemplate = kafkaTemplate;
        this.registryService = registryService;
        this.registryHelper = registryHelper;
        this.webhookService = webhookService;
    }

    @KafkaListener(topics = "#{'${kafka.createEntityTopic}'}", groupId = createEntityGroupId, autoStartup = "${async.enabled}")
    public void createEntityConsumer(@Payload String message, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
        PostCreateEntityMessage postCreateEntityMessage = PostCreateEntityMessage.builder().build();
        try {
            logger.debug("Received message: {}, key: {}", message, key);
            CreateEntityMessage createEntityMessage = objectMapper.readValue(message, CreateEntityMessage.class);
            if (!StringUtils.isEmpty(createEntityMessage.getWebhookUrl())) {
                webhookUrl = createEntityMessage.getWebhookUrl();
            }
            JsonNode inputJson = createEntityMessage.getInputJson();
            String entityType = inputJson.fields().next().getKey();
            Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
            String entityOsid = registryService.addEntity(shard, createEntityMessage.getUserId(), inputJson, createEntityMessage.isSkipSignature());
            registryHelper.autoRaiseClaim(entityType, entityOsid, createEntityMessage.getUserId(), null, inputJson, createEntityMessage.getEmailId());
            postCreateEntityMessage = PostCreateEntityMessage.builder().entityType(entityType).osid(entityOsid)
                    .transactionId(key).userId(createEntityMessage.getUserId()).status(CreateEntityStatus.SUCCESSFUL).message("").build();

        } catch (Exception e) {
            logger.error("Creating entity failed, {}", e.getMessage(), e);
            postCreateEntityMessage = PostCreateEntityMessage.builder().status(CreateEntityStatus.FAILED).transactionId(key).message(e.getMessage()).build();
        } finally {
            try {
                kafkaTemplate.send(postCreateEntityTopic, key, objectMapper.writeValueAsString(postCreateEntityMessage));
                webhookService.postEvent(WebhookEvent.builder().event(String.format("%s-create", SUNBIRD_RC))
                        .data(postCreateEntityMessage)
                        .webhookUrl(webhookUrl)
                        .timestamp(Timestamp.from(Instant.now())).build());
            } catch (Exception e) {
                logger.error("Sending message to {} topic failed: {}", postCreateEntityTopic, e.getMessage(), e);
            }
        }
    }
}
