package com.sanrotech.pospublic.auth;

import org.junit.Test;
import java.time.Instant;
import java.util.Collections;
import static org.junit.Assert.*;

public class TenantContextValidatorTest {
    private TenantContextValidator.Context context() {
        TenantContextValidator.Context c = new TenantContextValidator.Context();
        c.contractVersion="1"; c.authenticated=true; c.accountId="a"; c.productAccountId="pa";
        c.tenantId="ST00001"; c.tenantCode="SR00001"; c.deviceId="d"; c.deviceJkt="jkt";
        c.sessionId="s"; c.authorizationEpoch=1; c.authorityRevision=1;
        c.serverTime=Instant.parse("2026-10-28T00:00:00Z");
        c.accountStatus="ACTIVE"; c.tenantStatus="ACTIVE"; c.productStatus="ACTIVE";
        c.subscriptionStatus="ACTIVE"; c.plan="FREE"; c.storageMode="LOCAL_ONLY";
        c.trialStartedAt=Instant.parse("2026-10-28T00:00:00Z");
        c.trialExpiresAt=Instant.parse("2027-01-28T00:00:00Z");
        c.entitlementExpiresAt=c.trialExpiresAt; c.accessMode="READ_WRITE";
        c.permissions=Collections.singleton("sale.create");
        c.historyPermissions=Collections.singleton("history.read");
        c.localDataAccess="AUTHORIZED"; c.requireOnlineAfterReboot=true;
        c.lockLocalDataOnSecurityEvent=true;
        return c;
    }
    private boolean valid(TenantContextValidator.Context c) {
        return new TenantContextValidator().isValid(c,"jkt","d","ST00001");
    }
    @Test public void acceptsBackendContextAndRequiresRebootAndLockPolicy() {
        assertTrue(valid(context()));
        TenantContextValidator.Context c=context(); c.authenticated=false; assertFalse(valid(c));
        c=context(); c.requireOnlineAfterReboot=false; assertFalse(valid(c));
        c=context(); c.lockLocalDataOnSecurityEvent=false; assertFalse(valid(c));
        c=context(); c.localDataAccess="LOCKED"; assertFalse(valid(c));
    }
    @Test public void bindsAccountTenantAndDeviceAndKeepsExpiredSubscriptionReadOnly() {
        TenantContextValidator.Context c=context(); c.tenantId="other"; assertFalse(valid(c));
        c=context(); c.subscriptionStatus="EXPIRED"; c.serverTime=c.entitlementExpiresAt;
        c.accessMode="READ_ONLY"; c.permissions=Collections.emptySet(); assertTrue(valid(c));
        c.permissions=Collections.singleton("sale.create"); assertFalse(valid(c));
    }
}
