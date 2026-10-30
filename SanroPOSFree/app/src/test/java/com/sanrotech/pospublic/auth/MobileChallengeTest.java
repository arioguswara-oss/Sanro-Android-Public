package com.sanrotech.pospublic.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class MobileChallengeTest {
    private static final long RECEIVED_ELAPSED=50_000L;
    private String response(String extras) {
        return "{\"ok\":true,\"request_id\":\"req-1\",\"data\":{"+
                "\"challenge_id\":\"challenge-1\",\"nonce\":\"abcdefghijklmnopqrstuvwxyz0123456789\","+
                "\"server_time\":\"2026-10-30T10:00:00Z\",\"expires_at\":\"2026-10-30T10:01:00Z\"}"+extras+"}";
    }
    @Test public void parsesContractAndMakesChallengeSingleUse() {
        MobileChallenge challenge=MobileChallenge.parse(response(""),RECEIVED_ELAPSED);
        assertNotNull(challenge);
        assertEquals(1_793_354_400L,challenge.consumeForProof(RECEIVED_ELAPSED));
        try { challenge.consumeForProof(RECEIVED_ELAPSED+1000); fail("nonce must be one-use"); }
        catch(SecurityException expected) { }
    }
    @Test public void rejectsMalformedExtraShortAndExpiredChallenge() {
        assertNull(MobileChallenge.parse("{\"ok\":true}",RECEIVED_ELAPSED));
        String extra=response("").replace("\"ok\":true", "\"ok\":true,\"unexpected\":1");
        assertNull(MobileChallenge.parse(extra,RECEIVED_ELAPSED));
        String shortNonce=response("").replace("abcdefghijklmnopqrstuvwxyz0123456789","short");
        assertNull(MobileChallenge.parse(shortNonce,RECEIVED_ELAPSED));
        MobileChallenge challenge=MobileChallenge.parse(response(""),RECEIVED_ELAPSED);
        try { challenge.consumeForProof(RECEIVED_ELAPSED+60_000); fail("expired challenge must fail closed"); }
        catch(SecurityException expected) { }
    }
}
