package com.waytta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;

import static org.junit.Assert.*;

public class UtilsTest {
    @Test
    public void testValidPillar() {
        String value = "{\"key\": \"value\"}";
        FormValidation formValidation = Utils.validatePillar(value);
        assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    public void testInvalidPillar() {
        String value = "{\"key\": value}";
        FormValidation formValidation = Utils.validatePillar(value);
        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    @Test
    public void testSuccessfulJSON() {
        File folder = new File("./src/test/java/com/waytta/successfulJSON");
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                JSONArray jsonArray;
                try {
                    jsonArray = JSONArray.fromObject(FileUtils.readFileToString(file));
                    Assert.assertTrue("Testing file: " + file.getName(), Utils.validateFunctionCall(jsonArray));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testFailedJSON() {
        File folder = new File("./src/test/java/com/waytta/failedJSON");
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                JSONArray jsonArray;
                try {
                    jsonArray = JSONArray.fromObject(FileUtils.readFileToString(file));
                    Assert.assertFalse("Testing file: " + file.getName(), Utils.validateFunctionCall(jsonArray));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Mock
    TaskListener listenerMock;

    @Mock
    Run jenkinsBuildMock;

    @Before
    public void setUp() throws Exception {
        jenkinsBuildMock = mock(Run.class);
        listenerMock = mock(TaskListener.class);
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKINGENVVAR", "true");
        // when(jenkinsBuildMock.getBuildVariables()).thenReturn(env);
        when(jenkinsBuildMock.getEnvironment(listenerMock)).thenReturn(new EnvVars(env));
        when(listenerMock.getLogger()).thenReturn(System.out);
    }

    @Test
    public void testParamorizeFoundMatch() throws IOException, InterruptedException {
        Assert.assertEquals(Utils.paramorize(jenkinsBuildMock, listenerMock, "{{WORKINGENVVAR}}"), "true");
    }

    @Test
    public void testParamorizeMissing() throws IOException, InterruptedException {
        Assert.assertEquals(Utils.paramorize(jenkinsBuildMock, listenerMock, "{{DOESNOTEXIST}}"), "");
    }

}
