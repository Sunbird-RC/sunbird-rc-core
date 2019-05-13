package org.sunbird.akka.core;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.FromConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import org.sunbird.akka.config.ConfigProcessor;


/**
 * This class will be responsible for bringing up the actor system and actors.
 */
public class SunbirdActorFactory {
    private ConfigProcessor configProcessor;
    private String actorScanPackage;
    private ActorSystem actorSystem;
    private ActorRef router;

    public SunbirdActorFactory(Config config, String actorScanPackage) {
        this.configProcessor = new ConfigProcessor(config);
        this.actorScanPackage = actorScanPackage;
    }

    /**
     * Inits the basic needs of the utility library
     * @param name
     */
    public void init(String name) {
        createActorSystem(name);
        createRouter();
        //createDeadLetterActor();
        initActors();
        printCache();
    }

    /**
     * Creates the actor system
     * @param name
     */
    private void createActorSystem(String name) {
        Config config = configProcessor.getConfig();
        actorSystem = ActorSystem.create(name, config.getConfig(name));
    }

    /**
     * Creates a router
     */
    private void createRouter() {
        router = actorSystem.actorOf(
                FromConfig.getInstance()
                        .props(
                                Props.create(Router.class).withDispatcher(getDispatcherName(Router.class))),
                Router.ROUTER_NAME);
        ActorCache.instance().add(Router.ROUTER_NAME, router);
    }

    /**
     * Init actors both local and remote
     */
    private void initActors() {
        ActorCache actorCache = ActorCache.instance();

        ConfigObject deployed = configProcessor.getConfig().getObject(this.actorSystem.name() + ".akka.actor.deployment");
        deployed.entrySet().forEach(stringConfigValueEntry -> {
            ConfigValue val = stringConfigValueEntry.getValue();
            if (val.valueType().compareTo(ConfigValueType.OBJECT) == 0) {
                ConfigObject valObj = (ConfigObject) val;
                if (valObj.containsKey("remote")) {
                    // Remote actors
                    String remotePath = valObj.get("remote").render().replace("\"","");
                    ActorSelection selection =
                            this.actorSystem.actorSelection(remotePath);
                    actorCache.add(stringConfigValueEntry.getKey().substring(1), selection);
                } else if (!stringConfigValueEntry.getKey().contains(Router.ROUTER_NAME)) {
                    // Local actors initialization other than router
                    ActorRef actorRef = null;
                    String className = actorScanPackage + "." + stringConfigValueEntry.getKey().substring(1);
                    try {
                        actorRef = createLocalActor(actorSystem, Class.forName(className));
                        actorCache.add(ActorUtils.getName(actorRef), actorRef);
                    } catch (ClassNotFoundException notFound) {
                        // class is not found, looks developer didn't implement
                        System.out.println(className + " not found.");
                    }
                }
            }
        });
    }

    /**
     * Gets the dispatcher name from the configuration
     * CAVEAT: The actors are not hierarchical.
     * @param actor
     * @return
     */
    private String getDispatcherName(Class<? extends BaseActor> actor) {
        String completePath = this.actorSystem.name() + ".akka.actor.deployment./" + actor.getSimpleName()+ ".dispatcher";
        String dispatcher = "";
        try {
            dispatcher = configProcessor.getConfig().getString(completePath);
        } catch (ConfigException missingConfig) {
            // System.out.println("Dispatcher not provided and so default");
        }
        return dispatcher;
    }

    /**
     * Creates an actor
     * @param actorContext
     * @param actor
     * @return
     */
    private ActorRef createLocalActor(
            ActorSystem actorContext,
            Class actor) {
        Props props;
        String dispatcher = getDispatcherName(actor);

        if (null != dispatcher) {
            props = Props.create(actor).withDispatcher(dispatcher);
        } else {
            props = Props.create(actor);
        }

        String name = actor.getSimpleName();
        ActorRef actorRef = actorContext.actorOf(FromConfig.getInstance().props(props), name);

        return actorRef;
    }

    // TODO - would it make sense to have a dead letter watcher and send back
    // an "unreachable" event for all types of messages - tell and ask?
//    private void createDeadLetterActor() {
//        final ActorRef actor = actorSystem.actorOf(Props.create(DeadLetterActor.class));
//        actorSystem.eventStream().subscribe(actor, DeadLetter.class);
//    }


    private void printCache() {
        ActorCache.instance().print();
    }
}
