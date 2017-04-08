package com.waytta;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import jenkins.security.MasterToSlaveCallable;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


class HttpCallable extends MasterToSlaveCallable<JSONObject, IOException> {
    private static final long serialVersionUID = 1L;
    private String targetURL;
    private JSONObject urlParams;
    private String auth;

    public HttpCallable(String targetURL, JSONObject urlParams, String auth) {
        this.targetURL = targetURL;
        this.urlParams = urlParams;
        this.auth = auth;
    }

    @Override
    public JSONObject call() throws IOException {
        HttpURLConnection connection = null;
        JSONObject responseJSON = new JSONObject();

        try {
            // Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);
            if ((urlParams != null && !urlParams.isEmpty()) || targetURL.contains("/hook") ) {
                // We have stuff to send, so do an HTTP POST not GET
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
            }
            connection.setConnectTimeout(5000); // set timeout to 5 seconds
            if (auth != null && !auth.isEmpty()) {
                connection.setRequestProperty("X-Auth-Token", auth);
            }

            // Send request
            if ((urlParams != null && !urlParams.isEmpty()) || targetURL.contains("/hook")) {
                // only necessary if we have stuff to send
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParams.toString());
                wr.flush();
                wr.close();
            }

            // Check http response code and fail out on error
            if (connection.getResponseCode() < 200 || connection.getResponseCode() > 299) {
                String responseError = "";

                try {
                    InputStream err = connection.getErrorStream();
                    if (err != null) {
                        responseError += "HTTP ERROR: \n" + read(err) + "\n\n";
                        err.close();
                    }
                } catch(Exception e){
                }

                try {
                    InputStream str = connection.getInputStream();
                    if (str != null) {
                        responseError += "HTTP Response: \n" + read(str);
                        str.close();
                    }
                } catch(Exception e) {
                }


                throw new IOException("Bad ResponseCode: " +
                                      connection.getResponseCode() + " " +
                                      connection.getResponseMessage() + "\n" +
                                      responseError);
            }

            // Get Response
            InputStream is = connection.getInputStream();
            String responseText = read(is);

            // Server response should be json so this should work
            responseJSON = (JSONObject) JSONSerializer.toJSON(responseText);
            // pass along server header during login
            if (auth == null || auth.isEmpty()) {
                String serverHeader = "unknown";
                serverHeader = connection.getHeaderField("Server");
                responseJSON.element("server", serverHeader);
            }
            return responseJSON;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String read(InputStream stream) throws IOException {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rdr.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rdr.close();

        return response.toString();
    }
}