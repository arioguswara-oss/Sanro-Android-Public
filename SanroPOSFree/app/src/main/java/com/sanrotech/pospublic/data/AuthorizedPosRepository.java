package com.sanrotech.pospublic.data;

import com.sanrotech.pospublic.auth.AuthorizationPolicy;
import java.util.List;

/** Only application-facing Room boundary. Authorization is checked again inside each call. */
public final class AuthorizedPosRepository {
    private final LocalDatabase.Ledger ledger;
    private final AuthorizationPolicy policy;
    public AuthorizedPosRepository(LocalDatabase database, AuthorizationPolicy policy) {
        this.ledger = database.ledger(); this.policy = policy;
    }
    public List<Product> products(String tenant) { policy.allow(tenant, AuthorizationPolicy.Operation.PRODUCT_READ); return ledger.products(tenant); }
    public List<CartItem> cart(String tenant) { policy.allow(tenant, AuthorizationPolicy.Operation.CART_WRITE); return ledger.cart(tenant); }
    public List<Sale> sales(String tenant) { policy.allow(tenant, AuthorizationPolicy.Operation.HISTORY_READ); return ledger.sales(tenant); }
    public Product product(String tenant, String id) { policy.allow(tenant, AuthorizationPolicy.Operation.PRODUCT_READ); return ledger.product(tenant,id); }
    public void changeCart(String tenant, String id, int quantity) {
        policy.allow(tenant, AuthorizationPolicy.Operation.CART_WRITE);
        if (quantity <= 0) ledger.removeCart(tenant,id); else ledger.putCart(new CartItem(tenant,id,quantity));
    }
    public void saveProduct(Product product, boolean existing) {
        policy.allow(product.tenantId, AuthorizationPolicy.Operation.PRODUCT_WRITE); ledger.saveProduct(product,existing);
    }
    public Sale checkout(String tenant, String key, long paid, long when) {
        policy.allow(tenant, AuthorizationPolicy.Operation.SALE_WRITE); return ledger.checkout(tenant,key,paid,when);
    }
    public Sale sale(String tenant, String id) { policy.allow(tenant, AuthorizationPolicy.Operation.HISTORY_READ); return ledger.sale(tenant,id); }
    public List<SaleLine> saleLines(String tenant, String id) {
        policy.allow(tenant, AuthorizationPolicy.Operation.HISTORY_READ);
        if (ledger.sale(tenant,id) == null) throw new SecurityException("Transaksi tidak ditemukan pada tenant aktif");
        return ledger.saleLines(id);
    }
}
