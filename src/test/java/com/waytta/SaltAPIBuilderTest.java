package com.waytta;

import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JSONObject.class})
public class SaltAPIBuilderTest {

    private static final java.lang.Integer TESTINT = (int)(new Date().getTime());
    private static final Integer TEN = new Integer(10);
    JSONObject clientInterfaces;



    @Before
    public void setup(){
        clientInterfaces = mock(JSONObject.class);
        when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
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
                "credentialsId");
    }
}
