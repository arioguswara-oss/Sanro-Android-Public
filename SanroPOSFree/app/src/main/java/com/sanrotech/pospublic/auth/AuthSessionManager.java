package com.sanrotech.pospublic.auth;

import android.content.Context;
import android.provider.Settings;

/**
 * Fail-closed lifecycle owner for the volatile verified session. Room authority
 * never survives process death. A boot change always requires online revalidation.
 */
public final class AuthSessionManager {
    public enum State {
        LOCKED, AUTHORIZED_VOLATILE, LOGGED_OUT, SECURITY_SUSPENDED,
        DEVICE_TRANSFERRED, REAUTHENTICATION_REQUIRED, RECOVERY_REQUIRED
    }

    private final AuthorizationPolicy policy;
    private final PersistentAuthState persisted;
    private final String bootId;
    private State state;

    public AuthSessionManager(Context context) {
        this(context, currentBootId(context), new PersistentAuthState(context));
    }

    AuthSessionManager(Context context, String bootId, PersistentAuthState persisted) {
        if (context == null || bootId == null || bootId.isEmpty() || persisted == null)
            throw new IllegalArgumentException("Persistent boot-bound auth state required");
        this.bootId = bootId;
        this.persisted = persisted;
        this.policy = new AuthorizationPolicy(bootId);
        this.policy.lock();
        PersistentAuthState.Snapshot previous = persisted.read();
        if (previous == null) {
            state = State.RECOVERY_REQUIRED;
            persisted.write(bootId, "RECOVERY_REQUIRED");
        } else {
            State restored = fromMarker(previous.state);
            if (restored == State.LOGGED_OUT || restored == State.SECURITY_SUSPENDED
                    || restored == State.DEVICE_TRANSFERRED || restored == State.RECOVERY_REQUIRED) {
                // A reboot must never erase an explicit logout or security lock.
                state = restored;
            } else if (!bootId.equals(previous.bootId) || restored == State.AUTHORIZED_VOLATILE) {
                state = State.REAUTHENTICATION_REQUIRED;
            } else {
                state = restored;
            }
            policy.lock();
            persisted.write(bootId, marker(state));
        }
    }

    public synchronized AuthorizationPolicy policy() { return policy; }
    public synchronized State state() { return state; }

    /** A lease is accepted only after the existing cryptographic verifier produced it. */
    public synchronized void installVerifiedLease(OfflineLeaseValidator.VerifiedLease lease) {
        if (state != State.REAUTHENTICATION_REQUIRED || lease == null
                || !bootId.equals(lease.bootId)) {
            if (state != State.SECURITY_SUSPENDED && state != State.DEVICE_TRANSFERRED
                    && state != State.LOGGED_OUT) lock(State.REAUTHENTICATION_REQUIRED);
            throw new SecurityException("Session cannot be restored without current online authorization");
        }
        policy.installVerifiedLeaseFromSession(lease);
        try {
            persisted.write(bootId, marker(State.AUTHORIZED_VOLATILE));
            state = State.AUTHORIZED_VOLATILE;
        } catch (RuntimeException persistenceFailure) {
            policy.lock();
            state = State.RECOVERY_REQUIRED;
            throw persistenceFailure;
        }
    }

    public synchronized void logout() { lock(State.LOGGED_OUT); }
    public synchronized void securitySuspended() { lock(State.SECURITY_SUSPENDED); }
    public synchronized void deviceTransferred() { lock(State.DEVICE_TRANSFERRED); }
    public synchronized void recoveryRequired() { lock(State.RECOVERY_REQUIRED); }

    private void lock(State next) {
        policy.lock();
        state = next;
        persisted.write(bootId, marker(next));
    }

    private static State fromMarker(String marker) {
        switch (marker) {
            case "LOGGED_OUT": return State.LOGGED_OUT;
            case "SECURITY_SUSPENDED": return State.SECURITY_SUSPENDED;
            case "DEVICE_TRANSFERRED": return State.DEVICE_TRANSFERRED;
            case "REAUTHENTICATION_REQUIRED":
            case "REBOOT_REAUTH_REQUIRED": return State.REAUTHENTICATION_REQUIRED;
            case "RECOVERY_REQUIRED": return State.RECOVERY_REQUIRED;
            case "AUTHORIZED_VOLATILE": return State.REAUTHENTICATION_REQUIRED;
            default: return State.RECOVERY_REQUIRED;
        }
    }

    private static String marker(State state) {
        switch (state) {
            case AUTHORIZED_VOLATILE: return "AUTHORIZED_VOLATILE";
            case LOGGED_OUT: return "LOGGED_OUT";
            case SECURITY_SUSPENDED: return "SECURITY_SUSPENDED";
            case DEVICE_TRANSFERRED: return "DEVICE_TRANSFERRED";
            case REAUTHENTICATION_REQUIRED: return "REAUTHENTICATION_REQUIRED";
            case RECOVERY_REQUIRED: return "RECOVERY_REQUIRED";
            default: return "LOCKED";
        }
    }

    private static String currentBootId(Context context) {
        try {
            int count = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
            if (count < 0) throw new SecurityException("Boot counter unavailable");
            return "android-boot-count-" + count;
        } catch (RuntimeException unavailable) {
            throw new SecurityException("Trusted boot identity unavailable", unavailable);
        }
    }
}
