package com.sanrotech.pospublic.auth;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.util.Base64URL;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;

/** RFC 9449 DPoP proof creation with a non-exportable AndroidKeyStore P-256 key. */
public final class DpopProofClient {
    private static final String ALIAS = "sanro-pos-mobile-device-v1";
    private final PrivateKey privateKey;
    private final ECPublicKey publicKey;
    private final ECKey publicJwk;
    private final String thumbprint;

    public DpopProofClient() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).setUserAuthenticationRequired(false).build());
            generator.generateKeyPair();
        }
        privateKey = (PrivateKey) store.getKey(ALIAS, null);
        publicKey = (ECPublicKey) store.getCertificate(ALIAS).getPublicKey();
        if (privateKey == null || publicKey == null || !"EC".equals(privateKey.getAlgorithm()))
            throw new SecurityException("Installation signing key unavailable");
        if (publicKey.getParams() == null || publicKey.getParams().getCurve().getField().getFieldSize() != 256 ||
                publicKey.getParams().getOrder().bitLength() != 256)
            throw new SecurityException("Installation key is not P-256");
        publicJwk = new ECKey.Builder(Curve.P_256, publicKey).build();
        thumbprint = publicJwk.computeThumbprint("SHA-256").toString();
    }

    public String thumbprint() { return thumbprint; }

    /** Consumes the challenge atomically so a nonce cannot be reused by this client. */
    public String createProof(String method, URI target, MobileChallenge challenge, String accessToken,
                              long elapsedRealtimeMillis) throws Exception {
        if (challenge == null) throw new IllegalArgumentException("Backend challenge required");
        long issuedAtSeconds=challenge.consumeForProof(elapsedRealtimeMillis);
        return createProofOnce(method,target,challenge.nonce,accessToken,issuedAtSeconds);
    }

    private String createProofOnce(String method, URI target, String nonce, String accessToken, long issuedAtSeconds)
            throws Exception {
        if (method == null || !method.matches("[A-Z]{3,10}") || target == null ||
                !"https".equalsIgnoreCase(target.getScheme()) || target.getHost() == null ||
                target.getUserInfo() != null || target.getFragment() != null ||
                nonce == null || nonce.length() < 32 || nonce.length() > 128 || issuedAtSeconds <= 0)
            throw new IllegalArgumentException("Invalid DPoP proof inputs");
        String htu = target.normalize().toASCIIString();
        // RFC 9449 htu excludes query and fragment; backend contract compares the canonical route URL.
        int query = htu.indexOf('?');
        if (query >= 0) htu = htu.substring(0, query);
        org.json.JSONObject claims = new org.json.JSONObject()
                .put("htm", method).put("htu", htu).put("iat", issuedAtSeconds)
                .put("jti", UUID.randomUUID().toString()).put("nonce", nonce);
        if (accessToken != null) claims.put("ath", Base64URL.encode(MessageDigest.getInstance("SHA-256")
                .digest(accessToken.getBytes(StandardCharsets.US_ASCII))).toString());
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).type(new com.nimbusds.jose.JOSEObjectType("dpop+jwt"))
                .jwk(publicJwk.toPublicJWK()).build();
        JWSObject jws = new JWSObject(header, new Payload(claims.toString()));
        jws.sign(new AndroidKeyStoreSigner(privateKey));
        return jws.serialize();
    }

    /** Nimbus owns JOSE serialization; AndroidKeyStore performs ES256 signing without key export. */
    private static final class AndroidKeyStoreSigner implements com.nimbusds.jose.JWSSigner {
        private final PrivateKey key;
        AndroidKeyStoreSigner(PrivateKey key) { this.key = key; }
        @Override public java.util.Set<JWSAlgorithm> supportedJWSAlgorithms() {
            return java.util.Collections.singleton(JWSAlgorithm.ES256);
        }
        @Override public com.nimbusds.jose.jca.JCAContext getJCAContext() { return new com.nimbusds.jose.jca.JCAContext(); }
        @Override public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
            try {
                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initSign(key);
                signature.update(signingInput);
                // Nimbus performs the standard ASN.1 ECDSA-to-JOSE fixed-width conversion.
                return Base64URL.encode(ECDSA.transcodeSignatureToConcat(signature.sign(), 32));
            } catch (Exception e) { throw new JOSEException("AndroidKeyStore ES256 signing failed", e); }
        }
    }
}
