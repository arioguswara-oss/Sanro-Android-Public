package com.sanrotech.pospublic.auth;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.UUID;

/** Installation identity key. It is non-exportable and is never treated as a hardware ID. */
public final class DeviceIdentity {
    private static final String ALIAS = "sanro-pos-mobile-device-v1";
    private DeviceIdentity() {}
    public static synchronized java.security.PublicKey getOrCreate() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return store.getCertificate(ALIAS).getPublicKey();
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        generator.initialize(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256).setUserAuthenticationRequired(false).build());
        return generator.generateKeyPair().getPublic();
    }
    public static String localInstallLabel() { return UUID.randomUUID().toString(); }
}
