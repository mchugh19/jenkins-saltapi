package com.waytta.clientinterface;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.DescriptorExtensionList;
import java.util.List;


public class BasicClient implements ExtensionPoint, Describable<BasicClient> {
	public Descriptor<BasicClient> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }
    
    public List<BasicClientDescriptor> getClientDescriptors() {
    	return Jenkins.getInstance().getDescriptorList(BasicClient.class);
    }

    public abstract class BasicClientDescriptor extends Descriptor<BasicClient> {

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
	
    protected BasicClient() {
    }
}
