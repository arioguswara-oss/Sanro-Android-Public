package com.sanrotech.pospublic.auth;

import org.junit.Test;
import java.util.Collections;
import java.util.Set;
import static org.junit.Assert.*;

public class OfflineLeaseValidatorTest {
    private final Set<String> permissions = Collections.singleton("sale.create");
    private OfflineLeaseValidator.Claims claims(long expiry) {
        return new OfflineLeaseValidator.Claims("t1", "d1", "s1", "boot1", 1000, expiry,
                100_000_000, permissions);
    }
    private OfflineLeaseValidator validator(boolean signature) {
        return new OfflineLeaseValidator(token -> signature && "fixture-signed-token".equals(token));
    }
    @Test public void acceptsOnlyVerifiedBoundLeaseInCurrentBoot() {
        assertTrue(validator(true).isValid("fixture-signed-token", claims(2000), "t1", "d1", "s1", "boot1", 1500, permissions));
        assertFalse(validator(false).isValid("fixture-signed-token", claims(2000), "t1", "d1", "s1", "boot1", 1500, permissions));
        assertFalse(validator(true).isValid("fixture-signed-token", claims(2000), "other", "d1", "s1", "boot1", 1500, permissions));
        assertFalse(validator(true).isValid("fixture-signed-token", claims(2000), "t1", "d1", "s1", "boot2", 1500, permissions));
    }
    @Test public void rejectsExpiredOverlongAndEntitlementOverrun() {
        assertFalse(validator(true).isValid("fixture-signed-token", claims(1400), "t1", "d1", "s1", "boot1", 1500, permissions));
        assertFalse(validator(true).isValid("fixture-signed-token", claims(1000 + OfflineLeaseValidator.MAX_LEASE_MS + 1), "t1", "d1", "s1", "boot1", 1500, permissions));
        OfflineLeaseValidator.Claims beyondEntitlement = new OfflineLeaseValidator.Claims("t1", "d1", "s1", "boot1", 1000, 3000, 2500, permissions);
        assertFalse(validator(true).isValid("fixture-signed-token", beyondEntitlement, "t1", "d1", "s1", "boot1", 1500, permissions));
    }
}
