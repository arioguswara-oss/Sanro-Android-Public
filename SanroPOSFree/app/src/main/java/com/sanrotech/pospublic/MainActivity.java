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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sanrotech.pospublic.data.LocalDatabase;
import com.sanrotech.pospublic.data.Product;
import com.sanrotech.pospublic.data.Sale;
import com.sanrotech.pospublic.data.SaleLine;
import com.sanrotech.pospublic.print.EscPosReceipt;
import com.sanrotech.pospublic.print.PandaBluetooth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    // Only a debug sandbox until real POS2 authentication and entitlement binding exist.
    private static final String TEST_TENANT = "_DEBUG_LOCAL_ONLY_";
    private static final String TEST_SKU = "DEBUG-001";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean printing = new AtomicBoolean(false);
    private TextView status;
    private String selectedPrinter;
    private LocalDatabase db;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = LocalDatabase.open(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView heading = new TextView(this);
        heading.setText("SANRO POS");
        heading.setTextSize(28);
        layout.addView(heading);
        status = new TextView(this);
        status.setTextSize(16);
        layout.addView(status);
        addButton(layout, "1. Buat barang uji (stok 5)", this::seed);
        addButton(layout, "2. Simpan transaksi uji Rp 50.000", this::checkout);
        addButton(layout, "3. Pilih printer Bluetooth yang sudah paired", this::choosePrinter);
        addButton(layout, "4. Cetak ulang nota uji + auto cutter", this::printLatest);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
        selectedPrinter = getPreferences(MODE_PRIVATE).getString("printer_mac", null);
        show("APK DEBUG/PROTOTIPE — BUKAN transaksi toko. Login tenant dan entitlement belum terhubung. Data uji tersimpan di Room SQLite lokal.");
    }

    private void addButton(LinearLayout layout, String label, Runnable callback) {
        Button button = new Button(this);
        button.setText(label);
        layout.addView(button);
        button.setOnClickListener(v -> callback.run());
    }
    private void show(String message) { runOnUiThread(() -> status.setText(message)); }
    private void runDb(Runnable action) {
        worker.execute(() -> {
            try { action.run(); }
            catch (Exception ex) { show("GAGAL: " + ex.getMessage()); }
        });
    }
    private void seed() {
        if (!BuildConfig.DEBUG) { show("Belum ada aktivasi tenant"); return; }
        runDb(() -> {
            Product product = db.ledger().product(TEST_TENANT, TEST_SKU);
            if (product == null) {
                db.ledger().insertProduct(new Product(TEST_SKU, TEST_TENANT, TEST_SKU, "Barang uji", 50000, 5));
                show("Barang uji tersimpan lokal, stok awal 5.");
            } else show("Barang sudah tersedia, stok: " + product.stockQty);
        });
    }
    private void checkout() {
        if (!BuildConfig.DEBUG) { show("Belum ada aktivasi tenant"); return; }
        runDb(() -> {
            Sale sale = db.ledger().checkout(TEST_TENANT, TEST_SKU, 1, 50000, System.currentTimeMillis());
            Product after = db.ledger().product(TEST_TENANT, TEST_SKU);
            show("TRANSAKSI DEBUG DISIMPAN: " + sale.receiptNo + ". Sisa stok: " + after.stockQty);
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
    private void printLatest() {
        if (!BuildConfig.DEBUG) { show("Belum ada aktivasi tenant"); return; }
        if (!allowBluetooth()) return;
        if (selectedPrinter == null) { show("Pilih printer terlebih dahulu."); return; }
        if (!printing.compareAndSet(false, true)) { show("Cetak sebelumnya masih berlangsung."); return; }
        worker.execute(() -> {
            try {
                Sale sale = db.ledger().latestSale(TEST_TENANT);
                if (sale == null) throw new IllegalStateException("Belum ada transaksi uji tersimpan");
                List<SaleLine> lines = db.ledger().saleLines(sale.id);
                if (lines.size() != 1) throw new IllegalStateException("Nota lokal tidak lengkap");
                SaleLine item = lines.get(0);
                Product product = db.ledger().product(TEST_TENANT, item.productId);
                if (product == null) throw new IllegalStateException("Barang uji tidak ditemukan");
                byte[] bytes = EscPosReceipt.testReceipt(sale.receiptNo, product.name,
                    item.qty, item.unitPrice, sale.total, sale.paid, true);
                show("Mengirim nota uji Bluetooth; jangan tekan ulang.");
                PandaBluetooth.sendPairedPrinter(selectedPrinter, bytes);
                show("Byte nota dikirim. Periksa kertas dan cutter fisik; ini bukan bukti cetak selesai.");
            } catch (Exception ex) {
                show("STATUS CETAK TIDAK PASTI: " + ex.getMessage() + ". Jangan otomatis mengulang.");
            } finally { printing.set(false); }
        });
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdown();
    }
}
