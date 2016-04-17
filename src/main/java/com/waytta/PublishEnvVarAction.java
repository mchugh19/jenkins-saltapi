package com.waytta;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

public class PublishEnvVarAction extends InvisibleAction implements EnvironmentContributingAction {
    private String key;
    private String value;

    /**
     * Constructor.
     * 
     * @param key
     *            the environment variable key
     * @param value
     *            the environment variable value
     */
    public PublishEnvVarAction(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * hudson.model.EnvironmentContributingAction#buildEnvVars(hudson.model.
     * AbstractBuild, hudson.EnvVars)
     */
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        env.put(key, value);
    }
}
