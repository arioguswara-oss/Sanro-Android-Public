package com.sanrotech.pospublic.auth;

import java.util.Set;

/** Semantic checks applied after a pinned-key JWS verifier accepts a lease signature. */
public final class OfflineLeaseValidator {
    public static final long MAX_LEASE_MS = 24L * 60L * 60L * 1000L;
    public interface SignatureVerifier { boolean verifyPinnedLease(String compactJws); }
    public static final class Claims {
        public final String tenantId, deviceId, sessionId, bootId;
        public final long issuedAtElapsedMs, expiresAtElapsedMs, entitlementExpiryElapsedMs;
        public final Set<String> permissions;
        public Claims(String tenant, String device, String session, String boot, long issued, long expiry,
                      long entitlementExpiry, Set<String> permissions) {
            this.tenantId=tenant; this.deviceId=device; this.sessionId=session; this.bootId=boot;
            this.issuedAtElapsedMs=issued; this.expiresAtElapsedMs=expiry;
            this.entitlementExpiryElapsedMs=entitlementExpiry; this.permissions=permissions;
        }
    }
    private final SignatureVerifier verifier;
    public OfflineLeaseValidator(SignatureVerifier verifier) { this.verifier = verifier; }
    public boolean isValid(String compactJws, Claims c, String tenant, String device, String session,
                           String currentBoot, long nowElapsedMs, Set<String> allowedPermissions) {
        if (compactJws == null || compactJws.isEmpty() || !verifier.verifyPinnedLease(compactJws) || c == null) return false;
        if (!eq(c.tenantId, tenant) || !eq(c.deviceId, device) || !eq(c.sessionId, session)
                || !eq(c.bootId, currentBoot)) return false;
        if (c.issuedAtElapsedMs < 0 || c.expiresAtElapsedMs <= nowElapsedMs
                || c.expiresAtElapsedMs <= c.issuedAtElapsedMs
                || c.expiresAtElapsedMs - c.issuedAtElapsedMs > MAX_LEASE_MS
                || c.expiresAtElapsedMs > c.entitlementExpiryElapsedMs) return false;
        return c.permissions != null && allowedPermissions != null && allowedPermissions.containsAll(c.permissions);
    }
    private static boolean eq(String a, String b) { return a != null && a.equals(b); }
}
