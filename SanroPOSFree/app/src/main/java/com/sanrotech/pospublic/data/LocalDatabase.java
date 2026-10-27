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

@Database(entities = {Product.class, Sale.class, SaleLine.class}, version = 1, exportSchema = false)
public abstract class LocalDatabase extends RoomDatabase {
    private static volatile LocalDatabase instance;
    public abstract Ledger ledger();

    public static LocalDatabase open(Context context) {
        if (instance == null) {
            synchronized (LocalDatabase.class) {
                if (instance == null) instance = Room.databaseBuilder(
                    context.getApplicationContext(), LocalDatabase.class, "sanro-pos-public-local-v1"
                ).build();
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
        @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id LIMIT 100")
        public abstract List<SaleLine> saleLines(String saleId);

        // Room transaction: either sale, sale_lines AND stock commit or all roll back.
        @Transaction
        public Sale checkout(@NonNull String tenantId, @NonNull String productId,
                             int qty, long paid, long when) {
            if (qty < 1 || qty > 1000) throw new IllegalArgumentException("Jumlah tidak valid");
            Product product = product(tenantId, productId);
            if (product == null) throw new IllegalArgumentException("Barang tidak ditemukan");
            if (product.unitPrice < 0) throw new IllegalArgumentException("Harga tidak valid");
            long total = Math.multiplyExact(product.unitPrice, qty);
            if (paid < total) throw new IllegalArgumentException("Pembayaran kurang");
            if (decreaseStock(tenantId, productId, qty) != 1)
                throw new IllegalArgumentException("Stok tidak cukup");
            String uuid = UUID.randomUUID().toString();
            Sale sale = new Sale(uuid, tenantId, "LOCAL-" + uuid.substring(0, 8).toUpperCase(),
                                 total, paid, when);
            insertSale(sale);
            insertSaleLine(new SaleLine(UUID.randomUUID().toString(), sale.id, productId,
                                       qty, product.unitPrice));
            return sale;
        }
    }
}
