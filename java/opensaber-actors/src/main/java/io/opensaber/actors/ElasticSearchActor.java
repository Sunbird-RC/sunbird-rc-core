package io.opensaber.actors;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.elastic.ESMessage;
import io.opensaber.elastic.ElasticServiceImpl;
import io.opensaber.elastic.IElasticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

public class ElasticSearchActor extends BaseActor {
    public IElasticService elasticSearch;
    public ObjectMapper objectMapper;

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to ElasticSearch Actor {}", request.getPerformOperation());
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        elasticSearch = new ElasticServiceImpl();
        objectMapper = new ObjectMapper();
        ESMessage esMessage = objectMapper.readValue(request.getPayload().getStringValue(), ESMessage.class);
        //ESMessage es =  objectMapper.writeValue(request.getPayload(), ESMessage.class);
        switch (request.getPerformOperation()) {
            case "add":
                elasticSearch.addEntity(esMessage.getIndexName(), esMessage.getOsid(), esMessage.getInput());
                break;
            case "update":
                elasticSearch.updateEntity(esMessage.getIndexName(), esMessage.getOsid(), esMessage.getInput());
                break;
            case "delete":
                elasticSearch.deleteEntity(esMessage.getIndexName(), esMessage.getOsid());
                break;
            case "read":
                break;
        }
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
