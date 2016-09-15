package com.waytta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.yaml.snakeyaml.Yaml;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.AncestorInPath;

import hudson.Extension;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONUtils;

public class SaltAPIBuilder extends Builder implements SimpleBuildStep {
    private static final Logger LOGGER = Logger.getLogger("com.waytta.saltstack");

    private final String servername;
    private final String authtype;
    private final String target;
    private final String targettype;
    private final String function;
    private String arguments;
    private String kwarguments;
    private JSONObject clientInterfaces;
    private final String clientInterface;
    private final Boolean blockbuild;
    private final Integer jobPollTime;
    private final String batchSize;
    private final String mods;
    private final Boolean usePillar;
    private final String pillarkey;
    private final String pillarvalue;

    private String credentialsId;
    private Boolean saveEnvVar;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public SaltAPIBuilder(String servername, String authtype, String target, String targettype, String function,
                          JSONObject clientInterfaces, String mods, String pillarkey,
                          String pillarvalue, String credentialsId) {
        this.credentialsId = credentialsId;
        this.servername = servername;
        this.authtype = authtype;
        this.target = target;
        this.targettype = targettype;
        this.function = function;
        this.clientInterfaces = clientInterfaces;
        if (clientInterfaces.has("clientInterface")) {
            this.clientInterface = clientInterfaces.get("clientInterface").toString();
        } else {
            this.clientInterface = "local";
        }
        if (clientInterface.equals("local")) {
            this.blockbuild = clientInterfaces.getBoolean("blockbuild");
            this.jobPollTime = clientInterfaces.getInt("jobPollTime");
            this.batchSize = "100%";
            this.mods = "";
            this.usePillar = false;
            this.pillarkey = "";
            this.pillarvalue = "";
        } else if (clientInterface.equals("local_batch")) {
            this.batchSize = clientInterfaces.get("batchSize").toString();
            this.blockbuild = false;
            this.jobPollTime = 10;
            this.mods = "";
            this.usePillar = false;
            this.pillarkey = "";
            this.pillarvalue = "";
        } else if (clientInterface.equals("runner")) {
            this.mods = clientInterfaces.get("mods").toString();
            if (clientInterfaces.has("usePillar")) {
                this.usePillar = true;
                this.pillarkey = clientInterfaces.getJSONObject("usePillar").get("pillarkey").toString();
                this.pillarvalue = clientInterfaces.getJSONObject("usePillar").get("pillarvalue")
                        .toString();
            } else {
                this.usePillar = false;
                this.pillarkey = "";
                this.pillarvalue = "";
            }
            this.blockbuild = false;
            this.jobPollTime = 10;
            this.batchSize = "100%";
        } else {
            this.batchSize = "100%";
            this.blockbuild = false;
            this.jobPollTime = 10;
            this.mods = "";
            this.usePillar = false;
            this.pillarkey = "";
            this.pillarvalue = "";
        }
    }

    /*
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getServername() {
        return servername;
    }

    public String getAuthtype() {
        return authtype;
    }

    public String getTarget() {
        return target;
    }

    public String getTargettype() {
        return this.targettype;
    }

    public String getFunction() {
        return function;
    }

    public String getArguments() {
        return arguments;
    }

    @DataBoundSetter
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getKwarguments() {
        return kwarguments;
    }

    @DataBoundSetter
    public void setKwarguments(String kwarguments) {
        this.kwarguments = kwarguments;
    }

    public String getClientInterface() {
        return clientInterface;
    }

    public Boolean getBlockbuild() {
        return blockbuild;
    }

    public String getBatchSize() {
        return batchSize;
    }

    public Integer getJobPollTime() {
        return jobPollTime;
    }

    public String getMods() {
        return mods;
    }

    public Boolean getUsePillar() {
        return usePillar;
    }

    public String getPillarkey() {
        return pillarkey;
    }

    public String getPillarvalue() {
        return pillarvalue;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setSaveEnvVar(Boolean saveEnvVar) {
        this.saveEnvVar = saveEnvVar;
    }

    public Boolean getSaveEnvVar() {
    	return saveEnvVar;
    }

    public JSONObject getClientInterfaces() {
    	return clientInterfaces;
    }

    @DataBoundSetter
    public void setClientInterfaces(JSONObject clientInterfaces) {
    	this.clientInterfaces = clientInterfaces;
    }


    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("SaltStack: Started from Pipeline");
        boolean success = perform(run, launcher, listener);
        if (!success) {
            throw new InterruptedException();
        }
    }

    //    @Override
    public boolean perform(Run build, Launcher launcher, TaskListener listener) {
        // This is where you 'build' the project.

        // If not not configured, grab the default
        int myJobPollTime = 10;
        if (jobPollTime == null) {
            myJobPollTime = getDescriptor().getPollTime();
        } else {
            myJobPollTime = jobPollTime;
        }

        String myOutputFormat = getDescriptor().getOutputFormat();
        String myClientInterface = clientInterface;
        String myservername = Utils.paramorize(build, listener, servername);
        String mytarget = Utils.paramorize(build, listener, target);
        String myfunction = Utils.paramorize(build, listener, function);
        String myarguments = Utils.paramorize(build, listener, arguments);
        String mykwarguments = Utils.paramorize(build, listener, kwarguments);
        Boolean myBlockBuild = blockbuild;

        StandardUsernamePasswordCredentials credential = getCredentialById(getCredentialsId());
        if (credential == null) {
            listener.error("Invalid credentials");
            return true;
        }

        // Setup connection for auth
        String token = new String();
        JSONArray authArray = createAuthArray(credential);
        // listener.getLogger().println("Sending auth: "+authArray.toString());
        JSONObject httpResponse = new JSONObject();
        JSONArray returnArray = new JSONArray();

        // Get an auth token
        token = Utils.getToken(myservername, authArray);
        if (token.contains("Error")) {
            listener.getLogger().println(token);
            return false;
        }
        // If we got this far auth must have been good and we've got a token

        // Hardcode clientInterface if not yet set. Once constructor runs, this
        // will not be necessary
        if (myClientInterface == null) {
            myClientInterface = "local";
        }
        if (saveEnvVar == null) {
            saveEnvVar = false;
        }

        JSONObject saltFunc = prepareSaltFunction(build, listener, myClientInterface, mytarget, myfunction, myarguments,
                mykwarguments);

        JSONArray saltArray = new JSONArray();
        saltArray.add(saltFunc);

        LOGGER.log(Level.FINE, "Sending JSON: " + saltArray.toString());
        

        if (myBlockBuild == null) {
            // Set a sane default if uninitialized
            myBlockBuild = false;
        }

        // blocking request
        if (myBlockBuild) {
            String jid = new String();
            // Send request to /minion url. This will give back a jid which we
            // will need to poll and lookup for completion
            httpResponse = Utils.getJSON(myservername + "/minions", saltArray, token);
            try {
                returnArray = httpResponse.getJSONArray("return");
                for (Object o : returnArray) {
                    JSONObject line = (JSONObject) o;
                    jid = line.getString("jid");
                }
                // Print out success
                listener.getLogger().println("Running jid: " + jid);
            } catch (Exception e) {
                listener.getLogger().println("Problem: " + myfunction + " " + myarguments + " to " + myservername
                        + " for " + mytarget + ":\n" + e + "\n\n" + httpResponse.toString(2));
                return false;
            }

            // Request successfully sent. Now use jid to check if job complete
            int numMinions = 0;
            int numMinionsDone = 0;
            httpResponse = Utils.getJSON(myservername + "/jobs/" + jid, null, token);
            try {
                // info array will tell us how many minions were targeted
                returnArray = httpResponse.getJSONArray("info");
                for (Object o : returnArray) {
                    JSONObject line = (JSONObject) o;
                    JSONArray minionsArray = line.getJSONArray("Minions");
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
                listener.getLogger().println("Problem: " + myfunction + " " + myarguments + " to " + myservername
                        + " for " + mytarget + ":\n" + e + "\n\n" + httpResponse.toString(2));
                return false;
            }

            // Now that we know how many minions have responded, and how many we
            // are waiting on. Let's see more have finished
            if (numMinionsDone < numMinions) {
                // Don't print annying messages unless we really are waiting for
                // more minions to return
                listener.getLogger()
                        .println("Will check status every " + String.valueOf(myJobPollTime) + " seconds...");
            }
            while (numMinionsDone < numMinions) {
                try {
                    Thread.sleep(myJobPollTime * 1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    // Allow user to cancel job in jenkins interface
                    listener.getLogger().println("Cancelling job");
                    return false;
                }
                httpResponse = Utils.getJSON(myservername + "/jobs/" + jid, null, token);
                try {
                    returnArray = httpResponse.getJSONArray("return");
                    numMinionsDone = returnArray.getJSONObject(0).names().size();
                } catch (Exception e) {
                    listener.getLogger().println("Problem: " + myfunction + " " + myarguments + " for " + mytarget
                            + ":\n" + e + "\n\n" + httpResponse.toString(2).split("\\\\n")[0]);
                    return false;
                }
            }
        } else {
            // Just send a salt request. Don't wait for reply
            httpResponse = Utils.getJSON(myservername, saltArray, token);
            try {
                returnArray = httpResponse.getJSONArray("return");
                if (!httpResponse.getJSONArray("return").isArray()) {
                    // Print problem
                    listener.getLogger().println("Problem on " + myfunction + " " + myarguments + " for " + mytarget
                            + ":\n" + httpResponse.toString(2));
                    return false;
                }
            } catch (Exception e) {
                listener.getLogger().println("Problem with " + myfunction + " " + myarguments + " for " + mytarget
                        + ":\n" + e + "\n\n" + httpResponse.toString(2).split("\\\\n")[0]);
                return false;
            }
        }
        // Done sending message. Check for error and print out results
        if (returnArray.get(0).toString().contains("TypeError")) {
            listener.getLogger().println("Salt reported an error for " + myfunction + " " + myarguments + " for "
                    + mytarget + ":\n" + returnArray.toString(2));
            return false;
        }

        LOGGER.log(Level.FINE, "Received response: " + returnArray);


        boolean validFunctionExecution = Utils.validateFunctionCall(returnArray);

        if (!validFunctionExecution) {
            listener.getLogger().println("ERROR occurred !\nERROR: One or more minion did not return code 0 for "
                    + myfunction + " " + myarguments + " for " + mytarget + ":\n" + returnArray.toString(2));
            // Save saltapi output to env if requested
            if (saveEnvVar) {
                build.addAction(new PublishEnvVarAction("SALTBUILDOUTPUT", returnArray.toString()));
            }
            return false;
        }

        // Loop is done. We have heard back from everybody. Good work team!
        listener.getLogger().println("Response on " + myfunction + " " + myarguments + " for " + mytarget + ":");
        if (myOutputFormat.equals("json")) {
            listener.getLogger().println(returnArray.toString(2));
        } else if (myOutputFormat.equals("yaml")) {
            Object outputObject = returnArray.toArray();
            Yaml yaml = new Yaml();
            listener.getLogger().println(yaml.dump(outputObject));
        } else {
            listener.getLogger().println("Error: Unknown output Format: x" + myOutputFormat + "x");
            return false;
        }

        // Save saltapi output to env if requested
        if (saveEnvVar) {
            build.addAction(new PublishEnvVarAction("SALTBUILDOUTPUT", returnArray.toString()));
        }

        // No fail condition reached. Must be good.
        return true;
    }

    private JSONArray createAuthArray(StandardUsernamePasswordCredentials credential) {
        JSONArray authArray = new JSONArray();
        JSONObject auth = new JSONObject();
        auth.put("username", credential.getUsername());
        auth.put("password", credential.getPassword().getPlainText());
        auth.put("eauth", authtype);
        authArray.add(auth);

        return authArray;
    }

    private JSONObject prepareSaltFunction(Run build, TaskListener listener, String myClientInterface,
                                           String mytarget, String myfunction, String myarguments, String mykwarguments) {
        JSONObject saltFunc = new JSONObject();
        saltFunc.put("client", myClientInterface);
        if (myClientInterface.equals("local_batch")) {
            saltFunc.put("batch", batchSize);
            listener.getLogger().println("Running in batch mode. Batch size: " + batchSize);
        }
        if (myClientInterface.equals("runner")) {
            saltFunc.put("mods", mods);

            if (usePillar) {
                String myPillarkey = Utils.paramorize(build, listener, pillarkey);
                String myPillarvalue = Utils.paramorize(build, listener, pillarvalue);

                JSONObject jPillar = new JSONObject();
                try {
                    // If value was already a jsonobject, treat it as such
                    JSON runPillarValue = JSONSerializer.toJSON(myPillarvalue);
                    jPillar.put(myPillarkey, runPillarValue);
                } catch (JSONException e) {
                    // Otherwise it must have been a string
                    jPillar.put(JSONUtils.stripQuotes(myPillarkey), JSONUtils.stripQuotes(myPillarvalue));
                }

                saltFunc.put("pillar", jPillar);
            }
        }
        saltFunc.put("tgt", mytarget);
        saltFunc.put("expr_form", targettype);
        saltFunc.put("fun", myfunction);
        addArgumentsToSaltFunction(myarguments, saltFunc);
        addKwArgumentsToSaltFunction(mykwarguments, saltFunc);

        return saltFunc;
    }

    private void addKwArgumentsToSaltFunction(String mykwarguments, JSONObject saltFunc) {
        if (mykwarguments.length() > 0) {
            Map<String, String> kwArgs = new HashMap<String, String>();
            // spit on comma seperated not inside of single and double quotes
            String[] kwargItems = mykwarguments.split(",(?=(?:[^'\"]|'[^']*'|\"[^\"]*\")*$)");
            for (String kwarg : kwargItems) {
                // remove spaces at begining or end
                kwarg = kwarg.replaceAll("^\\s+|\\s+$", "");
                kwarg = kwarg.replaceAll("\"|\\\"", "");
                if (kwarg.contains("=")) {
                    String[] kwString = kwarg.split("=");
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
                                kwFull += kwItem;
                                continue;
                            }
                            // add all other items with an = to rejoin
                            kwFull += "=" + kwItem;
                        }
                        kwArgs.put(kwString[0], kwFull);
                    } else {
                        kwArgs.put(kwString[0], kwString[1]);
                    }
                }
            }
            // Add any kwargs to json message
            saltFunc.element("kwarg", kwArgs);
        }
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
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider
                .lookupCredentials(StandardUsernamePasswordCredentials.class, context, ACL.SYSTEM, requirements);
        return credentials;
    }

    private void addArgumentsToSaltFunction(String myarguments, JSONObject saltFunc) {
        if (myarguments.length() > 0) {
            List<String> saltArguments = new ArrayList<String>();
            // spit on comma seperated not inside of single and double quotes
            String[] argItems = myarguments.split(",(?=(?:[^'\"]|'[^']*'|\"[^\"]*\")*$)");

            for (String arg : argItems) {
                // remove spaces at begining or end
                arg = arg.replaceAll("^\\s+|\\s+$", "");
                // if string wrapped in quotes, remove them since adding to list
                // re-quotes
                arg = arg.replaceAll("(^')|(^\")|('$)|(\"$)", "");
                saltArguments.add(arg);
            }

            // Add any args to json message
            saltFunc.element("arg", saltArguments);
        }
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

        private int pollTime = 10;
        private String outputFormat = "json";

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            try {
                // Test that value entered in config is an integer
                pollTime = formData.getInt("pollTime");
            } catch (Exception e) {
                // Fall back to default
                pollTime = 10;
            }
            outputFormat = formData.getString("outputFormat");
            save();
            return super.configure(req, formData);
        }

        public int getPollTime() {
            return pollTime;
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
                JSONArray authArray = new JSONArray();
                JSONObject auth = new JSONObject();
                auth.put("username", usedCredential.getUsername());
                auth.put("password", usedCredential.getPassword().getPlainText());
                auth.put("eauth", authtype);
                authArray.add(auth);
                String token = Utils.getToken(servername, authArray);
                if (token.contains("Error")) {
                    return FormValidation.error("Client error: " + token);
                }

                return FormValidation.ok("Success");
            }

            return FormValidation.warning("Cannot expand parametrized server name.");
        }

        public StandardListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context,
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

        public FormValidation doCheckTarget(@QueryParameter String value) {
            return validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }

        public FormValidation doCheckFunction(@QueryParameter String value) {
            return validateFormStringField(value, "Please specify a salt function", "Isn't it too short?");
        }

        private FormValidation validateFormStringField(String value, String lackOfFieldMessage,
                String fieldToShortMessage) {
            if (value.length() == 0) {
                return FormValidation.error(lackOfFieldMessage);
            }

            if (value.length() < 3) {
                return FormValidation.warning(fieldToShortMessage);
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

        public FormValidation doCheckBatchSize(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please specify batch size");
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
