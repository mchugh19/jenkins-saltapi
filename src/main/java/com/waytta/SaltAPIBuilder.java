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

import java.net.*;
import net.sf.json.JSONArray;
import net.sf.json.util.JSONUtils;
import net.sf.json.JSONSerializer;
import hudson.EnvVars;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;

import javax.servlet.ServletException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link SaltAPIBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class SaltAPIBuilder extends Builder {

    private final String servername;
    private final String username;
    private final String userpass;
    private final String authtype;
    private final String target;
    private final String targettype;
    private final String function;
    private final String arguments;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public SaltAPIBuilder(String servername, String username, String userpass, String authtype, String target, String targettype, String function, String arguments) {
        this.servername = servername;
        this.username = username;
        this.userpass = userpass;
        this.authtype = authtype;
        this.target = target;
        this.targettype = targettype;
        this.function = function;
        this.arguments = arguments;
    }

    //Thinger to connect to saltmaster over rest interface
    public static String sendJSON(String targetURL, String urlParams, String auth) {
      HttpURLConnection connection = null;  
      String serverUrl = new String();
      if (auth != null && !auth.isEmpty()){
        serverUrl = targetURL;
      } else {
        serverUrl = targetURL+"/login";
      }
      try {
        //Create connection
        URL url = new URL(serverUrl);
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000); //set timeout to 5 seconds
        if (auth != null && !auth.isEmpty()){
          connection.setRequestProperty("X-Auth-Token", auth);
        }
  
        //Send request
        DataOutputStream wr = new DataOutputStream ( connection.getOutputStream());
        wr.writeBytes(urlParams);
        wr.flush ();
        wr.close ();
  
        //Get Response	
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer(); 
        while((line = rd.readLine()) != null) {
          response.append(line);
          response.append('\r');
        }
        rd.close();
        return response.toString();
      } catch (Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        //return null;
        return errors.toString();
      } finally {
        if(connection != null) {
          connection.disconnect(); 
        }
      }
    }

    //replaces $string with value of env($string). Used in conjunction with parameterized builds
    public String paramorize(AbstractBuild build, BuildListener listener, String paramer) {
      Pattern pattern = Pattern.compile("\\{\\{\\w+\\}\\}");
      Matcher matcher = pattern.matcher(paramer);
      while (matcher.find()) {
        //listener.getLogger().println("FOUND: "+matcher.group());
        try {
          EnvVars envVars;
          envVars = build.getEnvironment(listener);
          //remove leading {{
          String replacementVar = matcher.group().substring(2);
          //remove trailing }} 
          replacementVar = replacementVar.substring(0, replacementVar.length()-2);
          //using proper env var name, perform a lookup and save value
          replacementVar = envVars.get(replacementVar);
          paramer = paramer.replace(matcher.group(), replacementVar);
        } catch (IOException e1) {
          listener.getLogger().println(e1);
          return "Error: "+e1;
        } catch (InterruptedException e1) {
          listener.getLogger().println(e1);
          return "Error: "+e1;
        }
      }
      return paramer;
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

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        String mytarget = target;
        String myfunction = function;
        String myarguments = arguments;
        //listener.getLogger().println("Salt Arguments before: "+myarguments);
        mytarget = paramorize(build, listener, target);
        myfunction = paramorize(build, listener, function);
        myarguments = paramorize(build, listener, arguments);
        //listener.getLogger().println("Salt Arguments after: "+myarguments);

        //Setup connection for auth
        String auth = "username="+username+"&password="+userpass+"&eauth="+authtype;
        String httpResponse = new String();
        httpResponse = sendJSON(servername, auth, null);
        if (httpResponse.contains("java.io.IOException") || httpResponse.contains("java.net.SocketTimeoutException")) {
          listener.getLogger().println("Error: "+httpResponse);
          return false;
        }
        try {
          JSONObject authresp = (JSONObject) JSONSerializer.toJSON(httpResponse);
          JSONArray returnArray = authresp.getJSONArray("return");
          String token = new String();
          for (Object o : returnArray ) {
            JSONObject line = (JSONObject) o;
            token = line.getString("token");
          }
        } catch (Exception e) {
          listener.getLogger().println("JSON Error: "e+"\n\n"+httpResponse);
          return false;
        }

        //If we got this far, auth must have been pretty good and we've got a token
        String saltFunc = new String();
        if (myarguments.length() > 0){ 
          saltFunc = "client=local&tgt="+mytarget+"&expr_form="+targettype+"&fun="+myfunction+"&arg="+myarguments;
        } else {
          saltFunc = "client=local&tgt="+mytarget+"&expr_form="+targettype+"&fun="+myfunction;
        }
        httpResponse = sendJSON(servername, saltFunc, token);
        if (httpResponse.contains("java.io.IOException") || 
            httpResponse.contains("java.net.SocketTimeoutException") || 
            httpResponse.contains("TypeError")  
           ) {
          listener.getLogger().println("Error: "+myfunction+" "+myarguments+" to "+servername+" for "+mytarget+":\n"+httpResponse);
          return false;
        }
        try {
          JSONObject jsonResp = (JSONObject) JSONSerializer.toJSON(httpResponse);
          //Print out success
          listener.getLogger().println("Response on "+myfunction+" "+myarguments+" for "+mytarget+":\n"+jsonResp.toString(2));
        } catch (Exception e) {
          listener.getLogger().println("Problem: "+myfunction+" "+myarguments+" to "+servername+" for "+mytarget+":\n"+e+"\n\n"+httpResponse);
          return false;
        }
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link SaltAPIBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

         public FormValidation doTestConnection(@QueryParameter("servername") final String servername, 
					   @QueryParameter("username") final String username, 
					   @QueryParameter("userpass") final String userpass, 
					   @QueryParameter("authtype") final String authtype) 
					   throws IOException, ServletException {
           String httpResponse = new String();
           String auth = "username="+username+"&password="+userpass+"&eauth="+authtype;
           try {
             httpResponse = sendJSON(servername, auth, null);
             if (httpResponse.contains("java.io.IOException") ) {
               return FormValidation.error("Connection error: "+httpResponse);
             } else if ( httpResponse.contains("java.net.SocketTimeoutException")) {
               return FormValidation.error("Connection error: "+servername+" timed out");
             }
             JSONObject authresp = (JSONObject) JSONSerializer.toJSON(httpResponse);
             JSONArray returnArray = authresp.getJSONArray("return");
             //print response from salt api
             String token = new String();
             for (Object o : returnArray ) {
               JSONObject line = (JSONObject) o;
               token = line.getString("token");
             }
             return FormValidation.ok("Success");
           } catch (Exception e) {
             return FormValidation.error("Client error : "+e.getMessage()+" "+httpResponse);
           }
         }



        /**
         * Performs on-the-fly validation of the form fields
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
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

