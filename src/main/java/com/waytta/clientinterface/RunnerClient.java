package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import org.kohsuke.stapler.QueryParameter;
import hudson.util.FormValidation;
import net.sf.json.JSONSerializer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSON;
import net.sf.json.JSONException;
import org.jenkinsci.Symbol;

import com.waytta.clientinterface.BasicClient.BasicClientDescriptor;
import com.waytta.clientinterface.LocalBatchClient.DescriptorImpl;

public class RunnerClient extends BasicClient {
    private String mods = "";
    private String pillarvalue = "";
    
    @DataBoundConstructor
    public RunnerClient(String mods, String pillarvalue){
        this.pillarvalue = pillarvalue;
        this.mods = mods;
    }

    public String getPillarvalue() {
        return pillarvalue;
    }

    public String getMods() {
        return mods;
    }
    
    @Symbol("runner")
    public static final class DescriptorImpl extends BasicClientDescriptor {
        private DescriptorImpl(Class<? extends BasicClient> clazz) {
            super(clazz);
        }
        
        @Override
        public String getDisplayName() {
        	return "runner";
        }
    	
        public FormValidation doCheckPillarvalue(@QueryParameter String value) {
        	if (value.length() > 0) {
	        	// Check to see if paramorized. Ex: {{variable}}
	        	// This cannot be evaluated until build, so trust that all is well
	        	Pattern pattern = Pattern.compile("\\{\\{\\w+\\}\\}");
	            Matcher matcher = pattern.matcher(value);
	            if (matcher.matches()) {
	            	return FormValidation.ok();
	            }
	        	try {
	                // If value was already a jsonobject, treat it as such
	                JSON runPillarValue = JSONSerializer.toJSON(value);
	            } catch (JSONException e) {
	                // Otherwise it must have been a string
	            	return FormValidation.error("Pillar should be in JSON format");
	            }
        	}
            return FormValidation.ok();
        }

    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl(RunnerClient.class);
}
