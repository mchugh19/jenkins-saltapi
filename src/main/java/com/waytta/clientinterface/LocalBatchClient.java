package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.Symbol;

import com.waytta.Utils;



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
    
    @Symbol("batch")
    public static final class DescriptorImpl extends BasicClientDescriptor {
        private DescriptorImpl(Class<? extends BasicClient> clazz) {
            super(clazz);
        }
        
        @Override
        public String getDisplayName() {
        	return "local_batch";
        }
    	
    	
    	public FormValidation doCheckBatchSize(@QueryParameter String value) {
    		if (value.length() == 0) {
    			return FormValidation.error("Please specify batch size");
    		}
    		return FormValidation.ok();
    	}
    	
        public FormValidation doCheckTarget(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }
    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl(LocalBatchClient.class);
}
