package com.waytta;

import java.io.IOException;
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
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
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

public class SaltAPIBuilder extends Builder implements SimpleBuildStep {
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
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        boolean success = perform(run, launcher, listener);
        if (!success) {
            throw new hudson.AbortException();
        }
    }

    //    @Override
    public boolean perform(Run build, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String myOutputFormat = getDescriptor().getOutputFormat();
        String myClientInterface = clientInterface.getDescriptor().getDisplayName();
        String myservername = Utils.paramorize(build, listener, servername);
        String mytarget = Utils.paramorize(build, listener, getTarget());
        String myfunction = Utils.paramorize(build, listener, getFunction());
        String myarguments = Utils.paramorize(build, listener, getArguments());
        
        boolean myBlockBuild = getBlockbuild();
        boolean jobSuccess = true;

        StandardUsernamePasswordCredentials credential = getCredentialById(getCredentialsId());
        if (credential == null) {
            listener.error("Invalid credentials");
            return false;
        }

        // Setup connection for auth
        String token = new String();
        JSONObject auth = createAuthArray(credential);
        JSONObject httpResponse = new JSONObject();
        JSONArray returnArray = new JSONArray();

        // Get an auth token
        token = Utils.getToken(myservername, auth);
        if (token.contains("Error")) {
            listener.error(token);
            return false;
        }
        
        // If we got this far, auth must have been good and we've got a token
        JSONObject saltFunc = prepareSaltFunction(build, listener, myClientInterface, mytarget, myfunction, myarguments);

        LOGGER.log(Level.FINE, "Sending JSON: " + saltFunc.toString());

        // Access different salt-api endpoints depending on function
        if (myBlockBuild) {
            // poll /minion for response
        	jobSuccess = runBlockingBuild(returnArray, myservername, token, saltFunc, listener);
        } else if (myClientInterface.equals("hook")) {
        	// publish event to salt event bus to /hook
        	String myTag = Utils.paramorize(build, listener, getTag());
        	// Cleanup myTag to remove extra characters
        	//myTag = myTag.replaceAll("\n", "");
        	httpResponse = Utils.getJSON(myservername + "/hook/" + myTag, saltFunc, token);
        	returnArray.add(httpResponse);
        } else {
            // Just send a salt request to /. Don't wait for reply
            httpResponse = Utils.getJSON(myservername, saltFunc, token);
            returnArray = httpResponse.getJSONArray("return");
        }
        // Finished processing

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
        if ( myClientInterface != "hook"){
        	listener.getLogger().println("Response on " + myfunction + " " + myarguments + " for "
                + mytarget + ":");
        }
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

    private JSONObject createAuthArray(StandardUsernamePasswordCredentials credential) {
        JSONObject auth = new JSONObject();
        auth.put("username", credential.getUsername());
        auth.put("password", credential.getPassword().getPlainText());
        auth.put("eauth", authtype);

        return auth;
    }

    private JSONObject prepareSaltFunction(Run build, TaskListener listener, String myClientInterface,
                                           String mytarget, String myfunction, String myarguments 
                                           ) throws IOException, InterruptedException {
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
            JSONObject jPillar = new JSONObject();
            // If value was already a jsonobject, treat it as such
            JSON runPillarValue = JSONSerializer.toJSON(myPillarvalue);
            saltFunc.put("pillar", runPillarValue);
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
        addArgumentsToSaltFunction(myarguments, saltFunc);

        return saltFunc;
    }


    private void addArgumentsToSaltFunction(String myarguments, JSONObject saltFunc) {
        if (myarguments.length() > 0) {
            JSONObject fullKwJSON = new JSONObject();

            // spit on space separated not inside of single and double quotes
            String[] argItems = myarguments.split("\\s+(?=(?:[^'\"]|'[^']*'|\"[^\"]*\")*$)");

            for (String arg : argItems) {
                // remove spaces at beginning or end
                arg = arg.replaceAll("^\\s+|\\s+$", "");
                // if string wrapped in quotes, remove them since adding to list
                // re-quotes
                arg = arg.replaceAll("(^')|(^\")|('$)|(\"$)", "");
                if (arg.contains("=")) {
                    String[] kwString = arg.split("=");
                    if (kwString.length > 2) {
                        // kwarg contained more than one =. Let's put the string
                        // back together
                        String kwFull = new String();
                        for (String kwItem : kwString) {
                            // Ignore the first item as it will remain the key
                            if (kwItem == kwString[0]) {
                                continue;
                            }
                            // add the second item
                            if (kwItem == kwString[1]) {
                            	// again remove any wrapping quotes
                                kwItem = kwItem.replaceAll("(^')|(^\")|('$)|(\"$)", "");
                                kwFull += kwItem;
                                continue;
                            }
                            // add all other items with an = to rejoin
                            kwFull += "=" + kwItem;
                        }
                        fullKwJSON.put(kwString[0], kwFull);
                    } else {
                    	// again remove any wrapping quotes
                        kwString[1] = kwString[1].replaceAll("(^')|(^\")|('$)|(\"$)", "");
        	        	try {
        	                // If value was already a jsonobject, treat it as such
        	                JSON kwJSON = JSONSerializer.toJSON(kwString[1]);
        	                // Rejoin key and json object
        	                fullKwJSON.element(kwString[0], kwJSON);
        	            } catch (JSONException e) {
        	                // Otherwise it must have been a string
                            fullKwJSON.put(kwString[0], kwString[1]);
        	            }
                    }
                } else {
                	// Add any args to json message
                	saltFunc.accumulate("arg", arg);
                }
            }
            // Now that loops have completed, add kwarg object
            saltFunc.element("kwarg", fullKwJSON);
        }
    }
    
    private boolean runBlockingBuild(JSONArray returnArray, String myservername, 
    		String token, JSONObject saltFunc, TaskListener listener) throws InterruptedException {
    	JSONObject httpResponse = new JSONObject();
    	String jid = new String();
    	// Send request to /minion url. This will give back a jid which we
    	// will need to poll and lookup for completion
    	httpResponse = Utils.getJSON(myservername + "/minions", saltFunc, token);
    	try {
    		returnArray = httpResponse.getJSONArray("return");
    		for (Object o : returnArray) {
    			JSONObject line = (JSONObject) o;
    			jid = line.getString("jid");
    		}
    		// Print out success
    		listener.getLogger().println("Running jid: " + jid);
    	} catch (Exception e) {
    		listener.error(httpResponse.toString(2));
    		return false;
    	}

    	// Request successfully sent. Now use jid to check if job complete
    	int numMinions = 0;
    	int numMinionsDone = 0;
    	JSONArray minionsArray = new JSONArray();
    	httpResponse = Utils.getJSON(myservername + "/jobs/" + jid, null, token);
    	try {
    		// info array will tell us how many minions were targeted
    		returnArray = httpResponse.getJSONArray("info");
    		for (Object o : returnArray) {
    			JSONObject line = (JSONObject) o;
    			minionsArray = line.getJSONArray("Minions");
    			// Check the info[Minions[]] array to see how many nodes we
    			// expect to hear back from
    			numMinions = minionsArray.size();
    			listener.getLogger().println("Waiting for " + numMinions + " minions");
    		}
    		returnArray = httpResponse.getJSONArray("return");
    		// Check the return[] array to see how many minions have
    		// responded
    		if (!returnArray.getJSONObject(0).names().isEmpty()) {
    			numMinionsDone = returnArray.getJSONObject(0).names().size();
    		} else {
    			numMinionsDone = 0;
    		}
    		listener.getLogger().println(numMinionsDone + " minions are done");
    	} catch (Exception e) {
    		listener.error(httpResponse.toString(2));
    		return false;
    	}

    	// Now that we know how many minions have responded, and how many we
    	// are waiting on. Let's see more have finished
    	if (numMinionsDone < numMinions) {
    		// Don't print annoying messages unless we really are waiting for
    		// more minions to return
    		listener.getLogger().println(
    				"Will check status every " + getJobPollTime() + " seconds...");
    	}
    	while (numMinionsDone < numMinions) {
    		try {
    			Thread.sleep(getJobPollTime() * 1000);
    		} catch (InterruptedException ex) {
    			Thread.currentThread().interrupt();
    			// Allow user to cancel job in jenkins interface
    			throw new InterruptedException();
    		}
    		Integer oldMinionsDone = numMinionsDone;
    		httpResponse = Utils.getJSON(servername + "/jobs/" + jid, null, token);
    		returnArray = httpResponse.getJSONArray("return");
    		numMinionsDone = returnArray.getJSONObject(0).names().size();
            Integer minionTimeout = getDescriptor().getTimeoutTime();
    		if (numMinionsDone > oldMinionsDone && numMinionsDone < numMinions) {
    			// Some minions returned, but not all
    			// Give them minionTimeout to all return or fail build
    			try {
    				listener.getLogger().println(
    						"Some minions returned. Waiting " + minionTimeout + " seconds");
    				Thread.sleep(minionTimeout * 1000);
    			} catch (InterruptedException ex) {
    				Thread.currentThread().interrupt();
    				// Allow user to cancel job in jenkins interface
    				throw new InterruptedException();
    			}
    			httpResponse = Utils.getJSON(servername + "/jobs/" + jid, null, token);
    			returnArray = httpResponse.getJSONArray("return");
    			numMinionsDone = returnArray.getJSONObject(0).names().size();
    			if (numMinionsDone < numMinions) {
    				JSONArray respondedMinions = returnArray.getJSONObject(0).names();
    				for (int i = 0; i < respondedMinions.size(); ++i) {
    					minionsArray.discard(respondedMinions.get(i));
    				}
    				listener.error(
    						"Minions timed out:\n" + minionsArray.toString() + "\n\n");
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
    StandardUsernamePasswordCredentials getCredentialById(String credentialId) {
        List<StandardUsernamePasswordCredentials> credentials = getCredentials(Jenkins.getInstance());
        for (StandardUsernamePasswordCredentials credential : credentials) {
            if (credential.getId().equals(credentialId)) {
                return credential;
            }
        }
        return null;
    }

    static List<StandardUsernamePasswordCredentials> getCredentials(Jenkins context) {
        List<DomainRequirement> requirements = URIRequirementBuilder.create().build();
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, context, ACL.SYSTEM, requirements);
        return credentials;
    }
    

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
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
            List<StandardUsernamePasswordCredentials> credentials = getCredentials(Jenkins.getInstance());
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
                    getCredentials(context));
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
            List<StandardUsernamePasswordCredentials> credentials = getCredentials(Jenkins.getInstance());
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
