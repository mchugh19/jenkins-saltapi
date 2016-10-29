package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;

import com.waytta.Utils;

public class LocalClient extends BasicClient {
    public static final int DEFAULT_JOB_POLL_TIME = 10;
    private Integer jobPollTime = DEFAULT_JOB_POLL_TIME;
    private Boolean blockbuild = false;
    private String target;
    private String targetType;

    @DataBoundConstructor
    public LocalClient(Boolean blockbuild, Integer jobPollTime, String target, String targetType) {
        this.blockbuild = blockbuild;
        this.jobPollTime = jobPollTime;
        this.target = target;
        this.targetType = targetType;
    }
    
    public Integer getJobPollTime() {
        return jobPollTime;
    }
    
    public Boolean getBlockbuild() {
        return blockbuild; 
    }
    
    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }
    
    @Symbol("local")
    public static final class DescriptorImpl extends BasicClientDescriptor {
        private DescriptorImpl(Class<? extends BasicClient> clazz) {
            super(clazz);
        }
        
        @Override
        public String getDisplayName() {
        	return "local";
        }
    	
        public FormValidation doCheckTarget(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }
    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl(LocalClient.class);
}
