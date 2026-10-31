package com.sanrotech.pospublic.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Authorization data accepted only after a trusted verifier has checked the server response. */
public final class AuthGrant {
    public enum Mode { READ_WRITE, READ_ONLY }
    public final String accountId, productAccountId, tenantId, tenantCode, deviceId, sessionId, bootId;
    public final long authorizationEpoch, authorityRevision;
    public final Mode mode;
    public final Set<String> permissions, historyPermissions;
    public final long leaseExpiryElapsedMs;
    public final boolean authenticated, suspended, transferred;

    AuthGrant(String accountId, String productAccountId, String tenantId, String tenantCode,
                     String deviceId, String sessionId, String bootId, long authorizationEpoch,
                     long authorityRevision, Mode mode, Set<String> permissions,
                     Set<String> historyPermissions, long leaseExpiryElapsedMs,
                     boolean authenticated, boolean suspended, boolean transferred) {
        this.accountId = accountId; this.productAccountId = productAccountId;
        this.tenantId = tenantId; this.tenantCode = tenantCode; this.deviceId = deviceId;
        this.sessionId = sessionId; this.bootId = bootId;
        this.authorizationEpoch = authorizationEpoch; this.authorityRevision = authorityRevision;
        this.mode = mode;
        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
        this.historyPermissions = Collections.unmodifiableSet(new HashSet<>(historyPermissions));
        this.leaseExpiryElapsedMs = leaseExpiryElapsedMs; this.authenticated = authenticated;
        this.suspended = suspended; this.transferred = transferred;
    }
}
