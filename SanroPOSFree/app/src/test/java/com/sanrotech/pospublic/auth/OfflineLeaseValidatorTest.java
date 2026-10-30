package com.sanrotech.pospublic.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/** Test-only ephemeral signer. These signed test tokens are never installed into application state. */
public class OfflineLeaseValidatorTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final Set<String> WRITES = Collections.singleton("sale.create");
    private static final Set<String> HISTORY = Collections.singleton("history.read");
    private final ECKey signingKey;
    private final ECPublicKey verificationKey;
    private final OfflineLeaseValidator validator;
    private final FakeTime time = new FakeTime();

    public OfflineLeaseValidatorTest() throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");
        generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        KeyPair pair=generator.generateKeyPair();
        verificationKey=(ECPublicKey)pair.getPublic();
        signingKey=new ECKey.Builder(Curve.P_256,verificationKey).privateKey((java.security.interfaces.ECPrivateKey)pair.getPrivate()).keyID("test-k1").build();
        Map<String,OfflineLeaseValidator.VerificationKey> keys=new HashMap<>();
        keys.put("test-k1",new OfflineLeaseValidator.VerificationKey(verificationKey,NOW+OfflineLeaseValidator.MAX_LEASE_MS+60_000));
        validator=new OfflineLeaseValidator(keys,time);
    }
    private static final class FakeTime implements OfflineLeaseValidator.TrustedTime {
        public long serverEpochMillis(){return NOW;}
        public long elapsedRealtimeMillis(){return 40_000;}
        public String bootId(){return "boot_identifier_123456";}
        public boolean isValid(){return true;}
    }
    private OfflineLeaseValidator.ExpectedBinding binding() {
        return new OfflineLeaseValidator.ExpectedBinding("https://issuer.example/mobile","com.sanrotech.pospublic",
                "account-1","product-account-1","ST12345","SR12345","device-1","session-1",
                "thumbprint-1","boot_identifier_123456",7,9,WRITES,HISTORY);
    }
    private String token(long issued,long expiry,String kid,Map<String,Object> override) throws Exception {
        long issuedSeconds=issued/1000, expirySeconds=expiry/1000;
        Map<String,Object> claims=new HashMap<>();
        claims.put("iss","https://issuer.example/mobile"); claims.put("aud","com.sanrotech.pospublic");
        claims.put("sub","account-1"); claims.put("product_account_id","product-account-1");
        claims.put("tenant_id","ST12345"); claims.put("tenant_code","SR12345"); claims.put("device_id","device-1");
        claims.put("session_id","session-1"); claims.put("cnf",Collections.singletonMap("jkt","thumbprint-1"));
        claims.put("authorization_epoch",7); claims.put("authority_revision",9); claims.put("plan","FREE");
        claims.put("storage_mode","LOCAL_ONLY"); claims.put("permissions",Arrays.asList("sale.create"));
        claims.put("history_permissions",Arrays.asList("history.read")); claims.put("iat",issuedSeconds);
        claims.put("nbf",issuedSeconds); claims.put("exp",expirySeconds);
        claims.put("trial_started_at","2026-01-01T00:00:00Z");
        claims.put("trial_expires_at",Instant.ofEpochMilli(expiry).toString());
        claims.put("entitlement_expires_at",Instant.ofEpochMilli(expiry).toString());
        claims.put("jti","unique-test-lease-identifier"); claims.put("boot_id","boot_identifier_123456");
        claims.put("require_online_after_reboot",true); claims.put("lock_local_data_on_security_event",true);
        claims.putAll(override);
        SignedJWT jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(new com.nimbusds.jose.JOSEObjectType("sanro-pos-offline-lease+jwt")).keyID(kid).build(),
                JWTClaimsSet.parse(JSONObjectUtils.toJSONString(claims)));
        jwt.sign(new ECDSASigner(signingKey)); return jwt.serialize();
    }
    @Test public void acceptsVerifiedBoundLeaseAndMapsExpiryToMonotonicClock() throws Exception {
        OfflineLeaseValidator.VerifiedLease result=validator.verify(token(NOW,NOW+23*60*60*1000,"test-k1",Collections.emptyMap()),binding());
        assertNotNull("valid test-signed lease should verify",result);
        assertEquals(40_000+23*60*60*1000L,result.expiryElapsedMillis);
        assertEquals(7,result.authorizationEpoch);
    }
    @Test public void rejectsMalformedUnsignedUnknownAndRetiredKeyTokens() throws Exception {
        assertNull(validator.verify("not.a.jwt",binding()));
        assertNull(validator.verify(token(NOW,NOW+60_000,"unknown",Collections.emptyMap()),binding()));
        Map<String,Object> retired=new HashMap<>(); retired.put("authorization_epoch",8);
        assertNull(validator.verify(token(NOW,NOW+60_000,"test-k1",retired),binding()));
    }
    @Test public void rejectsWrongBootTenantAccountThumbprintEpochAndPermissions() throws Exception {
        String lease=token(NOW,NOW+60_000,"test-k1",Collections.emptyMap());
        OfflineLeaseValidator.ExpectedBinding b=binding();
        assertNull(validator.verify(lease,new OfflineLeaseValidator.ExpectedBinding(b.issuer,b.audience,b.accountId,b.productAccountId,"ST99999",b.tenantCode,b.deviceId,b.sessionId,b.keyThumbprint,b.bootId,b.authorizationEpoch,b.authorityRevision,WRITES,HISTORY)));
        assertNull(validator.verify(lease,new OfflineLeaseValidator.ExpectedBinding(b.issuer,b.audience,b.accountId,b.productAccountId,b.tenantId,b.tenantCode,b.deviceId,b.sessionId,b.keyThumbprint,"different_boot_identifier",b.authorizationEpoch,b.authorityRevision,WRITES,HISTORY)));
        assertNull(validator.verify(token(NOW,NOW+60_000,"test-k1",Collections.singletonMap("authorization_epoch",99)),b));
        assertNull(validator.verify(token(NOW,NOW+60_000,"test-k1",Collections.singletonMap("permissions",Arrays.asList("sale.create","admin.all"))),b));
    }
    @Test public void rejectsExpiredLongEntitlementOverrunAndMissingRebootFlags() throws Exception {
        assertNull(validator.verify(token(NOW-100_000,NOW-1,"test-k1",Collections.emptyMap()),binding()));
        assertNull(validator.verify(token(NOW,NOW+OfflineLeaseValidator.MAX_LEASE_MS+1000,"test-k1",Collections.emptyMap()),binding()));
        assertNull(validator.verify(token(NOW,NOW+60_000,"test-k1",Collections.singletonMap("require_online_after_reboot",false)),binding()));
        OfflineLeaseValidator.TrustedTime invalidTime=new OfflineLeaseValidator.TrustedTime() {
            public long serverEpochMillis(){return NOW;} public long elapsedRealtimeMillis(){return 40_000;}
            public String bootId(){return "boot_identifier_123456";} public boolean isValid(){return false;}
        };
        OfflineLeaseValidator noAnchor=new OfflineLeaseValidator(Collections.singletonMap("test-k1",new OfflineLeaseValidator.VerificationKey(verificationKey,NOW+OfflineLeaseValidator.MAX_LEASE_MS+60_000)),invalidTime);
        assertNull(noAnchor.verify(token(NOW,NOW+60_000,"test-k1",Collections.emptyMap()),binding()));
    }
}
