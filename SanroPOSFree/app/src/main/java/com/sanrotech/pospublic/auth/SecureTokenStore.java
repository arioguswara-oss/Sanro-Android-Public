package com.sanrotech.pospublic.auth;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Refresh token storage only. Access tokens stay in memory; file is in noBackupFilesDir. */
public final class SecureTokenStore {
    private static final String ALIAS = "sanro-pos-refresh-aes-v1";
    private final File file;
    private final byte[] aad;
    public SecureTokenStore(Context context, String accountId, String tenantId, String deviceId) {
        if (empty(accountId) || empty(tenantId) || empty(deviceId)) throw new IllegalArgumentException("Token binding required");
        aad = ("sanro-pos-refresh-token:v1|" + accountId + "|" + tenantId + "|" + deviceId).getBytes(StandardCharsets.UTF_8);
        file = new File(context.getNoBackupFilesDir(), "mobile-refresh.enc");
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return gen.generateKey();
    }
    public synchronized void save(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD(aad);
        byte[] iv = cipher.getIV(), encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        java.nio.file.Files.write(temp.toPath(), (Base64.encodeToString(iv, Base64.NO_WRAP) + "\n" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).getBytes(StandardCharsets.UTF_8));
        if (!temp.renameTo(file)) { temp.delete(); throw new java.io.IOException("Token save failed"); }
    }
    public synchronized String load() throws Exception {
        if (!file.exists()) return null;
        String[] parts = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).split("\\n", -1);
        if (parts.length != 2) { clear(); throw new SecurityException("Token storage corrupt"); }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))); cipher.updateAAD(aad);
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
    public synchronized void clear() { file.delete(); }
    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
}

