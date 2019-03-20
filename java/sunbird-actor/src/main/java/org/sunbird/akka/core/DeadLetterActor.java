package org.sunbird.akka.core;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.DeadLetter;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DeadLetterActor extends AbstractActor {
    protected final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeadLetter.class, msg -> {
                    if (msg.message() instanceof MessageProtos.Message) {
                        MessageProtos.Message message = (MessageProtos.Message) msg.message();
                        logger.info("ERROR - Delivering message from {} to {}",
                                message.getSourceActorName(),
                                message.getTargetActorName());
                        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder(message);
                        msgBuilder.setPerformOperation("onFailure");
                        ActorSelection actorSelection =  getContext().actorSelection(message.getSourceActorName());
                        actorSelection.tell(msgBuilder.build(), null);
                    } else {
                        logger.info("Unknown message {}", msg.toString());
                    }
                })
                .build();
    }
}

