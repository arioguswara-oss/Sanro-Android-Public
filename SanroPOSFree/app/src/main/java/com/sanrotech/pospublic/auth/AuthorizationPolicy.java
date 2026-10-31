package com.sanrotech.pospublic.auth;

import android.os.SystemClock;

/** Central fail-closed policy used at every local repository boundary. */
public final class AuthorizationPolicy {
    public enum Operation { PRODUCT_READ, HISTORY_READ, CART_WRITE, PRODUCT_WRITE, SALE_WRITE, REPRINT }
    private AuthGrant grant;
    private final boolean sandbox;
    private final String currentBootId;
    private boolean sandboxLocked;

    public AuthorizationPolicy(String currentBootId) { this.sandbox = false; this.currentBootId = currentBootId; }
    public static AuthorizationPolicy debugSandbox(String currentBootId) {
        if (!com.sanrotech.pospublic.BuildConfig.DEBUG) throw new SecurityException("Debug sandbox unavailable in release");
        if (!"DEBUG_SANDBOX".equals(currentBootId)) throw new SecurityException("Sandbox tenant is fixed");
        return new AuthorizationPolicy(true, currentBootId);
    }
    private AuthorizationPolicy(boolean sandbox, String currentBootId) {
        this.sandbox = sandbox; this.currentBootId = currentBootId;
    }
    /** Installs only a lease object that can be created by the cryptographic verifier. */
    synchronized void installVerifiedLease(OfflineLeaseValidator.VerifiedLease lease) {
        if (lease == null || currentBootId == null || !currentBootId.equals(lease.bootId) || !lease.claimForInstallation()) {
            lock();
            throw new SecurityException("Verified lease does not match this boot");
        }
        grant = new AuthGrant(lease.accountId, lease.productAccountId, lease.tenantId, lease.tenantCode,
                lease.deviceId, lease.sessionId, lease.bootId, lease.authorizationEpoch, lease.authorityRevision,
                AuthGrant.Mode.READ_WRITE, lease.permissions, lease.historyPermissions,
                lease.expiryElapsedMillis, true, false, false);
    }
    public synchronized void lock() { grant = null; sandboxLocked = true; }
    synchronized void installVerifiedLeaseFromSession(OfflineLeaseValidator.VerifiedLease lease) {
        installVerifiedLease(lease);
    }
    public synchronized boolean allows(String tenantId, Operation operation) {
        if (sandbox) return !sandboxLocked && same(tenantId, currentBootId);
        if (grant == null || !grant.authenticated || grant.suspended || grant.transferred
                || !same(grant.tenantId, tenantId) || !same(grant.bootId, currentBootId)) return false;
        String capability = capability(operation);
        if (grant.leaseExpiryElapsedMs <= SystemClock.elapsedRealtime()) return false;
        if (operation == Operation.PRODUCT_READ || operation == Operation.HISTORY_READ || operation == Operation.REPRINT)
            return grant.historyPermissions.contains(capability);
        return grant.mode == AuthGrant.Mode.READ_WRITE && grant.permissions.contains(capability);
    }
    public synchronized boolean allow(String tenant, Operation operation) {
        if (!allows(tenant, operation)) throw new SecurityException("Otorisasi lokal tidak tersedia");
        return true;
    }
    private static boolean same(String a, String b) { return a != null && a.equals(b); }
    static String capability(Operation operation) {
        switch (operation) {
            case PRODUCT_READ: return "stock.read";
            case HISTORY_READ: return "history.read";
            case REPRINT: return "receipt.reprint";
            case CART_WRITE: return "cart.write";
            case PRODUCT_WRITE: return "stock.write";
            case SALE_WRITE: return "sale.create";
            default: throw new IllegalArgumentException("Unknown operation");
        }
    }
}
