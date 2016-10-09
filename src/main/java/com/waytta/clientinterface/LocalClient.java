package com.waytta.clientinterface;

public class LocalClient extends BasicClient {

    public LocalClient(String credentialsId, String target, String targettype, String function, Boolean blockbuild, Integer jobPollTime) {
        super(credentialsId, target, targettype, function, "100%");

        setBlockBuild(blockbuild);
        setJobPollTime(jobPollTime);
    }
}
