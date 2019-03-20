package org.sunbird.akka.core;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Value;

/**
 * An abstract actor
 */
public abstract class BaseActor extends UntypedAbstractActor {
    protected final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    protected MessageProtos.Message.Builder responseMsgBldr;
    protected final String ON_FAILURE_METHOD_NAME = "onFailure";
    protected final String ON_SUCCESS_METHOD_NAME = "onSuccess";

    @Override
    public void preStart() throws Exception {
        super.preStart();
        String name = self().path().name();
        logger.debug("Actor {} getting ready", name);
    }

    protected abstract void onReceive(MessageProtos.Message request) throws Throwable;

    protected void onSuccess(MessageProtos.Message request) {
        logger.debug("Generic success handler");
    }

    protected void onFailure(MessageProtos.Message request) {
        logger.debug("Generic success handler");
    }


    /**
     * Tells the request to the source - local or remote
     * @param request
     * @return
     */
    public void tellToSource(MessageProtos.Message request) {
        logger.info("REPLY ok to Source actor {}", sender().path().toSerializationFormat());
        sender().tell(request, getSelf());
    }


    @Override
    public void onReceive(Object genericMessage) throws Throwable {
        if (genericMessage != null) {
        MessageProtos.Message sunbirdActorMessage = null;
        try {
            sunbirdActorMessage = (MessageProtos.Message) genericMessage;
            String targetActorName = sunbirdActorMessage.getTargetActorName();

            boolean isSelfTargetted = sunbirdActorMessage.getSourceActorName() != null &&
                    sunbirdActorMessage.getTargetActorName() != null &&
                    targetActorName.equals(sunbirdActorMessage.getSourceActorName());
            if (isSelfTargetted) {
                logger.error("Eh! sending messages to self. Recheck logic");
                return;
            }

            String operation = sunbirdActorMessage.getPerformOperation();
            if (operation != null && operation.compareTo(ON_FAILURE_METHOD_NAME) == 0) {
                onFailure(sunbirdActorMessage);
            } else if (operation != null && operation.compareTo(ON_SUCCESS_METHOD_NAME) == 0) {
                onSuccess(sunbirdActorMessage);
            } else {
                MessageProtos.Message.Builder msgSourceBldr = setSourceActorName(sunbirdActorMessage);
                MessageProtos.Message msgWithSrc = sunbirdActorMessage;
                if (msgSourceBldr != null) {
                    msgWithSrc = msgSourceBldr.build();
                }

                responseMsgBldr = MessageProtos.Message.newBuilder(msgWithSrc);

                if (!self().path().parent().name().equals(Router.ROUTER_NAME)) {
                    logger.info("SEND message from {} to {}",
                            msgWithSrc.getSourceActorName(),
                            msgWithSrc.getTargetActorName());
                }

                // Act upon the message.
                onReceive(msgWithSrc);

                // Ack if this is of type 'ask'.
                if (msgWithSrc.getMsgOption() == MessageProtos.MessageOption.GET_BACK_RESPONSE) {
                    responseMsgBldr.setPerformOperation("onSuccess");
                    Value.Builder payloadBuilder = responseMsgBldr.getPayloadBuilder();
                    payloadBuilder.setStringValueBytes(ByteString.EMPTY);

                    MessageProtos.Message response = responseMsgBldr.build();

                    tellToSource(response);
                }
            }

        } catch (ClassCastException e) {
            logger.info("Ignoring message because it is not in expected format {}", genericMessage.toString());
        }
        }
    }

    /**
     * For all 'ask' types, this is the function that will be hit.
     * @param message
     * @param result
     * @param exception
     */
    protected final void onResponse(MessageProtos.Message message, Object result, Throwable exception) {
        Value.Builder payloadBuilder = responseMsgBldr.getPayloadBuilder();
        responseMsgBldr = MessageProtos.Message.newBuilder(message);

        if (exception != null) {
            logger.info("Exception in message processing for {} :: message: {}",
                    message.getSourceActorName(), exception.getMessage(), exception);
            responseMsgBldr.setPerformOperation("onFailure");

            payloadBuilder.setStringValueBytes(ByteString.copyFromUtf8(exception.getMessage()));
        } else {
            responseMsgBldr.setPerformOperation(ON_SUCCESS_METHOD_NAME);
            if (result != null) {
                payloadBuilder.setStringValueBytes(ByteString.copyFromUtf8(result.toString()));
            } else {
                payloadBuilder.setStringValueBytes(ByteString.EMPTY);
            }
        }

        // End of the message.
        tellToSource(responseMsgBldr.build());
    }

    /**
     * Sets the sourceActorName if it is not already set. This is always set with fully
     * qualified name.
     * @param request
     * @return
     */
    public final MessageProtos.Message.Builder setSourceActorName(MessageProtos.Message request) {
        MessageProtos.Message.Builder result = null;
        String simpleName = getSourceActorName(request);
        if (simpleName != null && simpleName.isEmpty()) {
            String absName = sender().path().parent().toSerializationFormat();
            int pos = absName.lastIndexOf("#");
            if (pos != -1) {
                simpleName = absName.substring(0, pos);
            }
            result = MessageProtos.Message.newBuilder(request).setSourceActorName(simpleName);
        }
        return result;
    }

    /**
     * Gets the source actor name who sent this request
     * If request doesn't contain source, then this identifies from context.
     * @param request
     * @return
     */
    public final String getSourceActorName(MessageProtos.Message request) {
        String simpleName = request.getSourceActorName();
        return simpleName;
    }
}


