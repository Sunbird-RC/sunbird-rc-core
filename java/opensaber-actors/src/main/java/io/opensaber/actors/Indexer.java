package io.opensaber.actors;

import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

public class Indexer extends BaseActor {

    @Override
    protected void onReceive(MessageProtos.Message message) throws Throwable {

    }

    @Override
    protected void onSuccess(MessageProtos.Message request) {
        super.onSuccess(request);
    }

    @Override
    protected void onFailure(MessageProtos.Message request) {
        super.onFailure(request);
    }
}
