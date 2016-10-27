package com.waytta.clientinterface;

import hudson.Extension;

public class LocalClient extends BasicClient {
    public static final int DEFAULT_JOB_POLL_TIME = 10;
    private Integer jobPollTime = DEFAULT_JOB_POLL_TIME;
    private Boolean blockBuild = Boolean.FALSE;

    public LocalClient(String credentialsId, String target, String targettype, String function, Boolean blockbuild, Integer jobPollTime) {
        super(credentialsId, target, targettype, function);

        this.blockBuild = blockbuild;
        this.jobPollTime = jobPollTime;
        setTarget(target);
        setTargetType(targettype);
    }
    
    public Integer getJobPollTime() {
        return jobPollTime;
    }
    
    public Boolean getBlockBuild() {
        return blockBuild; 
    }
    
    @Extension
    public static final BasicClientDescriptor DESCRIPTOR = new BasicClientDescriptor(LocalClient.class);
}
