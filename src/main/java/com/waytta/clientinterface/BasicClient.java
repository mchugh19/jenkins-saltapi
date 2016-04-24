package com.waytta.clientinterface;

/**
 * Created by esmalling on 4/24/16.
 */
public class BasicClient {

    private final String credentialsId;
    private final String serverName;
    private final String authType;
    private final String target;
    private final String targetType;
    private final String function;
    private String batchSize;

    private String mods = "";
    private Boolean usePillar = Boolean.FALSE;
    private String pillarkey = "";
    private String pillarvalue = "";
    private Boolean blockBuild = Boolean.FALSE;
    private Integer jobPollTime = new Integer(10);


    public BasicClient(String credentialsId, String servername, String authType, String target, String targetType, String function) {
        this(credentialsId, servername, authType, target, targetType, function, "100%");
    }

    public BasicClient(String credentialsId, String servername, String authType, String target, String targetType, String function, String batchSize) {
        this.credentialsId = credentialsId;
        this.serverName = servername;
        this.authType = authType;
        this.target = target;
        this.targetType = targetType;
        this.function = function;
        this.batchSize = batchSize;
    }


    public String getPillarValue() {
        return pillarvalue;
    }

    public void setPillarValue(String pillarvalue) {
        this.pillarvalue = pillarvalue;
    }

    public Boolean getUsePillar() {
        return usePillar;
    }

    public void setUsePillar(Boolean usePillar) {
        this.usePillar = usePillar;
    }

    public String getPillarKey() {
        return pillarkey;
    }

    public void setPillarKey(String pillarkey) {
        this.pillarkey = pillarkey;
    }

    public String getMods() {
        return mods;
    }

    public void setMods(String mods) {
        this.mods = mods;
    }

    public String getServerName() {
        return serverName;
    }

    public String getAuthType() {
        return authType;
    }

    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
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
