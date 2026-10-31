package com.sanrotech.pospublic.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthSessionManagerTest {
    private Context context;

    @Before public void clearState() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("mobile_auth_security_state_v1", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void firstRunAndProcessRestartAreLockedUntilVerifiedOnlineAuthorization() {
        AuthSessionManager first = new AuthSessionManager(context, "boot-1", new PersistentAuthState(context));
        assertEquals(AuthSessionManager.State.RECOVERY_REQUIRED, first.state());
        assertFalse(first.policy().allows("tenant-a", AuthorizationPolicy.Operation.HISTORY_READ));

        context.getSharedPreferences("mobile_auth_security_state_v1", Context.MODE_PRIVATE)
                .edit().putString("state", "AUTHORIZED_VOLATILE").commit();
        AuthSessionManager restarted = new AuthSessionManager(context, "boot-1", new PersistentAuthState(context));
        assertEquals(AuthSessionManager.State.REAUTHENTICATION_REQUIRED, restarted.state());
        assertFalse(restarted.policy().allows("tenant-a", AuthorizationPolicy.Operation.SALE_WRITE));
    }

    @Test public void rebootAndUnreadableStateRequireReauthorization() {
        context.getSharedPreferences("mobile_auth_security_state_v1", Context.MODE_PRIVATE)
                .edit().putString("boot_id", "old-boot").putString("state", "AUTHORIZED_VOLATILE").commit();
        AuthSessionManager afterReboot = new AuthSessionManager(context, "new-boot", new PersistentAuthState(context));
        assertEquals(AuthSessionManager.State.REAUTHENTICATION_REQUIRED, afterReboot.state());
        assertFalse(afterReboot.policy().allows("tenant-a", AuthorizationPolicy.Operation.PRODUCT_READ));

        context.getSharedPreferences("mobile_auth_security_state_v1", Context.MODE_PRIVATE).edit().clear().commit();
        AuthSessionManager missingAnchor = new AuthSessionManager(context, "new-boot", new PersistentAuthState(context));
        assertEquals(AuthSessionManager.State.RECOVERY_REQUIRED, missingAnchor.state());
    }

    @Test public void logoutSuspensionAndTransferRemainLockedAfterRestart() {
        AuthSessionManager manager = new AuthSessionManager(context, "boot-1", new PersistentAuthState(context));
        manager.logout();
        assertEquals(AuthSessionManager.State.LOGGED_OUT, manager.state());
        assertFalse(manager.policy().allows("tenant-a", AuthorizationPolicy.Operation.HISTORY_READ));
        assertEquals(AuthSessionManager.State.LOGGED_OUT,
                new AuthSessionManager(context, "boot-1", new PersistentAuthState(context)).state());

        manager.securitySuspended();
        assertEquals(AuthSessionManager.State.SECURITY_SUSPENDED,
                new AuthSessionManager(context, "boot-1", new PersistentAuthState(context)).state());
        manager.deviceTransferred();
        AuthSessionManager transferred = new AuthSessionManager(context, "boot-1", new PersistentAuthState(context));
        assertEquals(AuthSessionManager.State.DEVICE_TRANSFERRED, transferred.state());
        assertFalse(transferred.policy().allows("tenant-a", AuthorizationPolicy.Operation.SALE_WRITE));
    }

    @Test public void logoutSuspensionAndTransferLocksSurviveReboot() {
        AuthSessionManager manager = new AuthSessionManager(context, "boot-1", new PersistentAuthState(context));
        manager.logout();
        assertEquals(AuthSessionManager.State.LOGGED_OUT,
                new AuthSessionManager(context, "boot-2", new PersistentAuthState(context)).state());
        manager.securitySuspended();
        assertEquals(AuthSessionManager.State.SECURITY_SUSPENDED,
                new AuthSessionManager(context, "boot-3", new PersistentAuthState(context)).state());
        manager.deviceTransferred();
        AuthSessionManager transferred = new AuthSessionManager(context, "boot-4", new PersistentAuthState(context));
        assertEquals(AuthSessionManager.State.DEVICE_TRANSFERRED, transferred.state());
        assertFalse(transferred.policy().allows("tenant-a", AuthorizationPolicy.Operation.HISTORY_READ));
    }

    @Test public void syntheticNullLeaseCannotOpenSession() {
        AuthSessionManager manager = new AuthSessionManager(context, "boot-1", new PersistentAuthState(context));
        try {
            manager.installVerifiedLease(null);
            fail("null authorization must fail closed");
        } catch (SecurityException expected) { }
        assertFalse(manager.policy().allows("tenant-a", AuthorizationPolicy.Operation.SALE_WRITE));
    }
}
