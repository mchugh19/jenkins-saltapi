package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class LocalBatchClient extends BasicClient {
    private String batchSize = "100%";

    @DataBoundConstructor
    public LocalBatchClient(String target, String targetType, String batchSize) {
        super(target, targetType);

        this.batchSize = batchSize;
    }
    
    public String getBatchSize() {
        return batchSize;
    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new BasicClientDescriptor(LocalBatchClient.class);
}
