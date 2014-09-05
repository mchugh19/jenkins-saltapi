package com.waytta;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.net.*;
import hudson.EnvVars;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

public class Utils {
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
    public static String paramorize(AbstractBuild build, BuildListener listener, String paramer) {
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
}
