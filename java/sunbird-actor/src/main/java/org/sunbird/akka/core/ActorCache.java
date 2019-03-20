package org.sunbird.akka.core;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches the operation and its corresponding ActorRef
 */
public class ActorCache {
    // There seems to be no unified way to denote an actorref or actorselection
    private final static Map<String, ActorRef> routingMap = new HashMap<>();
    private final static Map<String, ActorSelection> remoteRoutingMap = new HashMap<>();
    private static ActorCache cacheRef = null;
    private static Boolean localLock = false;

    /**
     * Disallow external instantiation
     */
    private ActorCache() {}

    /**
     * Externally used to get an instance of this class
     * @return
     */
    public static ActorCache instance() {
        if (!localLock) {
            synchronized (localLock) {
                localLock = Boolean.TRUE;
                if (null == cacheRef) {
                    cacheRef = new ActorCache();
                }
            }
        }
        return cacheRef;
    }

    /**
     * Add the ActorRef to the cache
     * @param absName
     * @param actor
     */
    public void add(String absName, ActorRef actor) {
        routingMap.put(absName, actor);
    }

    /**
     * Add the ActorRef to the cache
     * @param absName
     * @param actor
     */
    public void add(String absName, ActorSelection actor) {
        remoteRoutingMap.put(absName, actor);
    }

    public ActorRef get(String simpleName) {
        return routingMap.get(simpleName);
    }

    public ActorSelection getRemote(String simpleName) {
        return remoteRoutingMap.get(simpleName);
    }

    protected void print() {
        routingMap.forEach((k,v) -> {
            System.out.println(k + " -> " + v);
        });
        remoteRoutingMap.forEach((k,v) -> {
            System.out.println(k + " -> " + v);
        });
    }
}
