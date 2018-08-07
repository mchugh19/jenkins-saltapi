package com.waytta;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.waytta.clientinterface.BasicClient;
import com.waytta.clientinterface.LocalBatchClient;
import com.waytta.clientinterface.LocalClient;
import com.waytta.clientinterface.RunnerClient;
import org.yaml.snakeyaml.Yaml;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.Tasks;
import hudson.model.Job;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.security.MasterToSlaveCallable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.interceptor.RequirePOST;

import jenkins.model.Jenkins;
import java.util.Collections;

import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

public class SaltAPIBuilder extends Builder implements SimpleBuildStep, Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    protected Object readResolve() throws IOException {
        // Support 1.7 and before
        if (clientInterfaces != null) {
            arguments = arguments.replaceAll(",", " ");

            if (clientInterfaces.has("clientInterface")) {
                if (clientInterfaces.getString("clientInterface").equals("local")) {
                    clientInterface = new LocalClient(function, arguments + " " + kwarguments, target, targettype);
                    ((LocalClient) clientInterface).setJobPollTime(clientInterfaces.getInt("jobPollTime"));
                    ((LocalClient) clientInterface).setBlockbuild(clientInterfaces.getBoolean("blockbuild"));
                } else if (clientInterfaces.getString("clientInterface").equals("local_batch")) {
                    clientInterface = new LocalBatchClient(function, arguments + " " + kwarguments, batchSize, target, targettype);
                } else if (clientInterfaces.getString("clientInterface").equals("runner")) {
                    clientInterface = new RunnerClient(function, arguments + " " + kwarguments, mods, pillarvalue);
                }
            }
            clientInterfaces = null;
        }
        return this;
    }


    private static final Logger LOGGER = Logger.getLogger("com.waytta.saltstack");

    private String servername;
    private String authtype;
    private BasicClient clientInterface;
    private boolean saveEnvVar = false;
    private final String credentialsId;
    private boolean saveFile = false;
    private boolean skipValidation = false;

    @Deprecated
    private transient JSONObject clientInterfaces;
    @Deprecated
    private transient String target;
    @Deprecated
    private transient String targettype;
    @Deprecated
    private transient String function;
    @Deprecated
    private transient String arguments;
    @Deprecated
    private transient String kwarguments;
    @Deprecated
    private transient String batchSize;
    @Deprecated
    private transient String mods;
    @Deprecated
    private transient String pillarvalue;
    @Deprecated
    private transient Boolean blockbuild;
    @Deprecated
    private transient Integer jobPollTime;
    @Deprecated
    private transient Boolean usePillar;
    @Deprecated
    private transient String pillarkey;


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

    @DataBoundSetter
    public void setServername(String servername) {
        this.servername = servername;
    }

    public String getAuthtype() {
        return authtype;
    }

    public String getTarget() {
        return clientInterface.getTarget();
    }

    public String getTargettype() {
        return clientInterface.getTargettype();
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

    public int getMinionTimeout() {
        return clientInterface.getMinionTimeout();
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

    @DataBoundSetter
    public void setSaveFile(boolean saveFile) {
        this.saveFile = saveFile;
    }

    public boolean getSaveFile() {
        return saveFile;
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
    
    @DataBoundSetter
    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }
    
    public boolean getSkipValidation() {
        return skipValidation;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        String myOutputFormat = getDescriptor().getOutputFormat();
        String myApiVersion = getDescriptor().getApiVersion();
        String myClientInterface = clientInterface.getDescriptor().getDisplayName();
        String myservername = Utils.paramorize(build, listener, servername);
        String mytarget = Utils.paramorize(build, listener, getTarget());
        String myfunction = Utils.paramorize(build, listener, getFunction());
        String myarguments = Utils.paramorize(build, listener, getArguments());

        boolean jobSuccess = true;

        StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(
                getCredentialsId(), StandardUsernamePasswordCredentials.class, build);

        if (credential == null) {
            listener.error("Invalid credentials");
            throw new RuntimeException("Invalid credentials");
        }

        // Setup connection for auth
        JSONObject auth = Utils.createAuthArray(credential, authtype);

        // Get an auth token
        ServerToken serverToken = Utils.getToken(launcher, myservername, auth);
        String token = serverToken.getToken();
        String netapi = serverToken.getServer();
        LOGGER.log(Level.FINE, "Discovered netapi: " + netapi);

        // If we got this far, auth must have been good and we've got a token
        JSONObject saltFunc = prepareSaltFunction(build, listener, myClientInterface, mytarget, myfunction, myarguments, myApiVersion, getTargettype());
        LOGGER.log(Level.FINE, "Sending JSON: " + saltFunc.toString());

        JSONArray returnArray;
        try {
            String jid = getJID(launcher, myservername, token, saltFunc, listener);
            returnArray = performRequest(launcher, build, token, myservername, saltFunc, listener, netapi, jid);
        } catch (SaltException e) {
            throw new RuntimeException(e);
        }
        LOGGER.log(Level.FINE, "Received response: " + returnArray);

        // Save saltapi output to env if requested
        if (saveEnvVar) {
            build.addAction(new PublishEnvVarAction("SALTBUILDOUTPUT", returnArray.toString()));
        }

        if (saveFile) {
            Utils.writeFile(returnArray.toString(), workspace);
        }

        boolean validFunctionExecution = true;
        if (!skipValidation) {
            // Check for error and print out results
            validFunctionExecution = Utils.validateFunctionCall(returnArray);
        } else {
            LOGGER.log(Level.INFO, "Skipping validation of salt return");
        }

        if (!validFunctionExecution) {
            listener.error("One or more minion did not return code 0\n");
            jobSuccess = false;
        }

        // Just finish up if we don't output
        if (myOutputFormat.equals("none")) {
            listener.getLogger().println("Completed " + myfunction + " " + myarguments);
            return;
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
            throw new RuntimeException("Unknown output format");
        }

        // Results now printed. Return success condition
        if (!jobSuccess) {
            throw new RuntimeException("Salt failure detected");
        }
    }

    public String getJID(Launcher launcher, String serverName, String token, JSONObject saltFunc, TaskListener listener) throws IOException, InterruptedException, SaltException {
        if (saltFunc.has("client")) {
            if (((String) saltFunc.get("client")).contains("async")) {
                return Builds.getBlockingBuildJid(launcher, serverName, token, saltFunc, listener);
            }
        }
        return null;
    }

    public JSONArray performRequest(Launcher launcher, Run build, String token, String serverName, JSONObject saltFunc, TaskListener listener, String netapi, String jid)
            throws InterruptedException, IOException, SaltException {
        JSONArray returnArray = new JSONArray();
        JSONObject httpResponse = new JSONObject();
        // Access different salt-api endpoints depending on function
        if (!saltFunc.has("client")) {
            // only hook communications should start with an empty function object
            // publish event to salt event bus to /hook
            String myTag = Utils.paramorize(build, listener, getTag());
            // Cleanup myTag to remove duplicate / and urlencode
            myTag = myTag.replaceAll("^/", "");
            myTag = URLEncoder.encode(myTag, "UTF-8");
            httpResponse = (JSONObject) JSONSerializer.toJSON(launcher.getChannel().call(new HttpCallable(serverName + "/hook/" + myTag, saltFunc, token)));
            returnArray.add(httpResponse);
        } else if (saltFunc.get("client").equals("local_async")) {
            int jobPollTime = getJobPollTime();
            int minionTimeout = getMinionTimeout();
            // poll /minion for response
            returnArray = Builds.checkBlockingBuild(launcher, serverName, token, saltFunc, listener, jobPollTime, minionTimeout, netapi, jid);
        } else {
            // Just send a salt request to /. Don't wait for reply
            httpResponse = (JSONObject) JSONSerializer.toJSON(launcher.getChannel().call(new HttpCallable(serverName, saltFunc, token)));
            returnArray = httpResponse.getJSONArray("return");
        }

        return returnArray;
    }

    public JSONObject prepareSaltFunction(Run build, TaskListener listener, String myClientInterface, String mytarget,
            String myfunction, String myarguments, String myApiVersion, String targetType) throws IOException, InterruptedException {
        JSONObject saltFunc = new JSONObject();
        saltFunc.put("client", myClientInterface);
        String tgt_key = "tgt_type";

        switch (myApiVersion) {
        case "2017.7":
            if (myfunction.startsWith("cmd.") || myfunction.startsWith("cmd_sync.")) {
                // cmd.run and cmd_sync functions have expanded output as of 2017.7
                saltFunc.put("full_return", true);
            }
            break;
        case "older":
            tgt_key = "expr_form";
            break;
        }

        switch (myClientInterface) {
        case "local":
            saltFunc.put("tgt", mytarget);
            saltFunc.put(tgt_key, targetType);
            if (getBlockbuild()) {
                // when sending to the /minion endpoint, use local_async instead of just local
                saltFunc.element("client", "local_async");
            }
            break;
        case "local_batch":
            saltFunc.put("tgt", mytarget);
            saltFunc.put(tgt_key, targetType);
            String mybatch = Utils.paramorize(build, listener, getBatchSize());
            saltFunc.put("batch", mybatch);
            listener.getLogger().println("Running in batch mode. Batch size: " + mybatch);
            break;
        case "runner":
            String myMods = getMods();
            if(myMods != null && !myMods.trim().isEmpty()) {
                saltFunc.put("mods", myMods);
            }
            String myPillarvalue = Utils.paramorize(build, listener, getPillarvalue());
            if (myPillarvalue.length() > 0) {
                // If value was already a jsonobject, treat it as such
                JSON runPillarValue = JSONSerializer.toJSON(myPillarvalue);
                saltFunc.put("pillar", runPillarValue);
            }
            break;
        case "local_subset":
            saltFunc.put("tgt", mytarget);
            saltFunc.put(tgt_key, targetType);
            String mySubset = Utils.paramorize(build, listener, getSubset());
            saltFunc.put("sub", Integer.parseInt(mySubset));
            listener.getLogger().println("Running in subset mode. Subset size: " + mySubset);
            break;
        case "hook":
            // Posting to /hook url should only contain object to be posted
            String myPost = Utils.paramorize(build, listener, getPost());
            if (myPost.length() == 0) {
                saltFunc = JSONObject.fromObject("{}");
            } else {
                saltFunc = JSONObject.fromObject(myPost);
            }
            return saltFunc;
        }

        saltFunc.put("fun", myfunction);
        if (myarguments != null) {
            Builds.addArgumentsToSaltFunction(myarguments, saltFunc);
        }

        return saltFunc;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        int pollTime = 10;
        int minionTimeout = 30;
        String outputFormat = "json";
        String apiVersion = "2017.7";

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            try {
                // Test that value entered in config is an integer
                pollTime = formData.getInt("pollTime");
                minionTimeout = formData.getInt("minionTimeout");
            } catch (Exception e) {
                // Fall back to default
                pollTime = 10;
                minionTimeout = 30;
            }
            outputFormat = formData.getString("outputFormat");
            apiVersion = formData.getString("apiVersion");
            save();
            return super.configure(req, formData);
        }

        public int getPollTime() {
            return pollTime;
        }

        public int getMinionTimeout() {
            return minionTimeout;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        @RequirePOST
        public static FormValidation doTestConnection(
                @QueryParameter String servername,
                @QueryParameter String credentialsId,
                @QueryParameter String authtype,
                @AncestorInPath Item project) {
            project.checkPermission(Item.CONFIGURE);
            StandardUsernamePasswordCredentials usedCredential = null;
            for (StandardUsernamePasswordCredentials c : CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    null,
                    Collections.<DomainRequirement>emptyList())) {
                if (c.getId().equals(credentialsId)) {
                    usedCredential = c;
                    break;
                }
            }

            if (usedCredential == null) {
                return FormValidation.error("CredentialId error: no credential found with given ID.");
            }

            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                throw new IllegalStateException("Jenkins has not been started, or was already shut down");
            }
            Launcher launcher = jenkins.createLauncher(TaskListener.NULL);

            if (!servername.matches("\\{\\{\\w+\\}\\}")) {
                JSONObject auth = Utils.createAuthArray(usedCredential, authtype);
                try {
                    String token = Utils.getToken(launcher, servername, auth).getToken();
                    if (token.contains("Error")) {
                        return FormValidation.error("Client error: " + token);
                    }
                } catch (InterruptedException|IOException e) {
                    return FormValidation.error("Error: Exception running http request");
                }

                return FormValidation.ok("Success");
            }

            return FormValidation.warning("Cannot expand parametrized server name.");
        }
        
        @RequirePOST
        public static ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Job context,
                @QueryParameter String credentialsId,
                @QueryParameter final String servername,
                @AncestorInPath Item project) {
            project.checkPermission(Item.CONFIGURE);
            Item item = Stapler.getCurrentRequest().findAncestorObject(Item.class);
            return new StandardUsernameListBoxModel()
                    .includeAs(
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                                    item,
                                    StandardUsernamePasswordCredentials.class,
                                    Collections.<DomainRequirement>emptyList()
                            );
        }

        public static FormValidation doCheckServername(@QueryParameter String value) {
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

        @RequirePOST
        public static FormValidation doCheckCredentialsId(
                @AncestorInPath Item project,
                @QueryParameter String value) {
            project.checkPermission(Item.CONFIGURE);
            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }

            if (value == null) {
                return FormValidation.ok();
            }

            Item item = Stapler.getCurrentRequest().findAncestorObject(Item.class);
            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList(),
                            CredentialsMatchers
                            .instanceOf(StandardUsernamePasswordCredentials.class))) {
                if (value.equals(o.value)) {
                    return FormValidation.ok();
                }
            }
            return FormValidation.error("Cannot find any credentials with id " + value);
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

        public FormValidation doCheckMinionTimeout(@QueryParameter String value) {
            String errorText = "Specify a non zero number. Positive numbers fail the build when "
                    + "reached, negative numbers will timeout minions without failing";
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error(errorText);
            }
            if (Integer.parseInt(value) == 0) {
                return FormValidation.warning(errorText);
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
