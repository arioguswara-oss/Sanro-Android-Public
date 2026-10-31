package com.sanrotech.pospublic.data;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.sanrotech.pospublic.auth.AuthorizationPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthorizedPosRepositoryTest {
    @Test public void logoutGateLocksRoomReadsAndWritesWithoutDeletingRows() {
        Context context=ApplicationProvider.getApplicationContext(); context.deleteDatabase("access-test");
        LocalDatabase db= Room.databaseBuilder(context,LocalDatabase.class,"access-test")
                .addMigrations(LocalDatabase.MIGRATION_1_2).allowMainThreadQueries().build();
        try {
            db.ledger().saveProduct(new Product("p","tenant","SKU","Kopi",1000,3),false);
            AuthorizationPolicy policy=new AuthorizationPolicy("boot");
            AuthorizedPosRepository repository=new AuthorizedPosRepository(db,policy);
            assertDenied(() -> repository.products("tenant"));
            assertDenied(() -> repository.saveProduct(new Product("p","tenant","SKU","Ubah",1000,3),true));
            policy.lock();
            assertEquals("Kopi",db.ledger().product("tenant","p").name);
        } finally { db.close(); context.deleteDatabase("access-test"); }
    }
    private void assertDenied(Runnable operation) {
        try { operation.run(); fail("Expected authorization denial"); }
        catch (SecurityException expected) { }
    }
}
