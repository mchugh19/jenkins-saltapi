package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class LocalClient extends BasicClient {
    public static final int DEFAULT_JOB_POLL_TIME = 10;
    private Integer jobPollTime = DEFAULT_JOB_POLL_TIME;
    private Boolean blockbuild = false;
    private String target;
    private String targetType;

    @DataBoundConstructor
    public LocalClient(Boolean blockbuild, Integer jobPollTime, String target, String targetType) {
        this.blockbuild = blockbuild;
        this.jobPollTime = jobPollTime;
        this.target = target;
        this.targetType = targetType;
    }
    
    public Integer getJobPollTime() {
        return jobPollTime;
    }
    
    public Boolean getBlockbuild() {
        return blockbuild; 
    }
    
    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }
    
    //@Extension
    //public static final BasicClientDescriptor DESCRIPTOR = new BasicClientDescriptor(LocalClient.class);
}
