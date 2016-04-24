package com.waytta.clientinterface;

import net.sf.json.JSONObject;

public class RunnerClient extends BasicClient {

    public RunnerClient(String credentialsId, String servername, String authtype, String target, String targettype, String function, String mods, JSONObject clientInterfaces){
        super(credentialsId, servername, authtype, target, targettype, function, "100%");

        setMods(mods);

        if (clientInterfaces.has("usePillar")) {
            setUsePillar(true);
            setPillarKey(clientInterfaces.getJSONObject("usePillar").get("pillarkey").toString());
            setPillarValue(clientInterfaces.getJSONObject("usePillar").get("pillarvalue").toString());
        }
    }

    
}
