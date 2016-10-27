package com.waytta.clientinterface;

import hudson.Extension;

public class LocalBatchClient extends BasicClient {
    private String batchSize = "100%";

    public LocalBatchClient(String credentialsId, String target, String targettype, String function, String batchSize) {
        super(credentialsId, target, targettype, function);

        this.batchSize = batchSize;
        setTarget(target);
        setTargetType(targettype);
    }
    
    public String getBatchSize() {
        return batchSize;
    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new BasicClientDescriptor(LocalBatchClient.class);
}
