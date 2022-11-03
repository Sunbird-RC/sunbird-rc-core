package dev.sunbirdrc.actors;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.actors.services.NotificationService;
import dev.sunbirdrc.pojos.NotificationMessage;
import okhttp3.Response;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

public class NotificationActor extends BaseActor {
    public ObjectMapper objectMapper;
    private NotificationService notificationService;

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to Notification Actor {}", request.getPerformOperation());
        objectMapper = new ObjectMapper();
        notificationService = new NotificationService();
        NotificationMessage notificationMessage = objectMapper.readValue(request.getPayload().getStringValue(), NotificationMessage.class);
        Response response = notificationService.notify(notificationMessage);
        logger.info("{}", response.body());
    }

    @Override
    public void onFailure(MessageProtos.Message message) {
        logger.info("Send hello failed {}", message.toString());
    }

    @Override
    public void onSuccess(MessageProtos.Message message) {
        logger.info("Send hello answered successfully {}", message.toString());
    }

}
