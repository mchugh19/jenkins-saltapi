package com.waytta;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import net.sf.json.JSONArray;
import net.sf.json.util.JSONUtils;
import net.sf.json.JSONSerializer;
import java.io.*;
import java.util.*;

import javax.servlet.ServletException;




public class SaltAPIBuilder extends Builder {

    private final String servername;
    private final String username;
    private final String userpass;
    private final String authtype;
    private final String target;
    private final String targettype;
    private final String function;
    private final String arguments;
    private final JSONObject clientInterfaces;
    private final String clientInterface;
    private final Boolean blockbuild;
    private final Integer jobPollTime;
    private final String batchSize;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
	public SaltAPIBuilder(String servername, String username, String userpass, String authtype, String target, String targettype, String function, String arguments, JSONObject clientInterfaces, Integer jobPollTime) {
	    this.servername = servername;
	    this.username = username;
	    this.userpass = userpass;
	    this.authtype = authtype;
	    this.target = target;
	    this.targettype = targettype;
	    this.function = function;
	    this.arguments = arguments;
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
	    } else if (clientInterface.equals("local_batch")) {
		this.batchSize = clientInterfaces.get("batchSize").toString();
		this.blockbuild = false;
		this.jobPollTime = 10;
	    } else {
		this.batchSize = "100%";
		this.blockbuild = false;
		this.jobPollTime = 10;
            }
	}

    /*
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getServername() {
	return servername;
    }
    public String getUsername() {
	return username;
    }
    public String getUserpass() {
	return userpass;
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

    @Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
	    // This is where you 'build' the project.
	    String myClientInterface = clientInterface;
	    String mytarget = target;
	    String myfunction = function;
	    String myarguments = arguments;
	    //listener.getLogger().println("Salt Arguments before: "+myarguments);
	    mytarget = Utils.paramorize(build, listener, target);
	    myfunction = Utils.paramorize(build, listener, function);
	    myarguments = Utils.paramorize(build, listener, arguments);
	    //listener.getLogger().println("Salt Arguments after: "+myarguments);

	    //Setup connection for auth
	    String token = new String();
	    JSONObject auth = new JSONObject();
	    auth.put("username", username);
	    auth.put("password", userpass);
	    auth.put("eauth", authtype);
	    JSONArray authArray = new JSONArray();
	    authArray.add(auth);
	    JSONObject httpResponse = new JSONObject();

	    //Get an auth token
	    token = Utils.getToken(servername, authArray);
	    if (token.contains("Error")) {
		listener.getLogger().println(token);
		return false;
	    }

	    //If we got this far, auth must have been pretty good and we've got a token
	    JSONArray saltArray = new JSONArray();
	    JSONObject saltFunc = new JSONObject();
	    // Hardcode clientInterface if not yet set. Once constructor runs, this will not be necessary
	    if (myClientInterface == null) {
		myClientInterface = "local";
	    }

	    saltFunc.put("client", myClientInterface);
	    if (myClientInterface.equals("local_batch")) {
		saltFunc.put("batch", batchSize);
		listener.getLogger().println("Running in batch mode. Batch size: "+batchSize);
	    }
	    saltFunc.put("tgt", mytarget); 
	    saltFunc.put("expr_form", targettype);
	    saltFunc.put("fun", myfunction);
	    if (myarguments.length() > 0){ 
		List saltArguments = new ArrayList();
		Map kwArgs = new HashMap();
		String[] argItems = myarguments.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		for (String arg : argItems) {
		    arg = arg.replaceAll("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)", "");
		    arg = arg.replaceAll("\"|\\\"", "");
		    if (arg.contains("=")) {
			String[] kwString = arg.split("=");
			kwArgs.put(kwString[0], kwString[1]);
		    } else {
			saltArguments.add(arg);
		    }
		}
		saltFunc.element("arg", saltArguments);
		saltFunc.element("kwarg", kwArgs);
		saltArray.add(saltFunc);
	    }
	    listener.getLogger().println("Sending JSON: "+saltArray.toString());

	    Boolean myBlockBuild = blockbuild;
	    if (myBlockBuild == null) {
		//Set a sane default if uninitialized
		myBlockBuild = false;
	    }

	    //blocking request
	    if (myBlockBuild) {
		String jid = new String();
		//Send request to /minion url. This will give back a jid which we will need to poll and lookup for completion
		httpResponse = Utils.getJSON(servername+"/minions", saltArray, token);
		try {
		    JSONArray returnArray = httpResponse.getJSONArray("return");
		    for (Object o : returnArray ) {
			JSONObject line = (JSONObject) o;
			jid = line.getString("jid");
		    }
		    //Print out success
		    listener.getLogger().println("Running jid: " + jid);
		} catch (Exception e) {
		    listener.getLogger().println("Problem: "+myfunction+" "+myarguments+" to "+servername+" for "+mytarget+":\n"+e+"\n\n"+httpResponse.toString(2));
		    return false;
		}

		//Request successfully sent. Now use jid to check if job complete
		int numMinions = 0;
		int numMinionsDone = 0;
		JSONArray returnArray = new JSONArray();
		httpResponse = Utils.getJSON(servername+"/jobs/"+jid, null, token);
		try {
		    //info array will tell us how many minions were targeted
		    returnArray = httpResponse.getJSONArray("info");
		    for (Object o : returnArray ) {
			JSONObject line = (JSONObject) o;
			JSONArray minionsArray = line.getJSONArray("Minions");
			//Check the info[Minions[]] array to see how many nodes we expect to hear back from
			numMinions = minionsArray.size();
			listener.getLogger().println("Waiting for " + numMinions + " minions");
		    }
		    returnArray = httpResponse.getJSONArray("return");
		    //Check the return[] array to see how many minions have responded
		    if (!returnArray.getJSONObject(0).names().isEmpty()) {
			numMinionsDone = returnArray.getJSONObject(0).names().size();
		    } else {
			numMinionsDone = 0;
		    }
		    listener.getLogger().println(numMinionsDone + " minions are done");
		} catch (Exception e) {
		    listener.getLogger().println("Problem: "+myfunction+" "+myarguments+" to "+servername+" for "+mytarget+":\n"+e+"\n\n"+httpResponse.toString(2));
		    return false;
		}

		// If not not configured, grab the default
		int myJobPollTime = 10;
		if (jobPollTime == null) {
		    myJobPollTime = getDescriptor().getPollTime();
		} else {
		    myJobPollTime = jobPollTime;
		}

		//Now that we know how many minions have responded, and how many we are waiting on. Let's see more have finished
		if (numMinionsDone < numMinions) {
		    //Don't print annying messages unless we really are waiting for more minions to return
		    listener.getLogger().println("Will check status every "+String.valueOf(myJobPollTime)+" seconds...");
		}
		while (numMinionsDone < numMinions) {
		    try {
			Thread.sleep(myJobPollTime*1000);
		    } catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			//Allow user to cancel job in jenkins interface
			listener.getLogger().println("Cancelling job");
			return false;
		    }
		    httpResponse = Utils.getJSON(servername+"/jobs/"+jid, null, token);
		    try {
			returnArray = httpResponse.getJSONArray("return");
			numMinionsDone = returnArray.getJSONObject(0).names().size();
			if (myClientInterface.equals("local_batch")) {
			    listener.getLogger().println("Minions finished: " + numMinionsDone);
			    listener.getLogger().println(returnArray.toString(2));
			}
		    } catch (Exception e) {
			listener.getLogger().println("Problem: "+myfunction+" "+myarguments+" for "+mytarget+":\n"+e+"\n\n"+httpResponse.toString(2).split("\\\\n")[0]);
			return false;
		    }
		}
		if (returnArray.get(0).toString().contains("TypeError")) {
		    listener.getLogger().println("Salt reported an error for "+myfunction+" "+myarguments+" for "+mytarget+":\n"+returnArray.toString(2));
		    return false;
		}
		//Loop is done. We have heard back from everybody. Good work team!
		listener.getLogger().println("Response on "+myfunction+" "+myarguments+" for "+mytarget+":\n"+returnArray.toString(2));
	    } else {
		//Just send a salt request. Don't wait for reply
		httpResponse = Utils.getJSON(servername, saltArray, token);
		try {
		    if (!httpResponse.getJSONArray("return").isArray()) {
			//Print problem
			listener.getLogger().println("Problem on "+myfunction+" "+myarguments+" for "+mytarget+":\n"+httpResponse.toString(2));
			return false;
		    }
		} catch (Exception e) {
		    listener.getLogger().println("Problem with "+myfunction+" "+myarguments+" for "+mytarget+":\n"+e+"\n\n"+httpResponse.toString(2).split("\\\\n")[0]);
		    return false;
		}
		if (httpResponse.toString().contains("TypeError")) {
		    listener.getLogger().println("Salt reported an error on "+myfunction+" "+myarguments+" for "+mytarget+":\n"+httpResponse.toString(2));
		    return false;
		} else {
		    listener.getLogger().println("Response on "+myfunction+" "+myarguments+" for "+mytarget+":\n"+httpResponse.toString(2));
		}
	    }
	    //No fail condition reached. Must be good.
	    return true;
	}

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
	public DescriptorImpl getDescriptor() {
	    return (DescriptorImpl)super.getDescriptor();
	}

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

	    private int pollTime=10;

	    public DescriptorImpl() {
		load();
	    }

	    @Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
		    try {
			//Test that value entered in config is an integer
			pollTime = formData.getInt("pollTime");
		    } catch (Exception e) {
			//Fall back to default
			pollTime = 10;
		    }
		    save();
		    return super.configure(req,formData);
		}

	    public int getPollTime() {
		return pollTime;
	    }

	    public FormValidation doTestConnection(@QueryParameter("servername") final String servername, 
		    @QueryParameter("username") final String username, 
		    @QueryParameter("userpass") final String userpass, 
		    @QueryParameter("authtype") final String authtype) 
		throws IOException, ServletException {
		    JSONObject httpResponse = new JSONObject();
		    JSONArray authArray = new JSONArray();
		    JSONObject auth = new JSONObject();
		    auth.put("username", username);
		    auth.put("password", userpass);
		    auth.put("eauth", authtype);
		    authArray.add(auth);
		    String token = Utils.getToken(servername, authArray);
		    if (token.contains("Error")) {
			return FormValidation.error("Client error : "+token);
		    } else {
			return FormValidation.ok("Success");
		    }
		}


	    public FormValidation doCheckServername(@QueryParameter String value)
		throws IOException, ServletException {
		    if (value.length() == 0)
			return FormValidation.error("Please specify a name");
		    if (value.length() < 10)
			return FormValidation.warning("Isn't the name too short?");
		    if (!value.contains("https://") && !value.contains("http://"))
			return FormValidation.warning("Missing protocol: Servername should be in the format https://host.domain:8000");
		    if (!value.substring(7).contains(":"))
			return FormValidation.warning("Missing port: Servername should be in the format https://host.domain:8000");
		    return FormValidation.ok();
		}

	    public FormValidation doCheckUsername(@QueryParameter String value)
		throws IOException, ServletException {
		    if (value.length() == 0)
			return FormValidation.error("Please specify a name");
		    if (value.length() < 3)
			return FormValidation.warning("Isn't the name too short?");
		    return FormValidation.ok();
		}

	    public FormValidation doCheckUserpass(@QueryParameter String value)
		throws IOException, ServletException {
		    if (value.length() == 0)
			return FormValidation.error("Please specify a password");
		    if (value.length() < 3)
			return FormValidation.warning("Isn't it too short?");
		    return FormValidation.ok();
		}

	    public FormValidation doCheckTarget(@QueryParameter String value)
		throws IOException, ServletException {
		    if (value.length() == 0)
			return FormValidation.error("Please specify a salt target");
		    if (value.length() < 3)
			return FormValidation.warning("Isn't it too short?");
		    return FormValidation.ok();
		}

	    public FormValidation doCheckFunction(@QueryParameter String value)
		throws IOException, ServletException {
		    if (value.length() == 0)
			return FormValidation.error("Please specify a salt function");
		    if (value.length() < 3)
			return FormValidation.warning("Isn't it too short?");
		    return FormValidation.ok();
		}

	    public FormValidation doCheckPollTime(@QueryParameter String value)
		throws IOException, ServletException {
		    try {
			Integer.parseInt(value);
		    } catch(NumberFormatException e) {
			return FormValidation.error("Specify a number larger than 3");
		    }
		    if (Integer.parseInt(value) < 3)
			return FormValidation.warning("Specify a number larger than 3");
		    return FormValidation.ok();
		}

	    public FormValidation doCheckBatchSize(@QueryParameter String value)
		throws IOException, ServletException {
		    if (value.length() == 0)
			return FormValidation.error("Please specify batch size");
		    return FormValidation.ok();
		}

	    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
		// Indicates that this builder can be used with all kinds of project types 
		return true;
	    }

	    /**
	     * This human readable name is used in the configuration screen.
	     */
	    public String getDisplayName() {
		return "Send a message to Salt API";
	    }
	}
}

