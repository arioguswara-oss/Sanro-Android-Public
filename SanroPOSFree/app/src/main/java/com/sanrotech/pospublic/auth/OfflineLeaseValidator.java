package com.sanrotech.pospublic.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Strict verifier for backend-issued leases. No caller-supplied boolean or fixture can authorize. */
public final class OfflineLeaseValidator {
    public static final long MAX_LEASE_MS = 24L * 60L * 60L * 1000L;
    private static final String TYPE = "sanro-pos-offline-lease+jwt";

    /** Server-time estimate anchored to elapsed realtime for the current boot. */
    public interface TrustedTime {
        long serverEpochMillis();
        long elapsedRealtimeMillis();
        String bootId();
        boolean isValid();
    }

    /** An old verification key remains accepted only until its configured retirement deadline. */
    public static final class VerificationKey {
        public final ECPublicKey key;
        public final long verifyUntilEpochMillis;
        public VerificationKey(ECPublicKey key, long verifyUntilEpochMillis) {
            if (key == null || key.getParams() == null || key.getParams().getCurve().getField().getFieldSize() != 256 ||
                    key.getParams().getOrder().bitLength() != 256 || verifyUntilEpochMillis <= 0)
                throw new IllegalArgumentException("Pinned ES256 P-256 verification key required");
            this.key = key; this.verifyUntilEpochMillis = verifyUntilEpochMillis;
        }
    }

    public static final class ExpectedBinding {
        public final String issuer, audience, accountId, productAccountId, tenantId, tenantCode,
                deviceId, sessionId, keyThumbprint, bootId;
        public final long authorizationEpoch, authorityRevision;
        public final Set<String> allowedWritePermissions, allowedHistoryPermissions;
        public ExpectedBinding(String issuer, String audience, String accountId, String productAccountId,
                String tenantId, String tenantCode, String deviceId, String sessionId, String keyThumbprint,
                String bootId, long authorizationEpoch, long authorityRevision,
                Set<String> allowedWritePermissions, Set<String> allowedHistoryPermissions) {
            this.issuer=issuer; this.audience=audience; this.accountId=accountId;
            this.productAccountId=productAccountId; this.tenantId=tenantId; this.tenantCode=tenantCode;
            this.deviceId=deviceId; this.sessionId=sessionId; this.keyThumbprint=keyThumbprint;
            this.bootId=bootId; this.authorizationEpoch=authorizationEpoch; this.authorityRevision=authorityRevision;
            this.allowedWritePermissions=copy(allowedWritePermissions);
            this.allowedHistoryPermissions=copy(allowedHistoryPermissions);
        }
    }

    /** Result cannot be constructed by callers. Expiry is converted to this boot's monotonic clock. */
    public static final class VerifiedLease {
        public final String accountId, productAccountId, tenantId, tenantCode, deviceId, sessionId, bootId;
        public final long authorizationEpoch, authorityRevision, expiryElapsedMillis;
        public final Set<String> permissions, historyPermissions;
        private final AtomicBoolean installed = new AtomicBoolean(false);
        private VerifiedLease(String accountId, String productAccountId, String tenantId, String tenantCode,
                String deviceId, String sessionId, String bootId, long epoch, long revision, long expiryElapsed,
                Set<String> permissions, Set<String> historyPermissions) {
            this.accountId=accountId; this.productAccountId=productAccountId; this.tenantId=tenantId;
            this.tenantCode=tenantCode; this.deviceId=deviceId; this.sessionId=sessionId; this.bootId=bootId;
            this.authorizationEpoch=epoch; this.authorityRevision=revision; this.expiryElapsedMillis=expiryElapsed;
            this.permissions=copy(permissions); this.historyPermissions=copy(historyPermissions);
        }
        boolean claimForInstallation() { return installed.compareAndSet(false,true); }
    }

    private final Map<String, VerificationKey> keys;
    private final TrustedTime time;
    public OfflineLeaseValidator(Map<String, VerificationKey> keys, TrustedTime time) {
        if (keys == null || keys.isEmpty() || time == null) throw new IllegalArgumentException("Pinned lease trust required");
        this.keys = Collections.unmodifiableMap(new HashMap<>(keys)); this.time = time;
    }

    public VerifiedLease verify(String compactJws, ExpectedBinding expected) {
        try {
            if (compactJws == null || compactJws.length() > 16384 || expected == null || !time.isValid()) return null;
            long now = time.serverEpochMillis(), elapsed = time.elapsedRealtimeMillis();
            if (now <= 0 || elapsed < 0 || !eq(time.bootId(), expected.bootId)) return null;
            SignedJWT jwt = SignedJWT.parse(compactJws);
            Set<String> protectedFields=new HashSet<>(java.util.Arrays.asList("alg","typ","kid"));
            if (!protectedFields.equals(jwt.getHeader().getIncludedParams()) ||
                    !JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm()) ||
                    !TYPE.equals(jwt.getHeader().getType() == null ? null : jwt.getHeader().getType().toString()) ||
                    jwt.getHeader().getKeyID() == null) return null;
            VerificationKey trusted = keys.get(jwt.getHeader().getKeyID());
            if (trusted == null || now > trusted.verifyUntilEpochMillis || !jwt.verify(new ECDSAVerifier(trusted.key))) return null;
            Map<String,Object> c = jwt.getJWTClaimsSet().getClaims();
            Set<String> expectedClaims=new HashSet<>(java.util.Arrays.asList("iss","aud","sub","product_account_id",
                    "tenant_id","tenant_code","device_id","session_id","cnf","authorization_epoch","authority_revision",
                    "plan","storage_mode","permissions","history_permissions","iat","nbf","exp","trial_started_at",
                    "trial_expires_at","entitlement_expires_at","jti","boot_id","require_online_after_reboot",
                    "lock_local_data_on_security_event"));
            if (!expectedClaims.equals(c.keySet())) return null;
            Date issuedDate=jwt.getJWTClaimsSet().getIssueTime(), notBeforeDate=jwt.getJWTClaimsSet().getNotBeforeTime(),
                    expiryDate=jwt.getJWTClaimsSet().getExpirationTime();
            if (issuedDate==null || notBeforeDate==null || expiryDate==null) return null;
            long issued=issuedDate.getTime(), notBefore=notBeforeDate.getTime(), expires=expiryDate.getTime();
            long entitlement=Instant.parse(string(c,"entitlement_expires_at")).toEpochMilli();
            long trialStart=Instant.parse(string(c,"trial_started_at")).toEpochMilli();
            long trialEnd=Instant.parse(string(c,"trial_expires_at")).toEpochMilli();
            if (issued > now + 60_000L || notBefore > now || expires <= now || notBefore < issued || expires <= notBefore ||
                    expires - issued > MAX_LEASE_MS || expires > entitlement || expires > trusted.verifyUntilEpochMillis ||
                    trialEnd <= trialStart || trialEnd != entitlement) return null;
            if (!bool(c,"require_online_after_reboot") || !bool(c,"lock_local_data_on_security_event") ||
                    !"FREE".equals(string(c,"plan")) || !"LOCAL_ONLY".equals(string(c,"storage_mode"))) return null;
            if (!eq(string(c,"iss"),expected.issuer) || !audienceMatches(c.get("aud"),expected.audience) ||
                    !eq(string(c,"sub"),expected.accountId) || !eq(string(c,"product_account_id"),expected.productAccountId) ||
                    !eq(string(c,"tenant_id"),expected.tenantId) || !eq(string(c,"tenant_code"),expected.tenantCode) ||
                    !eq(string(c,"device_id"),expected.deviceId) || !eq(string(c,"session_id"),expected.sessionId) ||
                    !eq(string(c,"boot_id"),expected.bootId) || number(c,"authorization_epoch") != expected.authorizationEpoch ||
                    number(c,"authority_revision") != expected.authorityRevision || !present(string(c,"jti"))) return null;
            Object cnf=c.get("cnf");
            if (!(cnf instanceof Map) || !eq(String.valueOf(((Map<?,?>)cnf).get("jkt")),expected.keyThumbprint) ||
                    ((Map<?,?>)cnf).size()!=1) return null;
            Set<String> writes=strings(c,"permissions"), history=strings(c,"history_permissions");
            if (!expected.allowedWritePermissions.containsAll(writes) || !expected.allowedHistoryPermissions.containsAll(history)) return null;
            long expiryElapsed=Math.addExact(elapsed, expires-now);
            return new VerifiedLease(expected.accountId,expected.productAccountId,expected.tenantId,expected.tenantCode,
                    expected.deviceId,expected.sessionId,expected.bootId,expected.authorizationEpoch,
                    expected.authorityRevision,expiryElapsed,writes,history);
        } catch (Exception invalid) { return null; }
    }

    private static long number(Map<String,Object> c,String k) throws ParseException {
        Object v=c.get(k); if (!(v instanceof Number)) throw new ParseException("invalid number",0);
        long n=((Number)v).longValue(); if (n<1 || ((Number)v).doubleValue()!=n) throw new ParseException("invalid integer",0); return n;
    }
    private static String string(Map<String,Object> c,String k) throws ParseException {
        Object v=c.get(k); if (!(v instanceof String) || ((String)v).isEmpty()) throw new ParseException("invalid string",0); return (String)v;
    }
    private static Set<String> strings(Map<String,Object> c,String k) throws ParseException {
        Object v=c.get(k); if (!(v instanceof java.util.List)) throw new ParseException("invalid list",0);
        Set<String> out=new HashSet<>(); for(Object x:(java.util.List<?>)v) if (!(x instanceof String)||!out.add((String)x)) throw new ParseException("invalid list item",0); return out;
    }
    private static boolean bool(Map<String,Object> c,String k) { return Boolean.TRUE.equals(c.get(k)); }
    private static boolean audienceMatches(Object aud,String expected) {
        // Nimbus normalizes registered `aud` strings to a singleton list when parsing JWTClaimsSet.
        return aud instanceof String ? eq((String)aud,expected) :
                aud instanceof java.util.List && ((java.util.List<?>)aud).size()==1 &&
                        ((java.util.List<?>)aud).get(0) instanceof String && eq((String)((java.util.List<?>)aud).get(0),expected);
    }
    private static boolean present(String s) { return s != null && !s.trim().isEmpty(); }
    private static boolean eq(String a,String b) { return a != null && a.equals(b); }
    private static Set<String> copy(Set<String> source) { return source == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(source)); }
}
