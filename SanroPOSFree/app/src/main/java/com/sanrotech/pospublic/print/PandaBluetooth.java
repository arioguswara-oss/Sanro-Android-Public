package com.sanrotech.pospublic.print;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.OutputStream;
import java.util.UUID;

public final class PandaBluetooth {
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private PandaBluetooth() {}

    // Call on a worker thread, after checking BLUETOOTH_CONNECT permissions.
    @SuppressWarnings("MissingPermission")
    public static void sendPairedPrinter(String mac, byte[] receipt) throws Exception {
        if (receipt == null || receipt.length == 0) throw new IllegalArgumentException("Nota kosong");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) throw new IllegalStateException("Bluetooth belum aktif");
        BluetoothDevice device = adapter.getRemoteDevice(mac);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED)
            throw new IllegalArgumentException("Printer belum dipasangkan");
        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP);
            socket.connect();
            OutputStream out = socket.getOutputStream();
            for (int off = 0; off < receipt.length; off += 96) {
                int n = Math.min(96, receipt.length - off);
                out.write(receipt, off, n);
                out.flush();
                if (off + n < receipt.length) Thread.sleep(45);
            }
            Thread.sleep(1200); // candidate finishing delay; not physical print ACK
        } finally {
            if (socket != null) try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
