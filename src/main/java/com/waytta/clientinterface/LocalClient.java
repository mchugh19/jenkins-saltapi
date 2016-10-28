package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class LocalClient extends BasicClient {
    public static final int DEFAULT_JOB_POLL_TIME = 10;
    private Integer jobPollTime = DEFAULT_JOB_POLL_TIME;
    private Boolean blockBuild = Boolean.FALSE;

    @DataBoundConstructor
    public LocalClient(Boolean blockbuild, Integer jobPollTime, String target, String targetType) {
        super(target, targetType);

        this.blockBuild = blockbuild;
        this.jobPollTime = jobPollTime;
        setTarget(target);
        setTargetType(targetType);
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
