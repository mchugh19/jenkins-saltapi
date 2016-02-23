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
}
