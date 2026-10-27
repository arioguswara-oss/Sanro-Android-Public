package com.sanrotech.pospublic.print;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public final class EscPosReceipt {
    private EscPosReceipt() {}

    public static byte[] testReceipt(String reference, String product, int qty, long price,
                                     long total, long paid, boolean cut) {
        com.sanrotech.pospublic.data.SaleLine item = new com.sanrotech.pospublic.data.SaleLine("", "", "", qty, price);
        item.productName = product;
        return saleReceipt(reference, java.util.Collections.singletonList(item), total, paid, cut);
    }

    public static byte[] saleReceipt(String reference, java.util.List<com.sanrotech.pospublic.data.SaleLine> items,
                                     long total, long paid, boolean cut) {
        if (items == null || items.isEmpty() || items.size() > 50 || total < 0 || paid < total)
            throw new IllegalArgumentException("Data nota tidak valid");
        long sum = 0;
        for (com.sanrotech.pospublic.data.SaleLine item : items) {
            if (item.qty <= 0 || item.unitPrice < 0) throw new IllegalArgumentException("Baris nota tidak valid");
            sum = Math.addExact(sum, Math.multiplyExact(item.qty, item.unitPrice));
        }
        if (sum != total) throw new IllegalArgumentException("Total nota tidak sesuai");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bytes(out, 0x1B, 0x40);             // ESC @ initialize
        bytes(out, 0x1B, 0x74, 0x00);       // CP437, ASCII-safe
        bytes(out, 0x1B, 0x61, 0x01);       // center
        line(out, "SANRO POS");
        line(out, "NOTA DEBUG LOKAL");
        bytes(out, 0x1B, 0x61, 0x00);
        line(out, "No: " + clean(reference));
        line(out, "----------------------------------------");
        for (com.sanrotech.pospublic.data.SaleLine item : items) {
            String name = clean(item.productName);
            for (int i = 0; i < name.length(); i += 40) line(out, name.substring(i, Math.min(i + 40, name.length())));
            line(out, item.qty + " x Rp " + item.unitPrice + " = Rp " + Math.multiplyExact(item.qty, item.unitPrice));
        }
        line(out, "----------------------------------------");
        line(out, "TOTAL       Rp " + total);
        line(out, "BAYAR       Rp " + paid);
        line(out, "KEMBALIAN   Rp " + (paid - total));
        line(out, "Terima kasih");
        bytes(out, 0x1B, 0x61, 0x00);
        bytes(out, 0x1B, 0x45, 0x00);
        bytes(out, 0x1D, 0x21, 0x00);
        if (cut) {
            bytes(out, 10, 10, 10, 10);    // feed beyond print head
            bytes(out, 0x1D, 0x56, 0x00);  // GS V 00 full cut
        }
        if (out.size() > 24576) throw new IllegalArgumentException("Nota terlalu panjang");
        return out.toByteArray();
    }

    private static void line(ByteArrayOutputStream out, String value) {
        byte[] b = (clean(value) + "\n").getBytes(StandardCharsets.US_ASCII);
        out.write(b, 0, b.length);
    }
    private static String clean(String value) {
        String input = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 32 && c <= 126) builder.append(c);
        }
        return builder.toString();
    }
    private static void bytes(ByteArrayOutputStream out, int... vals) {
        for (int val : vals) out.write(val);
    }
}
