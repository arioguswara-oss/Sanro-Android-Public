package com.sanrotech.pospublic.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Collections;
import java.util.Set;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthorizationPolicyTest {
    @Test public void noGrantAndWrongTenantFailClosed() {
        AuthorizationPolicy policy = new AuthorizationPolicy("boot-a");
        assertFalse(policy.allows("tenant-a", AuthorizationPolicy.Operation.SALE_WRITE));
        try { policy.allow("tenant-a", AuthorizationPolicy.Operation.HISTORY_READ); fail("must deny"); }
        catch (SecurityException expected) { }
    }
    @Test public void lockRevokesRepositoryAuthorizationImmediately() {
        AuthorizationPolicy policy = new AuthorizationPolicy("boot-a");
        policy.lock(); assertFalse(policy.allows("tenant-a", AuthorizationPolicy.Operation.PRODUCT_READ));
        assertFalse(policy.allows("tenant-a", AuthorizationPolicy.Operation.SALE_WRITE));
    }
    @Test public void sandboxRequiresExplicitDebugPolicy() {
        AuthorizationPolicy policy = AuthorizationPolicy.debugSandbox("DEBUG_SANDBOX");
        assertTrue(policy.allows("_DEBUG_LOCAL_ONLY_", AuthorizationPolicy.Operation.SALE_WRITE));
        assertFalse(new AuthorizationPolicy("boot-a").allows("_DEBUG_LOCAL_ONLY_", AuthorizationPolicy.Operation.SALE_WRITE));
    }
}
