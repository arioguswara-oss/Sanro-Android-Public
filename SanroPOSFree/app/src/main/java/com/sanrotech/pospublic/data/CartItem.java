package com.sanrotech.pospublic.data;
import androidx.annotation.NonNull;
import androidx.room.Entity;
@Entity(tableName="cart", primaryKeys={"tenantId", "productId"})
public class CartItem {
    @NonNull public String tenantId;
    @NonNull public String productId;
    public int qty;
    public CartItem(@NonNull String tenantId, @NonNull String productId, int qty) {
        this.tenantId=tenantId; this.productId=productId; this.qty=qty;
    }
}
