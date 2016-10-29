package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;



public class LocalBatchClient extends BasicClient {
    private String batchSize = "100%";
    private String target;
    private String targetType;

    @DataBoundConstructor
    public LocalBatchClient(String batchSize, String target, String targetType) {
        this.batchSize = batchSize;
        this.target = target;
        this.targetType = targetType;
    }
    
    public String getBatchSize() {
        return batchSize;
    }
    
    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }
    
    //@Extension
    //public static final BasicClientDescriptor DESCRIPTOR = new BasicClientDescriptor(LocalBatchClient.class);
    
    //@Extension
    //public static class DescriptorImpl extends BasicClientDescriptor {
    @Extension 
    public static final class DescriptorImpl extends BasicClient.BasicClientDescriptor {
    	public FormValidation doCheckBatchSize(@QueryParameter String value) {
    		if (value.length() == 0) {
    			return FormValidation.error("Please specify batch size");
    		}
    		return FormValidation.ok();
    	}
    }
}
