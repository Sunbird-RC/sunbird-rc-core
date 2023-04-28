package dev.sunbirdrc.actors;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.elastic.ESMessage;
import dev.sunbirdrc.elastic.ElasticServiceImpl;
import dev.sunbirdrc.elastic.IElasticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;




public class ElasticSearchActor extends BaseActor {
    public IElasticService elasticSearch;
    public ObjectMapper objectMapper;


    //@Value("${registry.HARD_DELETE_ENABLED}")
    private boolean isHardDeleteEnabled;

    @Value("${search.providerName}")
    private String searchProvider;


    @Autowired
    //public RegistryService registryService1;

 /*   private boolean isElasticSearchEnabled() {
        return (searchProvider.equals("dev.sunbirdrc.registry.service.ElasticSearchService"));
    }*/

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to ElasticSearch Actor {}", request.getPerformOperation());
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        elasticSearch = new ElasticServiceImpl();
        objectMapper = new ObjectMapper();

        ESMessage esMessage = objectMapper.readValue(request.getPayload().getStringValue(), ESMessage.class);
        //ESMessage es =  objectMapper.writeValue(request.getPayload(), ESMessage.class);
        switch (request.getPerformOperation()) {
            case "ADD":
                elasticSearch.addEntity(esMessage.getIndexName(), esMessage.getOsid(), esMessage.getInput());
                break;
            case "UPDATE":
                elasticSearch.updateEntity(esMessage.getIndexName(), esMessage.getOsid(), esMessage.getInput());
                break;
            case "DELETE":
                elasticSearch.deleteEntity(esMessage.getIndexName(), esMessage.getOsid());
                    break;
        case "READ":
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

