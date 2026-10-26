package com.sanrotech.superadmin;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NavigationPolicyTest {
    @Test public void permitsOnlyProductionSuperadmin() {
        assertTrue(NavigationPolicy.isInternal("https://sanrotech.com/superadmin/login"));
        assertTrue(NavigationPolicy.isInternal("https://sanrotech.com/superadmin"));
        assertTrue(NavigationPolicy.isInternal("https://sanrotech.com/superadmin/dashboard?a=1"));
        assertTrue(NavigationPolicy.isInternal("https://sanrotech.com:443/superadmin/users"));
    }
    @Test public void deniesOffScopeOrInsecureNavigation() {
        assertFalse(NavigationPolicy.isInternal("http://sanrotech.com/superadmin"));
        assertFalse(NavigationPolicy.isInternal("https://evil.example/superadmin"));
        assertFalse(NavigationPolicy.isInternal("https://sanrotech.com.evil.example/superadmin"));
        assertFalse(NavigationPolicy.isInternal("https://sanrotech.com/superadmin-other"));
        assertFalse(NavigationPolicy.isInternal("https://sanrotech.com/"));
        assertFalse(NavigationPolicy.isInternal("javascript:alert(1)"));
        assertFalse(NavigationPolicy.isInternal("https://user@sanrotech.com/superadmin"));
        assertFalse(NavigationPolicy.isInternal("https://sanrotech.com:8443/superadmin"));
        assertFalse(NavigationPolicy.isInternal("https://sanrotech.com/superadmin/../private"));
        assertFalse(NavigationPolicy.isInternal("https://sanrotech.com/superadmin/%2e%2e/private"));
    }
    @Test public void recognizesAuthLandingPages() {
        assertTrue(NavigationPolicy.isLogin("https://sanrotech.com/superadmin/login"));
        assertTrue(NavigationPolicy.isDashboard("https://sanrotech.com/superadmin/dashboard"));
        assertFalse(NavigationPolicy.isDashboard("https://sanrotech.com/superadmin/tenants"));
    }
}
