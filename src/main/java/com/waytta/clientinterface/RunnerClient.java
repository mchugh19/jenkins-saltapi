package com.waytta.clientinterface;

import hudson.Extension;

public class RunnerClient extends BasicClient {
    private String mods = "";
    private String pillarvalue = "";
    
    public RunnerClient(String credentialsId, String function, String mods, String pillarvalue){
        super(credentialsId, "", "", function);

        setTarget("");
        setTargetType("");
        this.pillarvalue = pillarvalue;
        this.mods = mods;
    }

    public String getPillarvalue() {
        return pillarvalue;
    }

    public String getMods() {
        return mods;
    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new BasicClientDescriptor(RunnerClient.class);
}
