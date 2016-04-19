package com.waytta;

import net.sf.json.JSONObject;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;


import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JSONObject.class})
public class SaltAPIBuilderTest {

    private static final java.lang.Integer TESTINT = (int)(new Date().getTime());
    JSONObject clientInterfaces;
    String servername = "servername";
    String authtype = "authtype";
    String target = "target";
    String targettype = "targettype";
    String function = "function";
    String mods = "mods";
    String pillarkey = "pillarkey";
    String pillarvalue = "pillarvalue";
    String credentialsId = "credentialsId";


    @Before
    public void setup(){
        clientInterfaces = mock(JSONObject.class);
    }
    
    @Test
    public void testConstructorWithAnyValueForClientInterface() {
        when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
        when(clientInterfaces.get("clientInterface")).thenReturn("JUNIT");
        SaltAPIBuilder builder = build();
        assertEquals("JUNIT", builder.getClientInterface());
        assertEquals("100%", builder.getBatchSize());
        assertFalse(builder.getBlockbuild());
        assertEquals(new Integer(10), builder.getJobPollTime());
        assertEquals("", builder.getMods());
        assertFalse(builder.getUsePillar());
        assertEquals("", builder.getPillarkey());
        assertEquals("", builder.getPillarvalue());
    }

    @Test
    public void testConstructorWithLocalBatchClientInterface() {
        when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
        when(clientInterfaces.get("clientInterface")).thenReturn("local_batch");
        when(clientInterfaces.get("batchSize")).thenReturn("JUNIT_BATCHSIZE");
        SaltAPIBuilder builder = build();
        assertEquals("local_batch", builder.getClientInterface());
        assertFalse(builder.getBlockbuild());
        assertEquals(new Integer(10), builder.getJobPollTime());
        assertEquals("", builder.getMods());
        assertFalse(builder.getUsePillar());
        assertEquals("", builder.getPillarkey());
        assertEquals("", builder.getPillarvalue());
    }

    @Test
    public void testConstructorWithUsePillarAndRunnerClientInterfaceAndUserPillar() {
        when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
        when(clientInterfaces.get("clientInterface")).thenReturn("runner");
        when(clientInterfaces.get("mods")).thenReturn("JUNIT_MODS");
        JSONObject usePillar = mock(JSONObject.class);
        when(clientInterfaces.has("usePillar")).thenReturn(TRUE);
        when(clientInterfaces.getJSONObject("usePillar")).thenReturn(usePillar);
        when(usePillar.get("pillarkey")).thenReturn("JUNIT_PILLARKEY");
        when(usePillar.get("pillarvalue")).thenReturn("JUNIT_PILLARVALUE");
        SaltAPIBuilder builder = build();
        assertEquals("runner", builder.getClientInterface());
        assertTrue(builder.getUsePillar());
        assertEquals("JUNIT_PILLARKEY", builder.getPillarkey());
        assertEquals("JUNIT_PILLARVALUE", builder.getPillarvalue());
        assertFalse(builder.getBlockbuild());
        assertEquals(new Integer(10), builder.getJobPollTime());
        assertEquals("100%", builder.getBatchSize());
    }

    @Test
    public void testConstructorWithUsePillarAndRunnerClientInterfaceButNoUserPillar() {
        when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
        when(clientInterfaces.get("clientInterface")).thenReturn("runner");
        when(clientInterfaces.get("mods")).thenReturn("JUNIT_MODS");
        JSONObject usePillar = mock(JSONObject.class);
        SaltAPIBuilder builder = build();
        assertEquals("runner", builder.getClientInterface());
        assertFalse(builder.getUsePillar());
        assertEquals("", builder.getPillarkey());
        assertEquals("", builder.getPillarvalue());
        assertFalse(builder.getBlockbuild());
        assertEquals(new Integer(10), builder.getJobPollTime());
        assertEquals("100%", builder.getBatchSize());
    }
    
    @Test
    public void testConstructorWithOutClientInterface() {
        when(clientInterfaces.has("clientInterface")).thenReturn(FALSE);
        when(clientInterfaces.getBoolean("blockBuild")).thenReturn(FALSE);
        when(clientInterfaces.getInt("jobPollTime")).thenReturn(TESTINT);
        SaltAPIBuilder builder = build();
        assertEquals("local", builder.getClientInterface());
        assertEquals("100%", builder.getBatchSize());
        assertFalse(builder.getBlockbuild());
        assertEquals(TESTINT, builder.getJobPollTime());
        assertEquals("", builder.getMods());
        assertFalse(builder.getUsePillar());
        assertEquals("", builder.getPillarkey());
        assertEquals("", builder.getPillarvalue());
    }

    SaltAPIBuilder build() {
        return new SaltAPIBuilder(
                servername,
                authtype,
                target,
                targettype,
                function,
                clientInterfaces,
                mods,
                pillarkey,
                pillarvalue,
                credentialsId);
    }
}
