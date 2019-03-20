package org.sunbird.akka.core;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static org.sunbird.akka.core.MessageProtos.MessageOption.GET_BACK_RESPONSE;
import static org.sunbird.akka.core.MessageProtos.MessageOption.SEND_AND_FORGET;

/**
 * A specialized actor of the library that allows us to direct the
 * Message to the appropriate actor
 */
public class Router extends BaseActor {
    /**
     * Sunbird Actor router
     */
    public static final String ROUTER_NAME = "SBRouter";

    /**
     * Public to override by implementors
     */
    public static int WAIT_TIME_VALUE = 10;

    // TODO - Could there be a supervisor strategy?
//    private static SupervisorStrategy strategy =
//            new OneForOneStrategy(10, Duration.create(1, TimeUnit.MINUTES),
//                    DeciderBuilder.match(java.net.ConnectException.class, e -> SupervisorStrategy.resume())
//                            .match(AskTimeoutException.class, e -> SupervisorStrategy.resume())
//                            .build());
//
//    @Override
//    public SupervisorStrategy supervisorStrategy() {
//        return strategy;
//    }

    @Override
    public void onReceive(MessageProtos.Message request) {
        route(request);
    }

    /**
     * Tells the request to the target - local or remote
     * @param request
     * @return
     */
    public boolean tellToTarget(MessageProtos.Message request) {
        if (request.getTargetActorName().equals("/")) {
            return true;
        }

        ActorRef ref = ActorCache.instance().get(request.getTargetActorName());
        ActorSelection actorSelection = null;
        if (ref == null) {
            actorSelection = ActorCache.instance().getRemote(request.getTargetActorName());
        }

        if (request.getMsgOption() ==
                SEND_AND_FORGET) {
            if (ref != null) {
                ref.tell(request, self());
            } else if (actorSelection != null) {
                actorSelection.tell(request, self());
            }
        } else if (request.getMsgOption() == GET_BACK_RESPONSE) {
            route(actorSelection, ref, request, getContext().dispatcher());
        }
        return (ref != null || actorSelection != null);
    }

    /**
     * The source of the message may be different from that of the router
     * in case of remote calling.
     * In such cases, this method will inform to the right source.
     * @param request
     */
    public void tellToSource(MessageProtos.Message request) {
        String sourceName = request.getSourceActorName();

        ActorSelection selection = getContext().actorSelection(sourceName);
        selection.tell(request, getSelf());
    }

    /**
     * Routes the message to the relevant target
     * Also sets the source before routing it
     * @param request
     */
    public void route(MessageProtos.Message request) {
        if (!request.getPerformOperation().equals(ON_FAILURE_METHOD_NAME) ||
                !request.getPerformOperation().equals(ON_SUCCESS_METHOD_NAME)) {
            if (!tellToTarget(request)) {
                onResponse(request, null, new Exception("Actor not found"));
            }
        }
    }

    /**
     * Asks the actor to reply to the message.
     *
     * @param router
     * @param message
     * @return boolean
     */
    private boolean route(ActorSelection router, ActorRef ref, MessageProtos.Message message, ExecutionContext ec) {
        logger.info("Actor Service Call start for api {}", message.getTargetActorName());
        Timeout timeout = new Timeout(Duration.create(WAIT_TIME_VALUE, TimeUnit.SECONDS));
        Future<Object> future = null;
        if (router == null) {
            future = Patterns.ask(ref, message, timeout);
        } else {
            future = Patterns.ask(router, message, timeout);
        }

        future.onComplete(
                new OnComplete<Object>() {
                    @Override
                    public void onComplete(Throwable failure, Object result) {
                        if (failure != null) {
                            // We got a failure, handle it here
                            logger.error(failure.getMessage(), failure);
                            failure.printStackTrace();
                        }
                        onResponse(message, result, failure);
                    }
                },
                ec);
        return true;
    }

    protected void onSuccess(MessageProtos.Message request) {
        // divert this to the right source
        tellToSource(request);
    }

    protected void onFailure(MessageProtos.Message request) {
        // divert this to the right source.
        tellToSource(request);
    }
}

