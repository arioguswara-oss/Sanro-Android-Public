package com.sanrotech.pospublic.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LocalDatabaseTest {
    private LocalDatabase db;
    private Context context;
    @Before public void open() {
        context=ApplicationProvider.getApplicationContext();
        context.deleteDatabase("ledger-test");
        db=builder();
        db.ledger().saveProduct(new Product("a","t","A","Kopi",10000,5),false);
        db.ledger().saveProduct(new Product("b","t","B","Teh",5000,2),false);
    }
    private LocalDatabase builder() {
        return Room.databaseBuilder(context,LocalDatabase.class,"ledger-test")
            .addMigrations(LocalDatabase.MIGRATION_1_2).allowMainThreadQueries().build();
    }
    @After public void close() { db.close(); context.deleteDatabase("ledger-test"); }
    @Test public void multiItemCashAndRetryAreAtomic() {
        db.ledger().putCart(new CartItem("t","a",2));
        db.ledger().putCart(new CartItem("t","b",1));
        Sale sale=db.ledger().checkout("t","request",30000,1);
        assertEquals(25000,sale.total); assertEquals(5000,sale.paid-sale.total);
        assertEquals(3,db.ledger().product("t","a").stockQty);
        assertEquals(1,db.ledger().product("t","b").stockQty);
        assertEquals(2,db.ledger().saleLines(sale.id).size());
        assertTrue(db.ledger().cart("t").isEmpty());
        db.ledger().checkout("t","request",30000,2);
        assertEquals(1,db.ledger().sales("t").size());
        assertEquals(3,db.ledger().product("t","a").stockQty);
    }
    @Test public void stockFailureRollsBackEarlierLines() {
        db.ledger().putCart(new CartItem("t","a",1));
        db.ledger().putCart(new CartItem("t","b",3));
        try { db.ledger().checkout("t","failed",100000,1); fail(); }
        catch(IllegalArgumentException expected) { /* transaction rollback */ }
        assertEquals(5,db.ledger().product("t","a").stockQty);
        assertTrue(db.ledger().sales("t").isEmpty());
        assertTrue(db.ledger().saleLines("failed").isEmpty());
        assertEquals(2,db.ledger().cart("t").size());
    }
    @Test public void insufficientCashRollsBackStock() {
        db.ledger().putCart(new CartItem("t","a",1));
        try { db.ledger().checkout("t","failed",9999,1); fail(); }
        catch(IllegalArgumentException expected) { }
        assertEquals(5,db.ledger().product("t","a").stockQty);
        assertTrue(db.ledger().sales("t").isEmpty());
    }
    @Test public void survivesReopenAndKeepsSnapshot() {
        db.ledger().putCart(new CartItem("t","a",1));
        db.close(); db=builder();
        assertEquals(1,db.ledger().cart("t").size());
        db.ledger().checkout("t","persist",10000,1);
        db.ledger().saveProduct(new Product("a","t","A","Nama baru",12000,4),true);
        db.close(); db=builder();
        assertEquals("Kopi",db.ledger().saleLines("persist").get(0).productName);
        assertEquals(10000,db.ledger().sale("t","persist").total);
        assertEquals(4,db.ledger().product("t","a").stockQty);
        assertTrue(db.ledger().cart("t").isEmpty());
    }
    @Test public void tenantCannotSpendAnotherTenantsStock() {
        db.ledger().putCart(new CartItem("other","a",1));
        try { db.ledger().checkout("other","isolated",10000,1); fail(); }
        catch(IllegalArgumentException expected) { }
        assertNull(db.ledger().product("other","a"));
        assertNull(db.ledger().sale("other","isolated"));
        assertTrue(db.ledger().sales("other").isEmpty());
        assertEquals(5,db.ledger().product("t","a").stockQty);
    }
    @Test public void concurrentRetriesCommitOnlyOnce() throws Exception {
        db.ledger().putCart(new CartItem("t","a",1));
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Sale> attempt=()->db.ledger().checkout("t","same",10000,1);
            java.util.concurrent.Future<Sale> a=pool.submit(attempt), b=pool.submit(attempt);
            assertEquals(a.get().id,b.get().id);
            assertEquals(1,db.ledger().sales("t").size());
            assertEquals(4,db.ledger().product("t","a").stockQty);
        } finally { pool.shutdownNow(); }
    }
    @Test public void migratesPrototypeWithoutDataLoss() {
        db.close(); context.deleteDatabase("ledger-test");
        SQLiteDatabase old=context.openOrCreateDatabase("ledger-test",0,null);
        old.execSQL("CREATE TABLE products (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, sku TEXT NOT NULL, name TEXT NOT NULL, unitPrice INTEGER NOT NULL, stockQty INTEGER NOT NULL)");
        old.execSQL("CREATE UNIQUE INDEX index_products_tenantId_sku ON products(tenantId,sku)");
        old.execSQL("CREATE TABLE sales (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, receiptNo TEXT NOT NULL, total INTEGER NOT NULL, paid INTEGER NOT NULL, createdAt INTEGER NOT NULL)");
        old.execSQL("CREATE INDEX index_sales_tenantId_createdAt ON sales(tenantId,createdAt)");
        old.execSQL("CREATE TABLE sale_lines (id TEXT NOT NULL PRIMARY KEY, saleId TEXT NOT NULL, productId TEXT NOT NULL, qty INTEGER NOT NULL, unitPrice INTEGER NOT NULL)");
        old.execSQL("CREATE INDEX index_sale_lines_saleId ON sale_lines(saleId)");
        old.execSQL("INSERT INTO products VALUES ('a','t','A','Lama',10000,4)");
        old.execSQL("INSERT INTO sales VALUES ('s','t','LOCAL-OLD',10000,10000,1)");
        old.execSQL("INSERT INTO sale_lines VALUES ('l','s','a',1,10000)");
        old.setVersion(1); old.close();
        db=builder();
        assertEquals("Lama",db.ledger().saleLines("s").get(0).productName);
        assertEquals(4,db.ledger().product("t","a").stockQty);
        assertEquals(10000,db.ledger().sale("t","s").total);
        assertTrue(db.ledger().cart("t").isEmpty());
    }
}
