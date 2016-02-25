package com.waytta;

import org.junit.Assert;
import org.junit.Test;

import net.sf.json.JSONArray;

public class UtilsTest {
    @Test
    public void testValidValidateFunctionCall() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\"sql.stg.local\": {\n" +
                "  \"postgres_database_|-smth-db_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"Database smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.253964\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 207.749,\n" +
                "    \"__run_num__\": 0,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "  },\n" +
                "  \"postgres_user_|-smth-db-user_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "  }\n" +
                "}}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidValidateFunctionCallForSimpleObject() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidValidateFunctionCallForSimpleObjectWithOkReturnCode() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"},\n" +
                "    \"retcode\":0\n" +
                "}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testInvalidValidateFunctionCall() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\"sql.stg.local\": {\n" +
                "  \"postgres_database_|-smth-db_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"Database smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.253964\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 207.749,\n" +
                "    \"__run_num__\": 0,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "  },\n" +
                "  \"postgres_user_|-smth-db-user_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"},\n" +
                "    \"retcode\":2\n" +
                "  }\n" +
                "}}}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testInvalidValidateFunctionCallForSimpleObject() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
                "    \"comment\": \"User made action\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"},\n" +
                "    \"retcode\":2\n" +
                "}}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidateFunctionCallForFailedHighstate() {
	JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
		"\"minionname\": {\n" +
		"  \"cmd_|-fails_|-/bin/false_|-run\": {\n" +
		"    \"__run_num__\": 0,\n" +
		"    \"_stamp\": \"2016-02-23T21:15:55.813678\",\n" +
		"    \"changes\": {\n" +
		"      \"pid\": 16745,\n" +
		"      \"retcode\": 1,\n" +
		"      \"stderr\": \"\",\n" +
		"      \"stdout\": \"\" },\n" +
		"    \"comment\": \"Command \\\"/bin/false\\\" run\",\n" +
		"    \"duration\": 17.302,\n" +
		"    \"fun\": \"state.sls\",\n" +
		"    \"id\": \"minionname\",\n" +
		"    \"jid\": \"20160223151555485695\",\n" +
		"    \"name\": \"/bin/false\",\n" +
		"    \"start_time\": \"15:15:29.462937\",\n" +
		"    \"result\": false,\n" +
		"    \"retcode\":2,\n" +
		"    \"return\": \"Error: cmd.run\",\n" +
		"    \"success\": false\n" +
		"}}}}]");

	Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testInvalidValidateFunctionCallForMissingPillar() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	        "\"data\": {\n" +
		  "\"minionname\": [\n" +
                  "    \"Rendering SLS 'base:failures.pillar' failed: Jinja variable 'dict object' has no attribute 'nope'\"\n" +
                  "]},\n" +
		"\"outputter\":\"highstate\"\n" +
		"}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidateDockerInitWithArrays() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	    "\"data\": {\n" +
	    "\"celery01.mydomain.com\": {\n" +
	    "\"dockerng_|-celery-c-celery_task-1-container_|-c-celery_task-1_|-running\":   {\n" +
		"\"comment\": \"Container 'c-celery_task-1' was replaced. Image changed from 'registry.mydomain.com/celery_task:11d77cc6c1a21d84fd06f2abc982faf940ee392d' to 'registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357'.\",\n" +
		"\"name\": \"c-celery_task-1\",\n" +
		"\"start_time\": \"17:29:56.303184\",\n" +
		"\"result\": true,\n" +
		"\"duration\": 4973.264,\n" +
		"\"__run_num__\": 1,\n" +
		"\"changes\":     {\n" +
		"\"diff\": {\"image\":       {\n" +
		"\"new\": \"registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357\",\n" +
		"\"old\": \"registry.mydomain.com/celery_task:11d77cc6c1a21d84fd06f2abc982faf940ee392d\"\n" +
		"}},\n" +
		"\"removed\": [\"7913e24c53235f2ecf915ca397ea7e48ef9b4f4245b1cb25e72a96bba506314f\"],\n" +
		"\"added\":       {\n" +
		"\"Time_Elapsed\": 0.03751707,\n" +
		"\"Id\": \"77d2301d0e0735550b9cf535fbac466b611be59ac70f351061c00b77395d1151\",\n" +
		"\"Name\": \"c-celery_task-1\",\n" +
		"\"Warnings\": null\n" +
		"}\n" +
		"}\n" +
		"},\n" +
		"\"dockerng_|-celery-celery_task-image_|-registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357_|-image_present\":   {\n" +
		"\"comment\": \"Image 'registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357' was pulled\",\n" +
		"\"name\": \"registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357\",\n" +
		"\"start_time\": \"17:29:48.444762\",\n" +
		"\"result\": true,\n" +
		"\"duration\": 7858.216,\n" +
		"\"__run_num__\": 0,\n" +
		"\"changes\":     {\n" +
		"\"Layers\":       {\n" +
		"\"Pulled\":         [\n" +
		"\"a3ed95caeb02\",\n" +
		"\"8fb0f56b3447\",\n" +
		"\"0d1cc929b218\"\n" +
		"],\n" +
		"\"Already_Pulled\":         [\n" +
		"\"4ff201d9f6ac\",\n" +
		"\"a3ed95caeb02\",\n" +
		"\"b4b8389cb98d\",\n" +
		"\"071ee56cce56\",\n" +
		"\"9690bd523008\",\n" +
		"\"c1c4f1a1eb80\",\n" +
		"\"177a932959c7\"\n" +
		"]\n" +
		"},\n" +
		"\"Status\": \"Downloaded newer image for registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357\",\n" +
		"\"Time_Elapsed\": 7.8480453\n" +
		"}\n" +
		"}\n" +
		"}}}]");

	Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }
 
    @Test
    public void testValidateFunctionCallForEmptyResponse() {
        JSONArray jsonArray = JSONArray.fromObject("[{}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }
 
    @Test
    public void testValidateFunctionCallForShortEmptyResponse() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	    "\"minion1\":\"\",\n" +
	    "\"minion2\":\"\"\n" +
	    "}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }
}
