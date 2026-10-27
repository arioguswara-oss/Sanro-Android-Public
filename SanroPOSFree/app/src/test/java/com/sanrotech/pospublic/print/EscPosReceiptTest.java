package com.sanrotech.pospublic.print;

import org.junit.Test;
import static org.junit.Assert.*;

public class EscPosReceiptTest {
    @Test public void cutterExactlyOnceAtEnd() {
        byte[] receipt = EscPosReceipt.testReceipt("LOCAL-TEST", "Barang", 1, 50000, 50000, 50000, true);
        int count = 0;
        for (int i = 0; i + 2 < receipt.length; i++)
            if ((receipt[i] & 255) == 0x1D && (receipt[i + 1] & 255) == 0x56 && receipt[i + 2] == 0) count++;
        assertEquals(1, count);
        assertEquals((byte)0x1D, receipt[receipt.length - 3]);
        assertEquals((byte)0x56, receipt[receipt.length - 2]);
        assertEquals((byte)0, receipt[receipt.length - 1]);
    }
    @Test public void noCutterWithoutExplicitFlag() {
        byte[] receipt = EscPosReceipt.testReceipt("LOCAL-TEST", "Barang", 1, 50000, 50000, 50000, false);
        for (int i = 0; i + 1 < receipt.length; i++) {
            assertFalse((receipt[i] & 255) == 0x1D && (receipt[i + 1] & 255) == 0x56);
            assertFalse((receipt[i] & 255) == 0x1B && (receipt[i + 1] & 255) == 0x70);
        }
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectInvalidPayment() {
        EscPosReceipt.testReceipt("X", "Barang", 1, 1000, 1000, 0, true);
    }
}
