package com.sanrotech.pospublic.auth;

import org.json.JSONObject;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/** Strict single-use representation of the backend-issued challenge response. */
public final class MobileChallenge {
    public final String requestId, challengeId, nonce;
    public final long serverTimeMillis, expiresAtMillis;
    private final long receivedElapsedRealtimeMillis;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    private MobileChallenge(String requestId, String challengeId, String nonce, long serverTime, long expiry, long receivedAt) {
        this.requestId=requestId; this.challengeId=challengeId; this.nonce=nonce;
        this.serverTimeMillis=serverTime; this.expiresAtMillis=expiry;
        this.receivedElapsedRealtimeMillis=receivedAt;
    }

    public static MobileChallenge parse(String json, long receivedElapsedRealtimeMillis) {
        try {
            if (json == null || json.length() > 8192 || receivedElapsedRealtimeMillis < 0) return null;
            JSONObject root=new JSONObject(json);
            if (!exact(root,"ok","request_id","data") || !root.getBoolean("ok")) return null;
            String request=root.getString("request_id"); JSONObject data=root.getJSONObject("data");
            if (!exact(data,"challenge_id","nonce","server_time","expires_at")) return null;
            String id=data.getString("challenge_id"), nonce=data.getString("nonce");
            if (request.isEmpty() || id.isEmpty() || nonce.length()<32 || nonce.length()>128) return null;
            long server=Instant.parse(data.getString("server_time")).toEpochMilli();
            long expiry=Instant.parse(data.getString("expires_at")).toEpochMilli();
            // Challenge TTL is bounded to the backend's 120-second profile; reject stale/future clock drift.
            if (expiry <= server || expiry-server > 120_000)
                return null;
            return new MobileChallenge(request,id,nonce,server,expiry,receivedElapsedRealtimeMillis);
        } catch (Exception malformed) { return null; }
    }

    /** Consumes the nonce once; on retry the caller must obtain a new challenge and proof. */
    public long consumeForProof(long nowElapsedRealtimeMillis) {
        long elapsedAge=nowElapsedRealtimeMillis-receivedElapsedRealtimeMillis;
        if (elapsedAge < 0 || elapsedAge >= expiresAtMillis-serverTimeMillis ||
                !consumed.compareAndSet(false,true)) throw new SecurityException("Challenge is stale or already consumed");
        return (serverTimeMillis + elapsedAge)/1000L;
    }
    private static boolean exact(JSONObject object,String... names) {
        if (object.length()!=names.length) return false;
        for(String name:names) if(!object.has(name)) return false;
        return true;
    }
}
