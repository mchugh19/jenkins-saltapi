package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class RunnerClient extends BasicClient {
    private String mods = "";
    private String pillarvalue = "";
    
    @DataBoundConstructor
    public RunnerClient(String mods, String pillarvalue){
        super("", "");

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
