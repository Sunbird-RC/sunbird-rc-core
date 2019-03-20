package org.sunbird.akka.core;

import akka.actor.ActorRef;

public class ActorUtils {
    /**
     * Returns the name of the actor
     * This is presently used to add the actor to the cache
     * @param actor
     * @return
     */
    public static String getName(ActorRef actor) {
        return actor.path().name();
    }
}
