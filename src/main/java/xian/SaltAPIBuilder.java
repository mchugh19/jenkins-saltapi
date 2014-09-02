package xian;
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
import net.sf.json.JSONSerializer;

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

    public static String sendJSON(String targetURL, String urlParams, String auth) {
      HttpURLConnection connection = null;  
      String myauth = new String();
      myauth = auth;
      String myurl = new String();
      if (myauth != null && !myauth.isEmpty()){
        myurl = targetURL;
      } else {
        myurl = targetURL+"/login";
      }
      try {
        //Create connection
        URL url = new URL(myurl);
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        //connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        if (myauth != null && !myauth.isEmpty()){
          connection.setRequestProperty("X-Auth-Token", myauth);
        }
			  
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000); //set timeout to 5 seconds
  
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

        /*
        listener.getLogger().println("Salt Server: "+servername);
        listener.getLogger().println("Salt User: "+username);
        listener.getLogger().println("Salt User Pass: "+userpass);
        listener.getLogger().println("Salt Auth: "+authtype);
        listener.getLogger().println("Salt Target: "+target);
        listener.getLogger().println("Salt Target Type: "+targettype);
        listener.getLogger().println("Salt Function: "+function);
        listener.getLogger().println("Salt Arguments: "+arguments);
        */

        //Setup connection for auth
        String auth = "username="+username+"&password="+userpass+"&eauth="+authtype;
        String httpResponse = new String();
        httpResponse = sendJSON(servername, auth, null);
        JSONObject authresp = (JSONObject) JSONSerializer.toJSON(httpResponse);
        JSONArray params = authresp.getJSONArray("return");
        //print response from salt api
        //listener.getLogger().println("json params "+params.toString(2));
        String token = new String();
        for (Object o : params ) {
          JSONObject line = (JSONObject) o;
          token = line.getString("token");
        }
        //listener.getLogger().println("token is "+token);
        if (httpResponse.contains("java.io.IOException") || httpResponse.contains("java.net.SocketTimeoutException")) {
          listener.getLogger().println("Error: "+httpResponse);
          return false;
        }


        //If we got this far, auth must have been pretty good
        //listener.getLogger().println("Sending auth to "+servername+": "+token);
        String saltFunc = new String();
        if (arguments.length() > 0){ 
          saltFunc = "client=local&tgt="+target+"&expr_form="+targettype+"&fun="+function+"&arg="+arguments;
        } else {
          saltFunc = "client=local&tgt="+target+"&expr_form="+targettype+"&fun="+function;
        }
        httpResponse = sendJSON(servername, saltFunc, token);
        if (httpResponse.contains("java.io.IOException") || 
            httpResponse.contains("java.net.SocketTimeoutException") || 
            httpResponse.contains("Error")  
           ) {
          listener.getLogger().println("Error: "+function+" "+arguments+" to "+servername+"for "+target+":\n"+httpResponse);
          return false;
        }
        listener.getLogger().println("Response on "+function+" "+arguments+" to "+servername+" for "+target+":\n"+httpResponse);

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
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

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
             JSONArray params = authresp.getJSONArray("return");
             //print response from salt api
             String token = new String();
             for (Object o : params ) {
               JSONObject line = (JSONObject) o;
               token = line.getString("token");
             }
             return FormValidation.ok("Success");
           } catch (Exception e) {
             return FormValidation.error("Client error : "+e.getMessage()+" "+httpResponse);
           }
         }



        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
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

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            //useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }
    }
}

