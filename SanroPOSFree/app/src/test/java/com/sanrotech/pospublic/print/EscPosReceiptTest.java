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
    @Test public void multiItemReceiptHasCompleteFooterAndOneCut() {
        com.sanrotech.pospublic.data.SaleLine a = new com.sanrotech.pospublic.data.SaleLine("a","s","p",2,10000);
        com.sanrotech.pospublic.data.SaleLine b = new com.sanrotech.pospublic.data.SaleLine("b","s","q",1,5000);
        a.productName = "Kopi"; b.productName = "Teh";
        byte[] bytes = EscPosReceipt.saleReceipt("saved-sale",java.util.Arrays.asList(a,b),25000,30000,true);
        String text = new String(bytes,java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(text.contains("Kopi")); assertTrue(text.contains("Teh"));
        assertTrue(text.contains("2 x Rp 10000 = Rp 20000"));
        assertTrue(text.contains("KEMBALIAN   Rp 5000")); assertTrue(text.contains("Terima kasih"));
        int cuts=0;
        for(int i=0;i+2<bytes.length;i++) if(bytes[i]==0x1d && bytes[i+1]==0x56 && bytes[i+2]==0) cuts++;
        assertEquals(1,cuts);
    }
    @Test public void productTextCannotInjectPrinterCommands() {
        byte[] bytes=EscPosReceipt.testReceipt("ref", "Kopi\u001dV\u0000\n\u001b@",1,1,1,1,true);
        int init=0,cut=0;
        for(int i=0;i+1<bytes.length;i++) {
            if(bytes[i]==0x1b && bytes[i+1]==0x40) init++;
            if(bytes[i]==0x1d && bytes[i+1]==0x56) cut++;
        }
        assertEquals(1,init); assertEquals(1,cut);
    }
    @Test(expected=IllegalArgumentException.class)
    public void inconsistentTotalCannotPrint() {
        EscPosReceipt.testReceipt("ref","Barang",2,1000,1000,1000,true);
    }
}
