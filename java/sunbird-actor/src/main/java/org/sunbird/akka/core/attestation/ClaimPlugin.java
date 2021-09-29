package org.sunbird.akka.core.attestation;

import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

public class ClaimPlugin extends BaseActor {

    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        // call claim service and save the claim
        // how to attest

    }
}
