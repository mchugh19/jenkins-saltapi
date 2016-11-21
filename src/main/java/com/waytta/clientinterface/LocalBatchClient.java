package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.Symbol;

import com.waytta.Utils;


public class LocalBatchClient extends BasicClient {
	private String function;
	private String arguments;
	private String batchSize = "100%";
	private String target;
	private String targetType;

    @DataBoundConstructor
    public LocalBatchClient(String function, String arguments, String batchSize, String target, String targetType) {
    	this.function = function;
    	this.arguments = arguments;
    	this.batchSize = batchSize;
    	this.target = target;
    	this.targetType = targetType;
    }
    
    public String getFunction() {
    	return function;
    }
    
    public String getArguments() {
    	return arguments;
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
    
    @Symbol("batch") @Extension
    public static final class DescriptorImpl extends BasicClientDescriptor {
        public DescriptorImpl() {
            super(LocalBatchClient.class);
        }
        
        @Override
        public String getDisplayName() {
        	return "local_batch";
        }
    	
        public FormValidation doCheckFunction(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt function", "Isn't it too short?");
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
    
    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
