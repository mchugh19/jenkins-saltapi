package com.waytta.clientinterface;

public class LocalClient extends AbstractClientInterface {

    public LocalClient(String credentialsId, String servername, String authtype, String target, String targettype, String function, Boolean blockbuild, Integer jobPollTime) {
        super(credentialsId, servername, authtype, target, targettype, function, "100%");

        setBlockBuild(blockbuild);
        setJobPollTime(jobPollTime);
    }
}
