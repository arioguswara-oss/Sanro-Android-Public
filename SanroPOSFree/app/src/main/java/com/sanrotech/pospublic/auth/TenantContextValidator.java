package com.sanrotech.pospublic.auth;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Strict semantic gate for parsed OpenAPI Context values; unknown states fail closed. */
public final class TenantContextValidator {
    public static final class Context {
        public String contractVersion, accountId, productAccountId, tenantId, tenantCode, deviceId,
                deviceJkt, sessionId, accountStatus, tenantStatus, productStatus, subscriptionStatus,
                plan, storageMode, accessMode;
        public long authorizationEpoch, authorityRevision;
        public Instant serverTime, entitlementExpiresAt, trialStartedAt, trialExpiresAt;
        public Set<String> permissions, historyPermissions;
    }
    public boolean isValid(Context c, String expectedJkt, String expectedDevice, String expectedTenant) {
        if (c == null || !eq(c.contractVersion,"1") || !present(c.accountId) || !present(c.productAccountId)
                || !present(c.tenantId) || !present(c.tenantCode) || !present(c.deviceId) || !present(c.deviceJkt)
                || !present(c.sessionId) || c.authorizationEpoch < 1 || c.authorityRevision < 1
                || !eq(c.deviceJkt,expectedJkt) || !eq(c.deviceId,expectedDevice) || !eq(c.tenantId,expectedTenant)
                || c.serverTime == null || c.entitlementExpiresAt == null || !eq(c.storageMode,"LOCAL_ONLY")) return false;
        if (!oneOf(c.accountStatus,"ACTIVE") || !oneOf(c.tenantStatus,"ACTIVE") || !oneOf(c.productStatus,"ACTIVE")) return false;
        if (!oneOf(c.plan,"FREE","STARTER","PRO") || !oneOf(c.subscriptionStatus,"ACTIVE","EXPIRED")) return false;
        if (!oneOf(c.accessMode,"READ_WRITE","READ_ONLY") || c.permissions == null || c.historyPermissions == null) return false;
        if (!known(c.permissions, "sale.create", "product.write", "cart.write")
                || !known(c.historyPermissions,"history.read","receipt.reprint","history.export")) return false;
        if ("FREE".equals(c.plan) && (c.trialStartedAt == null || c.trialExpiresAt == null)) return false;
        if ("EXPIRED".equals(c.subscriptionStatus)) return !c.entitlementExpiresAt.isAfter(c.serverTime)
                && "READ_ONLY".equals(c.accessMode) && c.permissions.isEmpty();
        if (!c.entitlementExpiresAt.isAfter(c.serverTime)) return false;
        if ("FREE".equals(c.plan) && (!c.trialExpiresAt.isAfter(c.trialStartedAt)
                || !c.trialExpiresAt.equals(c.entitlementExpiresAt))) return false;
        return "READ_WRITE".equals(c.accessMode) || c.permissions.isEmpty();
    }
    private static boolean present(String value) { return value != null && !value.trim().isEmpty(); }
    private static boolean eq(String a,String b) { return a != null && a.equals(b); }
    private static boolean oneOf(String value,String... allowed) { return Arrays.asList(allowed).contains(value); }
    private static boolean known(Set<String> input,String... allowed) { return new HashSet<>(Arrays.asList(allowed)).containsAll(input); }
}
