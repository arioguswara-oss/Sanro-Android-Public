package com.sanrotech.pospublic.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists only a deny/reauthorize marker and the boot in which it was written.
 * No token, grant, permission or tenant authority is restored from this store.
 */
final class PersistentAuthState {
    private static final String PREFS = "mobile_auth_security_state_v1";
    private static final String BOOT = "boot_id";
    private static final String STATE = "state";
    private final SharedPreferences preferences;

    PersistentAuthState(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized Snapshot read() {
        try {
            if (!preferences.contains(BOOT) || !preferences.contains(STATE)) return null;
            String boot = preferences.getString(BOOT, null);
            String state = preferences.getString(STATE, null);
            if (boot == null || state == null || state.isEmpty()) return null;
            return new Snapshot(boot, state);
        } catch (RuntimeException corrupt) {
            return null;
        }
    }

    synchronized void write(String bootId, String state) {
        if (bootId == null || bootId.isEmpty() || state == null || state.isEmpty()
                || !preferences.edit().putString(BOOT, bootId).putString(STATE, state).commit()) {
            throw new SecurityException("Could not persist authorization lock state");
        }
    }

    static final class Snapshot {
        final String bootId;
        final String state;
        Snapshot(String bootId, String state) { this.bootId = bootId; this.state = state; }
    }
}
