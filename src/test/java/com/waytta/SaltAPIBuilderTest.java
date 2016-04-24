package com.waytta;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JSONObject.class, Jenkins.class, SaltAPIBuilder.DescriptorImpl.class, CredentialsProvider.class, Utils.class})
public class SaltAPIBuilderTest {

    private static final Integer TESTINT = (int)(new Date().getTime());
    private static final Integer TEN = new Integer(10);
    private static final String DEFAULT_CREDENTIAL_ID = "credentials_id";
    private static final hudson.util.Secret DEFAULT_CREDENTIAL_PASSWORD = Secret.fromString("junit_password");
    JSONObject clientInterfaces;
    private List<StandardUsernamePasswordCredentials> credentials;


    @Before
    public void setup(){
        clientInterfaces = mock(JSONObject.class);                                          
        when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
        credentials = new ArrayList<StandardUsernamePasswordCredentials>();
    }
    
    @Test
    public void testConstructorWithAnyValueForClientInterface() {
        when(clientInterfaces.get("clientInterface")).thenReturn("JUNIT");
        SaltAPIBuilder builder = build();
        validateBuilder(builder, "JUNIT"); 
    }

    @Test
    public void testConstructorWithLocalBatchClientInterface() {
        when(clientInterfaces.get("clientInterface")).thenReturn("local_batch");
        when(clientInterfaces.get("batchSize")).thenReturn("JUNIT_BATCHSIZE");
        SaltAPIBuilder builder = build();
        validateBuilder(builder, "local_batch", "JUNIT_BATCHSIZE");
        
    }

    @Test
    public void testConstructorWithUsePillarAndRunnerClientInterfaceAndUserPillar() {
        when(clientInterfaces.get("clientInterface")).thenReturn("runner");
        when(clientInterfaces.get("mods")).thenReturn("JUNIT_MODS");
        JSONObject usePillar = mock(JSONObject.class);
        when(clientInterfaces.has("usePillar")).thenReturn(TRUE);
        when(clientInterfaces.getJSONObject("usePillar")).thenReturn(usePillar);
        when(usePillar.get("pillarkey")).thenReturn("JUNIT_PILLARKEY");
        when(usePillar.get("pillarvalue")).thenReturn("JUNIT_PILLARVALUE");
        SaltAPIBuilder builder = build();
        validateBuilder(builder, "runner", "JUNIT_MODS", true, "JUNIT_PILLARKEY", "JUNIT_PILLARVALUE");
    }

    @Test
    public void testConstructorWithUsePillarAndRunnerClientInterfaceButNoUserPillar() {
        when(clientInterfaces.get("clientInterface")).thenReturn("runner");
        when(clientInterfaces.get("mods")).thenReturn("JUNIT_MODS");
        
        SaltAPIBuilder builder = build();
        validateBuilder("runner", builder, "JUNIT_MODS");        
    }
    
    @Test
    public void testConstructorWithOutClientInterface() {
        when(clientInterfaces.has("clientInterface")).thenReturn(FALSE);
        when(clientInterfaces.getBoolean("blockBuild")).thenReturn(FALSE);
        when(clientInterfaces.getInt("jobPollTime")).thenReturn(TESTINT);
        SaltAPIBuilder builder = build();
        validateBuilder(builder, "local", TESTINT); 
    }

    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterfaces,
                                 String mods, 
                                 Boolean usePillar,
                                 String pillarkey,
                                 String pillarValue){
        validateBuilder(builder, clientInterfaces, TEN, "100%", mods, usePillar, pillarkey, pillarValue );
    }
    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface) {
        validateBuilder( builder, clientInterface, TEN, "100%", "", false, "", "");
    }

    private void validateBuilder(String clientInterface, 
                                 SaltAPIBuilder builder,
                                 String mods) {
        validateBuilder( builder, clientInterface, TEN, "100%", mods, false, "", "");
    }

    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface,
                                 String batchSize) {
        validateBuilder( builder, clientInterface, TEN, batchSize, "", false, "", "");
    }

    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface,
                                 Integer jobPollTime) {
        validateBuilder( builder, clientInterface, jobPollTime, "100%", "", false, "", "");
    }
    
    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface,
                                 Integer jobPollTime,
                                 String batchSize,
                                 String mods,
                                 Boolean usePillar,
                                 String pillarkey,
                                 String pillarValue) {
        assertFalse(builder.getBlockbuild());
        assertEquals(clientInterface, builder.getClientInterface());
        assertEquals(batchSize, builder.getBatchSize());
        assertEquals(jobPollTime, builder.getJobPollTime());
        assertEquals(mods, builder.getMods());
        assertEquals(usePillar,builder.getUsePillar());
        assertEquals(pillarkey, builder.getPillarkey());
        assertEquals(pillarValue, builder.getPillarvalue());
    }
    
    SaltAPIBuilder build() {
        return new SaltAPIBuilder(
                "servername",
                "authtype",
                "target",
                "targettype",
                "function",
                clientInterfaces,
                "mods",
                "pillarkey",
                "pillarvalue",
                DEFAULT_CREDENTIAL_ID);
    }

    @Test
    public void testPerformWithNoCredentials() throws Exception {
        SaltAPIBuilder builder = setupBuilderForDefaultPerform();
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        Launcher launcher = mock(Launcher.class);
        BuildListener buildListener = mock(BuildListener.class);
        
        assertTrue(builder.perform(jenkinsBuild, launcher, buildListener));
    }
    
    @Test
    public void testPerformWithFailingCredentials() throws Exception {
        StandardUsernamePasswordCredentials mockCred = mock(StandardUsernamePasswordCredentials.class);
        when(mockCred.getId()).thenReturn(DEFAULT_CREDENTIAL_ID);
        when(mockCred.getPassword()).thenReturn(DEFAULT_CREDENTIAL_PASSWORD);
        credentials.add(mockCred);
        
        SaltAPIBuilder builder = setupBuilderForDefaultPerform();
        
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        Launcher launcher = mock(Launcher.class);
        assertFalse(builder.perform(jenkinsBuild, launcher, buildListener));
    }

    @Test
    public void testPerformWithPassingCredentials() throws Exception {
        StandardUsernamePasswordCredentials mockCred = mock(StandardUsernamePasswordCredentials.class);
        when(mockCred.getId()).thenReturn(DEFAULT_CREDENTIAL_ID);
        when(mockCred.getPassword()).thenReturn(DEFAULT_CREDENTIAL_PASSWORD);
        credentials.add(mockCred);

        SaltAPIBuilder builder = setupBuilderForDefaultPerform();
        mockStatic(Utils.class);
        when(Utils.getToken(anyString(), any(JSONArray.class))).thenReturn("okie_dokie");

        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        Launcher launcher = mock(Launcher.class);
        when(Utils.paramorize(jenkinsBuild, buildListener, builder.getTarget())).thenReturn("junit_mytarget");
        when(Utils.paramorize(jenkinsBuild, buildListener, builder.getFunction())).thenReturn("junit_myfunction");
        when(Utils.paramorize(jenkinsBuild, buildListener, builder.getArguments())).thenReturn("junit_myarguments");
        when(Utils.paramorize(jenkinsBuild, buildListener, builder.getKwarguments())).thenReturn("junit_mykwarguments");
        JSONObject httpResponse = mock(JSONObject.class);
        when(httpResponse.toString(2)).thenReturn("junit_httpResponse");
        when(Utils.getJSON(anyString(), any(JSONArray.class),anyString())).thenReturn(httpResponse);
        assertTrue(builder.perform(jenkinsBuild, launcher, buildListener));
    }
    private SaltAPIBuilder setupBuilderForDefaultPerform() {

        when(clientInterfaces.get("clientInterface")).thenReturn("JUNIT");
        SaltAPIBuilder.DescriptorImpl descriptor = mock(SaltAPIBuilder.DescriptorImpl.class);
        mockStatic(Jenkins.class);
        Jenkins jenkins = mock(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getDescriptorOrDie((Class<? extends hudson.model.Describable>) any())).thenReturn(descriptor);


        mockStatic(CredentialsProvider.class);
        when(CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, new ArrayList<DomainRequirement>())).thenReturn(credentials);;

        SaltAPIBuilder builder = build();
        builder.setArguments("junit arguments");
        builder.setKwarguments("junit kwarguments");
        
        return builder;
    }
}
