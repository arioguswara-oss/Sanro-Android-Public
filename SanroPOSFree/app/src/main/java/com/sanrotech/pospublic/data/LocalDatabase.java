package com.sanrotech.pospublic.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import java.util.List;
import java.util.UUID;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Product.class, Sale.class, SaleLine.class, CartItem.class}, version = 2, exportSchema = false)
public abstract class LocalDatabase extends RoomDatabase {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE sale_lines ADD COLUMN productName TEXT NOT NULL DEFAULT ''");
            db.execSQL("UPDATE sale_lines SET productName = COALESCE((SELECT name FROM products WHERE products.id = sale_lines.productId), 'Barang lama')");
            db.execSQL("CREATE TABLE IF NOT EXISTS cart (tenantId TEXT NOT NULL, productId TEXT NOT NULL, qty INTEGER NOT NULL, PRIMARY KEY(tenantId, productId))");
        }
    };
    private static volatile LocalDatabase instance;
    public abstract Ledger ledger();

    public static LocalDatabase open(Context context) {
        if (instance == null) {
            synchronized (LocalDatabase.class) {
                if (instance == null) instance = Room.databaseBuilder(
                    context.getApplicationContext(), LocalDatabase.class, "sanro-pos-public-local-v1"
                ).addMigrations(MIGRATION_1_2).build();
            }
        }
        return instance;
    }

    @Dao
    public abstract static class Ledger {
        @Insert(onConflict = OnConflictStrategy.ABORT)
        public abstract void insertProduct(Product product);
        @Insert public abstract void insertSale(Sale sale);
        @Insert public abstract void insertSaleLine(SaleLine line);
        @Query("SELECT * FROM products WHERE tenantId = :tenantId AND id = :productId LIMIT 1")
        public abstract Product product(String tenantId, String productId);
        @Query("UPDATE products SET stockQty = stockQty - :qty WHERE id = :productId AND tenantId = :tenantId AND stockQty >= :qty")
        public abstract int decreaseStock(String tenantId, String productId, int qty);
        @Query("SELECT * FROM sales WHERE tenantId = :tenantId ORDER BY createdAt DESC LIMIT 1")
        public abstract Sale latestSale(String tenantId);
        @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
        public abstract List<SaleLine> saleLines(String saleId);

        @Query("SELECT * FROM products WHERE tenantId = :tenant ORDER BY name, id")
        public abstract List<Product> products(String tenant);
        @Query("SELECT * FROM sales WHERE tenantId = :tenant ORDER BY createdAt DESC, id DESC")
        public abstract List<Sale> sales(String tenant);
        @Query("SELECT * FROM sales WHERE tenantId = :tenant AND id = :id")
        public abstract Sale sale(String tenant, String id);
        @Query("SELECT * FROM cart WHERE tenantId = :tenant ORDER BY productId")
        public abstract List<CartItem> cart(String tenant);
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract void putCart(CartItem item);
        @Query("DELETE FROM cart WHERE tenantId = :tenant AND productId = :id")
        public abstract void removeCart(String tenant, String id);
        @Query("DELETE FROM cart WHERE tenantId = :tenant")
        public abstract void clearCart(String tenant);
        @Query("UPDATE products SET sku=:sku, name=:name, unitPrice=:price, stockQty=:stock WHERE tenantId=:tenant AND id=:id")
        protected abstract int updateProduct(String tenant, String id, String sku, String name, long price, long stock);

        @Transaction
        public void saveProduct(Product product, boolean existing) {
            if (product.name.trim().isEmpty() || product.sku.trim().isEmpty() ||
                    product.name.length() > 80 || product.sku.length() > 40 ||
                    product.unitPrice < 1 || product.unitPrice > 1000000000L ||
                    product.stockQty < 0 || product.stockQty > 1000000000L)
                throw new IllegalArgumentException("Nama/SKU, harga atau stok tidak valid");
            if (existing) {
                if (updateProduct(product.tenantId, product.id, product.sku, product.name,
                        product.unitPrice, product.stockQty) != 1)
                    throw new IllegalArgumentException("Barang tidak ditemukan");
            } else insertProduct(product);
        }

        // Durable request ID is the sale primary key. Retry returns the committed sale.
        // Room serializes this transaction: any failure rolls back stock, lines and sale.
        @Transaction
        public Sale checkout(String tenant, String requestId, long paid, long when) {
            if (requestId == null || requestId.isEmpty()) throw new IllegalArgumentException("ID kosong");
            Sale previous = sale(tenant, requestId);
            if (previous != null) return previous;
            List<CartItem> items = cart(tenant);
            if (items.isEmpty() || items.size() > 50) throw new IllegalArgumentException("Keranjang harus berisi 1–50 barang");
            long total = 0;
            java.util.ArrayList<SaleLine> lines = new java.util.ArrayList<>();
            for (CartItem item : items) {
                Product product = product(tenant, item.productId);
                if (product == null || item.qty < 1 || item.qty > 1000 || product.unitPrice < 1)
                    throw new IllegalArgumentException("Barang/jumlah tidak valid");
                total = Math.addExact(total, Math.multiplyExact(product.unitPrice, item.qty));
                if (decreaseStock(tenant, product.id, item.qty) != 1)
                    throw new IllegalArgumentException("Stok tidak cukup: " + product.name);
                SaleLine line = new SaleLine(UUID.randomUUID().toString(), requestId, product.id, item.qty, product.unitPrice);
                line.productName = product.name;
                lines.add(line);
            }
            if (paid < total) throw new IllegalArgumentException("Pembayaran kurang");
            Sale result = new Sale(requestId, tenant, "LOCAL-" + requestId.toUpperCase(java.util.Locale.ROOT), total, paid, when);
            insertSale(result);
            for (SaleLine line : lines) insertSaleLine(line);
            clearCart(tenant);
            return result;
        }
    }
}
