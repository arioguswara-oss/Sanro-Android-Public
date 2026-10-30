package com.sanrotech.pospublic.auth;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards the checked-in contract copy against the implemented backend's required fields. */
public class BackendContractCompatibilityTest {
    @Test public void checkedInOpenApiContainsBackendCriticalContractFields() throws Exception {
        String text = new String(Files.readAllBytes(Paths.get("docs/mobile-auth-v1/openapi.json")), "UTF-8");
        JSONObject schemas = new JSONObject(text).getJSONObject("components").getJSONObject("schemas");
        assertRequired(schemas, "Context", "authenticated", "local_data_access", "require_online_after_reboot", "lock_local_data_on_security_event");
        assertRequired(schemas, "LeaseClaims", "boot_id", "require_online_after_reboot", "lock_local_data_on_security_event");
        assertRequired(schemas, "LeaseRequest", "challenge_id", "boot_id");
        assertTrue(schemas.getJSONObject("Error").getJSONObject("properties").getJSONObject("error")
                .getJSONObject("properties").getJSONObject("action").getJSONArray("enum").toString().contains("LOCK_LOCAL_DATA"));
        String fixtureText = new String(Files.readAllBytes(Paths.get("docs/mobile-auth-v1/schema-fixtures.json")), "UTF-8");
        assertTrue(fixtureText.contains("SYNTHETIC"));
        assertFalse("fixtures must never carry operational authorization", fixtureText.contains("operational_access"));
    }
    private static void assertRequired(JSONObject schemas, String name, String... expected) throws Exception {
        JSONArray required = schemas.getJSONObject(name).getJSONArray("required");
        for (String field : expected) {
            boolean found=false;
            for (int i=0;i<required.length();i++) if (field.equals(required.getString(i))) found=true;
            assertTrue(name + " missing " + field, found);
        }
    }
}
