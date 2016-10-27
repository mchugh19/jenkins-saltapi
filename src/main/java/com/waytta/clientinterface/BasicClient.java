package com.waytta.clientinterface;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;


public class BasicClient implements ExtensionPoint, Describable<BasicClient> {
	public Descriptor<BasicClient> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    public static class BasicClientDescriptor extends Descriptor<BasicClient> {

        public BasicClientDescriptor(Class<? extends BasicClient> clazz) {
            super(clazz);
        }

        @Override
        public String getDisplayName() {
            if (clazz == LocalClient.class)
                return "local";
            if (clazz == LocalBatchClient.class)
                return "local_batch";
            if (clazz == RunnerClient.class)
                return "runner";
            return "";
        }
    }
	

    private final String credentialsId;
    private String target;
    private String targetType;
    private final String function;

    public BasicClient(String credentialsId, String target, String targetType, String function) {
        this.credentialsId = credentialsId;
        this.target = target;
        this.targetType = targetType;
        this.function = function;
    }

    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
    	this.target = target;
    }

    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
    	this.targetType = targetType;
    }

    public String getFunction() {
        return function;
    }

    public String getCredentialsId() {
        return credentialsId;
    }
}
