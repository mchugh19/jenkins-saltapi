package com.waytta.clientinterface;

public class LocalBatchClient extends BasicClient {

    public LocalBatchClient(String credentialsId, String target, String targettype, String function, String batchSize) {
        super(credentialsId, target, targettype, function);

        setBatchSize(batchSize);
        setTarget(target);
        setTargetType(targettype);
    }

}
