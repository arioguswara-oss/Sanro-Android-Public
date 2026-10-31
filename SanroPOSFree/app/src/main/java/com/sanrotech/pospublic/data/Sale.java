package com.sanrotech.pospublic.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales", indices = {@Index(value = {"tenantId", "createdAt"})})
public class Sale {
    @PrimaryKey @NonNull public String id;
    @NonNull public String tenantId;
    @NonNull public String receiptNo;
    public long total;
    public long paid;
    public long createdAt;

    public Sale(@NonNull String id, @NonNull String tenantId, @NonNull String receiptNo,
                long total, long paid, long createdAt) {
        this.id = id; this.tenantId = tenantId; this.receiptNo = receiptNo;
        this.total = total; this.paid = paid; this.createdAt = createdAt;
    }
}
