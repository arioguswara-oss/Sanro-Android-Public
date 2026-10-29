package com.sanrotech.pospublic.auth;

import android.os.SystemClock;

/** Central fail-closed policy used at every local repository boundary. */
public final class AuthorizationPolicy {
    public enum Operation { PRODUCT_READ, HISTORY_READ, CART_WRITE, PRODUCT_WRITE, SALE_WRITE, REPRINT }
    private AuthGrant grant;
    private final boolean sandbox;
    private final String currentBootId;

    public AuthorizationPolicy(boolean sandbox, String currentBootId) {
        this.sandbox = sandbox; this.currentBootId = currentBootId;
    }
    synchronized void installVerifiedGrant(AuthGrant verifiedGrant) { grant = verifiedGrant; }
    public synchronized void lock() { grant = null; }
    public synchronized boolean allows(String tenantId, Operation operation) {
        if (sandbox) return true; // Explicitly isolated DEBUG-only tenant; never an account grant.
        if (grant == null || !grant.authenticated || grant.suspended || grant.transferred
                || !same(grant.tenantId, tenantId) || !same(grant.bootId, currentBootId)) return false;
        String capability = capability(operation);
        if (operation == Operation.PRODUCT_READ || operation == Operation.HISTORY_READ || operation == Operation.REPRINT)
            return grant.historyPermissions.contains(capability);
        return grant.mode == AuthGrant.Mode.READ_WRITE && grant.leaseExpiryElapsedMs > SystemClock.elapsedRealtime()
                && grant.permissions.contains(capability);
    }
    public synchronized boolean allow(String tenant, Operation operation) {
        if (!allows(tenant, operation)) throw new SecurityException("Otorisasi lokal tidak tersedia");
        return true;
    }
    private static boolean same(String a, String b) { return a != null && a.equals(b); }
    private static String capability(Operation operation) {
        switch (operation) {
            case PRODUCT_READ: return "product.read";
            case HISTORY_READ: return "history.read";
            case REPRINT: return "receipt.reprint";
            case CART_WRITE: return "cart.write";
            case PRODUCT_WRITE: return "product.write";
            case SALE_WRITE: return "sale.create";
            default: throw new IllegalArgumentException("Unknown operation");
        }
    }
}
