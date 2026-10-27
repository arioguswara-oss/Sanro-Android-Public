package com.sanrotech.pospublic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.widget.*;
import com.sanrotech.pospublic.data.*;
import com.sanrotech.pospublic.print.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean printing = new AtomicBoolean(false);
    private final AtomicBoolean paying = new AtomicBoolean(false);
    private TextView status;
    private LinearLayout content;
    private String selectedPrinter;
    private LocalDatabase db;
    private String tenant;
    private String screen = "Kasir";
    private List<Product> products = new ArrayList<>();
    private List<CartItem> cart = new ArrayList<>();
    private List<Sale> sales = new ArrayList<>();
    private String requestId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column();
        applySafeArea(root);
        TextView heading = text("SANRO POS • Free", 26);
        root.addView(heading);
        status = text("Aktivasi POS2 belum tersedia. Operasi tenant terkunci.", 15);
        root.addView(status);
        root.addView(text("Login native dan aktivasi tenant belum tersedia. Kontrak akun, perangkat, dan izin operasional masih menunggu verifikasi.", 18));
        Button login = new Button(this);
        login.setText("Masuk POS2 • belum tersedia");
        login.setEnabled(false);
        root.addView(login);
        Button recovery = new Button(this);
        recovery.setText("Pulihkan sesi perangkat • belum tersedia");
        recovery.setEnabled(false);
        root.addView(recovery);
        if (BuildConfig.DEBUG) {
            addButton(root, "Buka sandbox lokal • bukan akun toko", this::openDebugSandbox);
        }
        setContentView(root);
    }
    private void openDebugSandbox() {
        if (!BuildConfig.DEBUG || db != null) return;
        LinearLayout root = column();
        applySafeArea(root);
        root.addView(text("SANRO POS • Free", 26));
        status = text("SANDBOX DEBUG • bukan sesi POS2", 15);
        root.addView(status);
        content = column();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        // No debug tenant or operational database is selected in release.
        tenant = "_DEBUG_LOCAL_ONLY_";
        db = LocalDatabase.open(this);
        requestId = getPreferences(MODE_PRIVATE).getString("checkout_request", null);
        if (requestId == null) rotateRequest();
        selectedPrinter = getPreferences(MODE_PRIVATE).getString("printer_mac", null);
        root.addView(text("SANDBOX DEBUG • data uji lokal, bukan aktivasi toko", 16));
        LinearLayout tabs = new LinearLayout(this);
        for (String name : new String[]{"Kasir", "Barang", "Riwayat", "Printer"}) {
            Button tab = new Button(this); tab.setText(name);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, -2, 1));
            tab.setOnClickListener(v -> { screen = name; refresh(); });
        }
        root.addView(tabs);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        show("Data disimpan offline pada perangkat ini.");
        refresh();
    }
    // Android 15+ may draw our content under system bars. Preserve the layout's
    // original padding and account for real status/navigation/cutout insets.
    private void applySafeArea(LinearLayout root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int insetLeft, insetTop, insetRight, insetBottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                insetLeft = safe.left;
                insetTop = safe.top;
                insetRight = safe.right;
                insetBottom = safe.bottom;
            } else {
                insetLeft = insets.getSystemWindowInsetLeft();
                insetTop = insets.getSystemWindowInsetTop();
                insetRight = insets.getSystemWindowInsetRight();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left + insetLeft, top + insetTop,
                right + insetRight, bottom + insetBottom);
            return insets;
        });
    }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL);
        int p = (int)(12 * getResources().getDisplayMetrics().density);
        view.setPadding(p, p, p, p); return view;
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); return view;
    }
    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); parent.addView(button);
        button.setOnClickListener(v -> action.run());
    }
    // Abbreviate only in the list; keep complete immutable receipt IDs in
    // history/detail, stored sale records and printed receipts.
    private String historyReference(String value) {
        if (value == null) return "";
        return value.length() > 19 ? value.substring(0, 19) + "…" : value;
    }
    private String money(long value) { return "Rp " + String.format(Locale.forLanguageTag("id-ID"), "%,d", value); }
    private void show(String message) { runOnUiThread(() -> { if (!isDestroyed()) status.setText(message); }); }
    private void runDb(Runnable action) {
        if (worker.isShutdown()) return;
        worker.execute(() -> { try { action.run(); } catch (Exception ex) { show("Gagal: " + ex.getMessage()); } });
    }
    private void rotateRequest() {
        requestId = UUID.randomUUID().toString();
        if (!getPreferences(MODE_PRIVATE).edit().putString("checkout_request", requestId).commit())
            throw new IllegalStateException("ID transaksi tidak dapat disimpan");
    }
    private void refresh() {
        if (db == null) return;
        runDb(() -> {
            List<Product> p = db.ledger().products(tenant);
            List<CartItem> c = db.ledger().cart(tenant);
            List<Sale> h = db.ledger().sales(tenant);
            runOnUiThread(() -> { if (isDestroyed()) return; products=p; cart=c; sales=h; render(); });
        });
    }
    private Product product(String id) {
        for (Product p : products) if (p.id.equals(id)) return p;
        throw new IllegalStateException("Barang tidak ditemukan");
    }
    private void render() {
        content.removeAllViews();
        if (screen.equals("Barang")) {
            addButton(content, "+ Tambah barang", () -> editProduct(null));
            for (Product p : products) addButton(content, p.name + " • " + p.sku + "\n" + money(p.unitPrice) + " • stok " + p.stockQty, () -> editProduct(p));
        } else if (screen.equals("Riwayat")) {
            content.addView(text("Riwayat transaksi cash", 22));
            if (sales.isEmpty()) content.addView(text("Belum ada transaksi.", 16));
            for (Sale sale : sales) addButton(content, new java.text.SimpleDateFormat("dd/MM HH:mm", Locale.ROOT).format(new Date(sale.createdAt)) + " • " + money(sale.total) + "\nNo. uji: " + historyReference(sale.receiptNo), () -> detail(sale));
        } else if (screen.equals("Printer")) {
            content.addView(text("Printer tersimpan: " + (selectedPrinter == null ? "belum dipilih" : selectedPrinter), 18));
            addButton(content, "Pilih printer Bluetooth paired", this::choosePrinter);
            content.addView(text("Cetak ulang melalui detail riwayat. Tidak mengubah stok atau pembayaran. Cutter aktif; periksa hasil fisik.", 16));
        } else renderCashier();
    }
    private void renderCashier() {
        content.addView(text("Pilih barang", 22));
        EditText search = field(content, "Cari nama / SKU", "", false);
        LinearLayout matches = column(); content.addView(matches);
        Runnable filter = () -> {
            matches.removeAllViews(); String query = search.getText().toString().toLowerCase(Locale.ROOT);
            for (Product p : products) if ((p.name + " " + p.sku).toLowerCase(Locale.ROOT).contains(query))
                addButton(matches, p.name + " • " + money(p.unitPrice) + " • stok " + p.stockQty, () -> changeCart(p.id, 1));
            if (products.isEmpty()) matches.addView(text("Tambahkan barang di tab Barang.", 16));
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { filter.run(); }
            public void afterTextChanged(Editable e) {}
        }); filter.run();
        content.addView(text("Keranjang", 22));
        long total = 0;
        for (CartItem item : cart) {
            Product p = product(item.productId); total = Math.addExact(total, Math.multiplyExact(p.unitPrice, item.qty));
            content.addView(text(p.name + " × " + item.qty + " = " + money(p.unitPrice * item.qty), 18));
            LinearLayout actions = new LinearLayout(this); content.addView(actions);
            addButton(actions, "−", () -> changeCart(p.id, -1));
            addButton(actions, "+", () -> changeCart(p.id, 1));
        }
        content.addView(text("Total " + money(total), 24));
        if (!cart.isEmpty()) {
            long due = total;
            addButton(content, "Bayar cash", () -> payment(due));
        }
    }
    private void changeCart(String id, int delta) {
        if (paying.get()) return;
        // Change request before mutating cart: a committed checkout can never be reused for a new cart.
        rotateRequest();
        runDb(() -> {
            int qty = delta;
            for (CartItem c : db.ledger().cart(tenant)) if (c.productId.equals(id)) qty += c.qty;
            Product p = db.ledger().product(tenant, id);
            if (qty > 1000 || p == null || qty > p.stockQty) throw new IllegalArgumentException("Jumlah melebihi stok/batas 1000");
            if (qty <= 0) db.ledger().removeCart(tenant, id);
            else db.ledger().putCart(new CartItem(tenant, id, qty));
            refresh();
        });
    }
    private EditText field(LinearLayout parent, String hint, String value, boolean number) {
        EditText input = new EditText(this); input.setHint(hint); input.setText(value); input.setSingleLine(true);
        if (number) input.setInputType(InputType.TYPE_CLASS_NUMBER);
        parent.addView(input); return input;
    }
    private void editProduct(Product p) {
        if (paying.get()) return;
        LinearLayout form = column();
        EditText sku=field(form,"SKU", p==null?"":p.sku,false);
        EditText name=field(form,"Nama barang",p==null?"":p.name,false);
        EditText price=field(form,"Harga (rupiah bulat)",p==null?"":Long.toString(p.unitPrice),true);
        EditText stock=field(form,"Stok tersedia",p==null?"":Long.toString(p.stockQty),true);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(p==null?"Tambah barang":"Edit barang / stok")
            .setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            try {
                Product value = new Product(p==null?UUID.randomUUID().toString():p.id,tenant,
                    sku.getText().toString().trim(),name.getText().toString().trim(),
                    Long.parseLong(price.getText().toString()), Long.parseLong(stock.getText().toString()));
                runDb(() -> { db.ledger().saveProduct(value,p!=null); show("Barang tersimpan."); refresh(); runOnUiThread(dialog::dismiss); });
            } catch (NumberFormatException ex) { show("Harga dan stok harus berupa angka bulat yang valid."); }
        })); dialog.show();
    }
    private void payment(long due) {
        LinearLayout form = column(); form.addView(text("Total " + money(due),22));
        form.addView(text("Harga dan stok diverifikasi ulang saat menyimpan.",15));
        EditText paid = field(form,"Uang diterima", "", true);
        TextView change = text("",18); form.addView(change);
        paid.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void afterTextChanged(Editable e) {}
            public void onTextChanged(CharSequence s,int st,int b,int c) {
                try { long n=Long.parseLong(s.toString()); change.setText(n<due?"Pembayaran kurang":"Kembalian " + money(n-due)); }
                catch (NumberFormatException ex) { change.setText("Masukkan rupiah bulat."); }
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pembayaran cash")
            .setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan transaksi",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            final long tender;
            try { tender=Long.parseLong(paid.getText().toString()); if(tender<due) throw new NumberFormatException(); }
            catch(NumberFormatException ex) { show("Pembayaran kurang/tidak valid."); return; }
            if (!paying.compareAndSet(false,true)) return;
            String key = requestId;
            dialog.setCancelable(false); dialog.getButton(-1).setEnabled(false); dialog.getButton(-2).setEnabled(false);
            worker.execute(() -> {
                try {
                    Sale sale=db.ledger().checkout(tenant,key,tender,System.currentTimeMillis());
                    show("Transaksi tersimpan. Kembalian " + money(sale.paid-sale.total));
                    runOnUiThread(() -> { if (isDestroyed()) return; dialog.dismiss(); screen="Riwayat"; refresh(); detail(sale); });
                } catch(Exception ex) {
                    show("Gagal menyimpan: " + ex.getMessage());
                    runOnUiThread(() -> { dialog.setCancelable(true); dialog.getButton(-1).setEnabled(true); dialog.getButton(-2).setEnabled(true); });
                } finally { paying.set(false); }
            });
        })); dialog.show();
    }
    private void detail(Sale sale) {
        runDb(() -> {
            List<SaleLine> lines=db.ledger().saleLines(sale.id);
            StringBuilder body=new StringBuilder(sale.receiptNo).append("\n");
            for(SaleLine l:lines) body.append(l.productName).append(" × ").append(l.qty).append(" • ").append(money(l.unitPrice*l.qty)).append("\n");
            body.append("Total ").append(money(sale.total)).append("\nCash ").append(money(sale.paid)).append("\nKembalian ").append(money(sale.paid-sale.total));
            runOnUiThread(() -> { if(!isDestroyed()) new AlertDialog.Builder(this).setTitle("Nota tersimpan")
                .setMessage(body).setNegativeButton("Tutup",null).setPositiveButton("Cetak ulang",(d,w)->printSale(sale.id)).show(); });
        });
    }
    private boolean allowBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 12);
            show("Izinkan Bluetooth, lalu tekan tombol lagi.");
            return false;
        }
        return true;
    }
    @SuppressLint("MissingPermission")
    private void choosePrinter() {
        if (!allowBluetooth()) return;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) { show("Aktifkan Bluetooth dahulu."); return; }
        Set<BluetoothDevice> paired = adapter.getBondedDevices();
        List<BluetoothDevice> devices = new ArrayList<>(paired);
        devices.sort(Comparator.comparing(BluetoothDevice::getName,
                Comparator.nullsLast(String::compareToIgnoreCase)));
        if (devices.isEmpty()) { show("Pair printer Panda melalui Pengaturan Bluetooth Android terlebih dahulu."); return; }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++)
            names[i] = (devices.get(i).getName() == null ? "Printer" : devices.get(i).getName()) + " — " + devices.get(i).getAddress();
        new AlertDialog.Builder(this).setTitle("Pilih printer paired")
            .setItems(names, (dialog, which) -> {
                selectedPrinter = devices.get(which).getAddress();
                getPreferences(MODE_PRIVATE).edit().putString("printer_mac", selectedPrinter).apply();
                show("Printer dipilih: " + names[which]);
            }).show();
    }
    private void printSale(String id) {
        if (!BuildConfig.DEBUG || db == null) { show("Belum ada aktivasi tenant"); return; }
        if (!allowBluetooth()) return;
        if (selectedPrinter == null) { show("Pilih printer terlebih dahulu."); return; }
        if (!printing.compareAndSet(false,true)) { show("Cetak sebelumnya masih berlangsung."); return; }
        String mac=selectedPrinter;
        worker.execute(() -> {
            try {
                Sale sale=db.ledger().sale(tenant,id);
                if(sale==null) throw new IllegalStateException("Nota tidak ditemukan");
                List<SaleLine> lines=db.ledger().saleLines(id);
                byte[] bytes=EscPosReceipt.saleReceipt(sale.receiptNo,lines,sale.total,sale.paid,true);
                show("Mengirim nota Bluetooth; jangan tekan ulang.");
                PandaBluetooth.sendPairedPrinter(mac,bytes);
                show("Byte nota dikirim. Periksa kertas dan cutter fisik; ini bukan bukti cetak selesai.");
            } catch(Exception ex) { show("STATUS CETAK TIDAK PASTI: " + ex.getMessage() + ". Jangan otomatis mengulang."); }
            finally { printing.set(false); }
        });
    }
    @Override protected void onDestroy() { super.onDestroy(); worker.shutdown(); }
}
