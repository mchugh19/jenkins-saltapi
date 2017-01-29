package com.waytta;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.Symbol;
import com.waytta.clientinterface.BasicClient;
import com.waytta.clientinterface.LocalBatchClient;
import com.waytta.clientinterface.LocalClient;
import com.waytta.clientinterface.RunnerClient;
import org.yaml.snakeyaml.Yaml;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.Item;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.security.ACL;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.AncestorInPath;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

public class SaltAPIBuilder extends Builder {
    private static final Logger LOGGER = Logger.getLogger("com.waytta.saltstack");

    private String servername;
    private String authtype;
    private String function;
    private String arguments;
    private String kwarguments;
    private BasicClient clientInterface;
    private boolean saveEnvVar = false;
    private final String credentialsId;


    @DataBoundConstructor
    public SaltAPIBuilder(String servername, String authtype, BasicClient clientInterface, String credentialsId) {
        this.servername = servername;
        this.authtype = authtype;
        this.clientInterface = clientInterface;
        this.credentialsId = credentialsId;
    }
  
    public String getServername() {
        return servername;
    }

    public String getAuthtype() {
        return authtype;
    }

    public String getTarget() {
    	return clientInterface.getTarget();
    }

    public String getTargettype() {
    	return clientInterface.getTargetType();
    }

    public String getFunction() {
        return clientInterface.getFunction();
    }

    public String getArguments() {
        return clientInterface.getArguments();
    }

    public boolean getBlockbuild() {
    	return clientInterface.getBlockbuild();
    }

    public String getBatchSize() {
    	return clientInterface.getBatchSize();
    }

    public int getJobPollTime() {
    	return clientInterface.getJobPollTime();
    }
    
    public String getMods() {
    	return clientInterface.getMods();
    }
    
    public String getPillarvalue() {
    	return clientInterface.getPillarvalue();
    }
    
    public String getSubset() {
    	return clientInterface.getSubset();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setSaveEnvVar(boolean saveEnvVar) {
        this.saveEnvVar = saveEnvVar;
    }

    public boolean getSaveEnvVar() {
        return saveEnvVar;
    }
    
    public BasicClient getClientInterface() {
    	return clientInterface;
    }
    
    public String getPost() {
    	return clientInterface.getPost();
    }
    
    public String getTag() {
    	return clientInterface.getTag();
    }


    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String myOutputFormat = getDescriptor().getOutputFormat();
        String myClientInterface = clientInterface.getDescriptor().getDisplayName();
        String myservername = Utils.paramorize(build, listener, servername);
        String mytarget = Utils.paramorize(build, listener, getTarget());
        String myfunction = Utils.paramorize(build, listener, getFunction());
        String myarguments = Utils.paramorize(build, listener, getArguments());
        
        boolean myBlockBuild = getBlockbuild();
        boolean jobSuccess = true;

        StandardUsernamePasswordCredentials credential = Utils.getCredentialById(getCredentialsId());
        if (credential == null) {
            listener.error("Invalid credentials");
            return false;
        }

		 // Setup connection for auth
	    JSONObject auth = Utils.createAuthArray(credential, authtype);

	    // Get an auth token
	    String token = new String();
	    token = Utils.getToken(myservername, auth);
	    if (token.contains("Error")) {
	        listener.error(token);
	        return false;
	    }
	    
	    // If we got this far, auth must have been good and we've got a token
	    JSONObject saltFunc = prepareSaltFunction(build, listener, myClientInterface, mytarget, myfunction, myarguments);
	    LOGGER.log(Level.FINE, "Sending JSON: " + saltFunc.toString());
        
        JSONArray returnArray = performRequest(build, token, myservername, saltFunc, listener, myBlockBuild);
        LOGGER.log(Level.FINE, "Received response: " + returnArray);

        // Save saltapi output to env if requested
        if (saveEnvVar) {
            build.addAction(new PublishEnvVarAction("SALTBUILDOUTPUT", returnArray.toString()));
        }

        // Check for error and print out results
        boolean validFunctionExecution = Utils.validateFunctionCall(returnArray);

        if (!validFunctionExecution) {
            listener.error("One or more minion did not return code 0\n");
            jobSuccess = false;
        }

        // Just finish up if we don't output
        if (myOutputFormat.equals("none")) {
        	listener.getLogger().println("Completed " + myfunction + " " + myarguments);
        	return jobSuccess;
        }
        
        // Print results
        if (myfunction.length() > 0) {listener.getLogger().print("Response on " + myfunction);}
        if (myarguments.length() > 0) {listener.getLogger().print(" " + myarguments);}
        if (mytarget.length() > 0) {listener.getLogger().print(" for " + mytarget + ":");}
        listener.getLogger().println("");

        if (myOutputFormat.equals("json")) {
            listener.getLogger().println(returnArray.toString(2));
        } else if (myOutputFormat.equals("yaml")) {
            Object outputObject = returnArray.toArray();
            Yaml yaml = new Yaml();
            listener.getLogger().println(yaml.dump(outputObject));
        } else {
            listener.error("Unknown output Format: x" + myOutputFormat + "x");
            jobSuccess = false;
        }

        // Results now printed. Return success condition 
        return jobSuccess;
    }
    
	public JSONArray performRequest(AbstractBuild build, String token, String serverName, JSONObject saltFunc, BuildListener listener, boolean blockBuild) throws InterruptedException, IOException {
	    JSONArray returnArray = new JSONArray();

	    JSONObject httpResponse = new JSONObject();
	    // Access different salt-api endpoints depending on function
	    if (blockBuild) {
	    	int jobPollTime = getJobPollTime();
	        int minionTimeout = getDescriptor().getTimeoutTime();
	        // poll /minion for response
	    	returnArray = Builds.runBlockingBuild(build, returnArray, serverName, token, saltFunc, listener, jobPollTime, minionTimeout);
	    } else if (saltFunc.getString("client").equals("hook")) {
	    	// publish event to salt event bus to /hook
	    	String myTag = Utils.paramorize(build, listener, getTag());
	    	// Cleanup myTag to remove duplicate / and urlencode
	    	myTag = myTag.replaceAll("^/", "");
	    	myTag = URLEncoder.encode(myTag, "UTF-8");
	    	httpResponse = Utils.getJSON(serverName + "/hook/" + myTag, saltFunc, token);
	    	returnArray.add(httpResponse);
	    } else {
	        // Just send a salt request to /. Don't wait for reply
	        httpResponse = Utils.getJSON(serverName, saltFunc, token);
	        returnArray = httpResponse.getJSONArray("return");
	    }
	    
	    return returnArray;
    }
    
    private JSONObject prepareSaltFunction(AbstractBuild build, BuildListener listener, String myClientInterface, String mytarget,
			String myfunction, String myarguments) throws IOException, InterruptedException {
		JSONObject saltFunc = new JSONObject();
		saltFunc.put("client", myClientInterface);

		switch (myClientInterface) {
		case "local_batch":
			String mybatch = Utils.paramorize(build, listener, getBatchSize());
			saltFunc.put("batch", mybatch);
			listener.getLogger().println("Running in batch mode. Batch size: " + mybatch);
			break;
		case "runner":
			saltFunc.put("mods", getMods());
			String myPillarvalue = Utils.paramorize(build, listener, getPillarvalue());
			if (myPillarvalue.length() > 0) {
				// If value was already a jsonobject, treat it as such
				JSON runPillarValue = JSONSerializer.toJSON(myPillarvalue);
				saltFunc.put("pillar", runPillarValue);
			}
			break;
		case "local_subset":
			String mySubset = Utils.paramorize(build, listener, getSubset());
			saltFunc.put("sub", Integer.parseInt(mySubset));
			listener.getLogger().println("Running in subset mode. Subset size: " + mySubset);
			break;
		case "hook":
			// Posting to /hook url should only contain object to be posted
			String myPost = Utils.paramorize(build, listener, getPost());
			saltFunc = JSONObject.fromObject(myPost);
			return saltFunc;
		}

		saltFunc.put("tgt", mytarget);
		saltFunc.put("expr_form", getTargettype());
		saltFunc.put("fun", myfunction);
		Builds.addArgumentsToSaltFunction(myarguments, saltFunc);

		return saltFunc;
	}
    

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension @Symbol("salt")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        int pollTime = 10;
        int timeoutTime = 30;
        String outputFormat = "json";

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            try {
                // Test that value entered in config is an integer
                pollTime = formData.getInt("pollTime");
                timeoutTime = formData.getInt("timeoutTime");
            } catch (Exception e) {
                // Fall back to default
                pollTime = 10;
                timeoutTime = 30;
            }
            outputFormat = formData.getString("outputFormat");
            save();
            return super.configure(req, formData);
        }

        public int getPollTime() {
            return pollTime;
        }
        
        public int getTimeoutTime() {
	        return timeoutTime;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public FormValidation doTestConnection(
                @QueryParameter String servername,
                @QueryParameter String credentialsId,
                @QueryParameter String authtype) {
            StandardUsernamePasswordCredentials usedCredential = null;
            List<StandardUsernamePasswordCredentials> credentials = Utils.getCredentials(Jenkins.getInstance());
            for (StandardUsernamePasswordCredentials credential : credentials) {
                if (credential.getId().equals(credentialsId)) {
                    usedCredential = credential;
                    break;
                }
            }

            if (usedCredential == null) {
                return FormValidation.error("CredentialId error: no credential found with given ID.");
            }

            if (!servername.matches("\\{\\{\\w+\\}\\}")) {
                JSONObject auth = new JSONObject();
                auth.put("username", usedCredential.getUsername());
                auth.put("password", usedCredential.getPassword().getPlainText());
                auth.put("eauth", authtype);
                String token = Utils.getToken(servername, auth);
                if (token.contains("Error")) {
                    return FormValidation.error("Client error: " + token);
                }

                return FormValidation.ok("Success");
            }

            return FormValidation.warning("Cannot expand parametrized server name.");
        }

        public StandardListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Jenkins context,
                @QueryParameter final String servername) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            result.withMatching(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                    Utils.getCredentials(context));
            return result;
        }

        public FormValidation doCheckServername(@QueryParameter String value) {
            if (!value.matches("\\{\\{\\w+\\}\\}")) {
                if (value.length() == 0) {
                    return FormValidation.error("Please specify a name");
                }
                if (value.length() < 10) {
                    return FormValidation.warning("Isn't the name too short?");
                }
                if (!value.contains("https://") && !value.contains("http://")) {
                    return FormValidation
                            .warning("Missing protocol: Servername should be in the format https://host.domain:8000");
                }
                if (!value.substring(7).contains(":")) {
                    return FormValidation
                            .warning("Missing port: Servername should be in the format https://host.domain:8000");
                }
                return FormValidation.ok();
            }

            return FormValidation.warning("Cannot expand parametrized server name.");
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project, @QueryParameter String value) {
            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }

            if (value == null) {
                return FormValidation.ok();
            }

            StandardUsernamePasswordCredentials usedCredential = null;
            List<StandardUsernamePasswordCredentials> credentials = Utils.getCredentials(Jenkins.getInstance());
            for (StandardUsernamePasswordCredentials credential : credentials) {
                if (credential.getId().equals(value)) {
                    usedCredential = credential;
                }
            }

            if (usedCredential == null) {
                return FormValidation.error("Cannot find any credentials with id " + value);
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPollTime(@QueryParameter String value) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error("Specify a number larger than 3");
            }
            if (Integer.parseInt(value) < 3) {
                return FormValidation.warning("Specify a number larger than 3");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckTimeoutTime(@QueryParameter String value) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error("Specify a number larger than 3");
            }
            if (Integer.parseInt(value) < 3) {
                return FormValidation.warning("Specify a number larger than 3");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Send a message to Salt API";
        }
    }
}
