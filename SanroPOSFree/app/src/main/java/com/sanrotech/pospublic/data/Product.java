package com.sanrotech.pospublic.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", indices = {@Index(value = {"tenantId", "sku"}, unique = true)})
public class Product {
    @PrimaryKey @NonNull public String id;
    @NonNull public String tenantId;
    @NonNull public String sku;
    @NonNull public String name;
    public long unitPrice;
    public long stockQty;

    public Product(@NonNull String id, @NonNull String tenantId, @NonNull String sku,
                   @NonNull String name, long unitPrice, long stockQty) {
        this.id = id; this.tenantId = tenantId; this.sku = sku; this.name = name;
        this.unitPrice = unitPrice; this.stockQty = stockQty;
    }
}
