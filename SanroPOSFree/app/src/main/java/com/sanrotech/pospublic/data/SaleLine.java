package com.sanrotech.pospublic.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sale_lines", indices = {@Index(value = {"saleId"})})
public class SaleLine {
    @PrimaryKey @NonNull public String id;
    @NonNull public String saleId;
    @NonNull public String productId;
    public int qty;
    public long unitPrice;

    public SaleLine(@NonNull String id, @NonNull String saleId, @NonNull String productId,
                    int qty, long unitPrice) {
        this.id = id; this.saleId = saleId; this.productId = productId;
        this.qty = qty; this.unitPrice = unitPrice;
    }
}
