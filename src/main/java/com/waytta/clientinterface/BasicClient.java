package com.waytta.clientinterface;

public class BasicClient {

    public static final int DEFAULT_JOB_POLL_TIME = 10;
    private final String credentialsId;
    private String target;
    private String targetType;
    private final String function;
    private String batchSize = "100%";
    private String mods = "";
    private String pillarvalue = "";
    private Boolean blockBuild = Boolean.FALSE;
    private Integer jobPollTime = DEFAULT_JOB_POLL_TIME;
    

    public BasicClient(String credentialsId, String target, String targetType, String function) {
        this.credentialsId = credentialsId;
        this.target = target;
        this.targetType = targetType;
        this.function = function;
    }


    public String getPillarValue() {
        return pillarvalue;
    }

    public void setPillarValue(String pillarvalue) {
        this.pillarvalue = pillarvalue;
    }

    public String getMods() {
        return mods;
    }

    public void setMods(String mods) {
        this.mods = mods;
    }

    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
    	this.target = target;
    }

    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
    	this.targetType = targetType;
    }

    public String getFunction() {
        return function;
    }

    public Boolean getBlockBuild() {
        return blockBuild; 
    }

    public String getBatchSize() {
        return batchSize;
    }

    public Integer getJobPollTime() {
        return jobPollTime;
    }

    public void setBlockBuild(Boolean blockBuild) {
        this.blockBuild = blockBuild;
    }

    public void setJobPollTime(Integer jobPollTime) {
        this.jobPollTime = jobPollTime;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public String getCredentialsId() {
        return credentialsId;
    }
}
