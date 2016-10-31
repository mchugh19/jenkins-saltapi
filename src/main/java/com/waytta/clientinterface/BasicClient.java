package com.waytta.clientinterface;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.DescriptorExtensionList;
import java.util.List;


abstract public class BasicClient implements ExtensionPoint, Describable<BasicClient> {
	public String getTarget() {
		return "";
	}
	
	public String getTargetType() {
		return "";
	}
    
	public boolean getBlockbuild() {
		return false;
	}
    
	public String getBatchSize() {
		return "";
	}
    
	public int getJobPollTime() {
		return 10;
	}
    
	public String getMods() {
		return "";
	}
    
	public String getPillarvalue() {
		return "";
	}

	
	public Descriptor<BasicClient> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }
    
    public List<BasicClientDescriptor> getClientDescriptors() {
    	return Jenkins.getInstance().getDescriptorList(BasicClient.class);
    }

    abstract static class BasicClientDescriptor extends Descriptor<BasicClient> {
        public BasicClientDescriptor(Class<? extends BasicClient> clazz) {
            super(clazz);
        }

        abstract public String getDisplayName();
    }

}
