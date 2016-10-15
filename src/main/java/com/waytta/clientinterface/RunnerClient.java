package com.waytta.clientinterface;

public class RunnerClient extends BasicClient {

    public RunnerClient(String credentialsId, String function, String mods, String pillarValue){
        super(credentialsId, "", "", function);

        setMods(mods);
        setTarget("");
        setTargetType("");
        setPillarValue(pillarValue);
    }
}
