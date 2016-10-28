package com.waytta.clientinterface;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.DescriptorExtensionList;


public class BasicClient implements ExtensionPoint, Describable<BasicClient> {
	public Descriptor<BasicClient> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }
    
    public DescriptorExtensionList<BasicClient,Descriptor<BasicClient>> getClientDescriptors() {
        return Jenkins.getInstance().<BasicClient,Descriptor<BasicClient>>getDescriptorList(BasicClient.class);
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
	

    private String target;
    private String targetType;

    @DataBoundConstructor
    public BasicClient(String target, String targetType) {
        this.target = target;
        this.targetType = targetType;
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
}
