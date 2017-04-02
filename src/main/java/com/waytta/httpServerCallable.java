package com.waytta;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import jenkins.security.MasterToSlaveCallable;


class httpServerCallable extends MasterToSlaveCallable<String, IOException> {
    private static final long serialVersionUID = 1L;
    private String targetURL;

    public httpServerCallable(String targetURL) {
        this.targetURL = targetURL;
    }

    @Override
    public String call() throws IOException {
        HttpURLConnection connection = null;
        String server = "";

        try {
            // Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);
            connection.setConnectTimeout(5000); // set timeout to 5 seconds
            server = connection.getHeaderField("Server");
        } catch (Exception e) {
            server = "unknown";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return server;
    }
}