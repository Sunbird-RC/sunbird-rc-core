## Sunbird-Actor

### Motivation
The dev teams need to either create or use Akka library for being able to tear apart modules, that could
be run independent without any trouble. This library is intended to provide easy way to embrace Akka
library for teams that may be new to distributed systems world.

### Features
1.  Drivers are Akka configuration file and @Sunbird annotations

 The Akka configuration syntax and semantics are described [here](https://doc.akka.io/docs/akka/2.5/general/configuration.html).
    This file forms the basis whether you want to instantiate an actor (local actor) or refer/point to an
    actor running inside another system. For example, the dispatcher configuration defines how many instances
    of the actor must be created. Use this carefully to design your system.
    @Sunbird annotation is required a class if you want to locally instantiate the actor. Override, the
    onReceive() function to supply your logic. You may optionally override handleFailure() function to take
    care of any failures reported by the other actor.

2.  Google protobuf serialization

 Messages between the actors are passed by the protobuf serialization engine. If you're not aware of this, take a read
    [here] (https://developers.google.com/protocol-buffers/docs/javatutorial). The default java serialization is not
    recommended for production use. The original Message.proto used by the protoc (protobuf compiler) is also available
    in the root of this library directory.

3.  Router

 /Router is a special actor created by the library that helps in routing messages. Even if there are no actors configured
 in the system, there will be a /Router. So, supply a dispatcher configuration for this actor for sure. All the other actors
 and /Router are siblings (not hierarchical).

### Example
* [System1-example] (https://github.com/indrajra/sunbird-actor-ref/tree/master/system1-example)
   Contains code samples for message sending and receiving
* [System2-example] (https://github.com/indrajra/sunbird-actor-ref/tree/master/system2-example)
   Contains code sample for running the actor system remotely

### Message
This forms the core of the data passed between the actors. The following are its key attributes
*  *targetActorName* - the target actor to which this message is addressed. Of the form, */<actorName1>* when there is no parent
or */<actorName1>/<actorName2>* where *actorName2* is the child of *actorName1*

*  *sourceActorName* - the source actor from which the message is originating. This would be /deadLetters if done by application.

*  *payload* - This could be any type. Preferably json that could be serialized. Checkout [SendHello.java](https://github.com/indrajra/sunbird-actor-ref/blob/master/system1-example/src/main/java/org/sunbird/akka/example1/actors/SendHello.java)

*  *operationName* - The operation name is an internal name to supply to the actor.
Say, an actor called 'calculator' could perform actions add and multiply. The operationName could be 'add' and 'multiply'.
In another way, one might want to invoke different actors based on the operationName. Basically, this is available for
basing the behaviour of an actor. This could be ignored and one may use the payload itself for this too.