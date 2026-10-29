package com.sanrotech.pospublic.auth;

import org.junit.Test;
import java.time.Instant;
import java.util.Collections;
import static org.junit.Assert.*;

public class TenantContextValidatorTest {
    private TenantContextValidator.Context active() {
        TenantContextValidator.Context c = new TenantContextValidator.Context();
        c.contractVersion="1"; c.accountId="a"; c.productAccountId="pa"; c.tenantId="t"; c.tenantCode="SR1";
        c.deviceId="d"; c.deviceJkt="jkt"; c.sessionId="s"; c.authorizationEpoch=1; c.authorityRevision=1;
        c.accountStatus="ACTIVE"; c.tenantStatus="ACTIVE"; c.productStatus="ACTIVE"; c.subscriptionStatus="ACTIVE";
        c.plan="FREE"; c.storageMode="LOCAL_ONLY"; c.accessMode="READ_WRITE";
        c.serverTime=Instant.parse("2026-10-29T00:00:00Z"); c.trialStartedAt=c.serverTime;
        c.trialExpiresAt=Instant.parse("2027-01-29T00:00:00Z"); c.entitlementExpiresAt=c.trialExpiresAt;
        c.permissions=Collections.singleton("sale.create"); c.historyPermissions=Collections.singleton("history.read");
        return c;
    }
    @Test public void validatesBoundActiveContextAndRejectsUnknownOrMismatchedValues() {
        TenantContextValidator validator=new TenantContextValidator();
        assertTrue(validator.isValid(active(),"jkt","d","t"));
        TenantContextValidator.Context mismatch=active(); mismatch.tenantId="caller-selected";
        assertFalse(validator.isValid(mismatch,"jkt","d","t"));
        TenantContextValidator.Context unknown=active(); unknown.plan="FUTURE";
        assertFalse(validator.isValid(unknown,"jkt","d","t"));
    }
    @Test public void expiredMustBeReadOnlyWithoutWritePermissions() {
        TenantContextValidator.Context c=active(); c.subscriptionStatus="EXPIRED"; c.accessMode="READ_ONLY";
        c.serverTime=c.entitlementExpiresAt; c.permissions=Collections.emptySet();
        assertTrue(new TenantContextValidator().isValid(c,"jkt","d","t"));
        c.permissions=Collections.singleton("sale.create");
        assertFalse(new TenantContextValidator().isValid(c,"jkt","d","t"));
    }
}
