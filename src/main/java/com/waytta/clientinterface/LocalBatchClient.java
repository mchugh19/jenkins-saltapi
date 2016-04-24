package com.waytta.clientinterface;

public class LocalBatchClient extends AbstractClientInterface {

    public LocalBatchClient(String credentialsId, String servername, String authtype, String target, String targettype, String function, String batchSize) {
        super(credentialsId, servername, authtype, target, targettype, function, "100%");

        setBatchSize(batchSize);
    }

}
