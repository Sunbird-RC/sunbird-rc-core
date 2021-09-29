package io.opensaber.registry.controller;

import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginRequestMessage;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

public class PluginRouter {
    public static void route(PluginRequestMessage requestMessage) throws Exception {
        MessageProtos.Message message = MessageFactory.instance().createPluginMessage(requestMessage);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(message, null);
    }
}
