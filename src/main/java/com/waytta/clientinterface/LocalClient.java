package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.QueryParameter;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;

import com.waytta.Utils;

public class LocalClient extends BasicClient {
    public static final int DEFAULT_JOB_POLL_TIME = 10;
    private int jobPollTime = DEFAULT_JOB_POLL_TIME;
    private boolean blockbuild = false;
    private String target;
    private String targetType;

    @DataBoundConstructor
    public LocalClient(String target, String targetType) {
        this.target = target;
        this.targetType = targetType;
    }
    
    public int getJobPollTime() {
        return jobPollTime;
    }
    
    @DataBoundSetter
    public void setJobPollTime(int jobPollTime) {
        this.jobPollTime = jobPollTime;
    }
    
    public boolean getBlockbuild() {
        return blockbuild; 
    }
    
    @DataBoundSetter
    public void setBlockbuild(boolean blockbuild) {
        this.blockbuild = blockbuild;
    }
    
    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }
   
    @Symbol("local") @Extension
    public static class DescriptorImpl extends BasicClientDescriptor {
    	public DescriptorImpl() {
            super(LocalClient.class);
        }
        
        @Override
        public String getDisplayName() {
        	return "local";
        }
    	
        public FormValidation doCheckTarget(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }
    }
    
    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
