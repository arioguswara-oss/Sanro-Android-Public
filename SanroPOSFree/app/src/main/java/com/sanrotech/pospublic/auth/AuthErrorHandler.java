package com.sanrotech.pospublic.auth;

import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Strict adapter for the backend Error envelope. Unknown actions are security failures. */
public final class AuthErrorHandler {
    public enum Action { NONE, LOGIN, VERIFY_EMAIL, RESUME_REGISTRATION, RECOVER_DEVICE, WAIT,
        RETRY, SUBSCRIPTION, GET_CHALLENGE, LOCK_LOCAL_DATA }
    public static final class Result {
        public final String code, requestId;
        public final Action action;
        public final boolean retryable;
        private Result(String code, String requestId, Action action, boolean retryable) {
            this.code=code; this.requestId=requestId; this.action=action; this.retryable=retryable;
        }
    }
    private static final Set<String> CODES = new HashSet<>(Arrays.asList(
            "AUTH_INVALID", "EMAIL_VERIFICATION_REQUIRED", "REGISTRATION_INCOMPLETE", "ACCOUNT_SUSPENDED",
            "TENANT_SUSPENDED", "PRODUCT_SUSPENDED", "SUBSCRIPTION_INACTIVE", "POS_ACTIVATION_PENDING",
            "PLAN_UNSUPPORTED", "AUTHORITY_SCHEMA_INVALID", "CONTROL_PLANE_UNAVAILABLE", "DEVICE_PROOF_INVALID",
            "DEVICE_BINDING_MISMATCH", "ACTIVE_SESSION_EXISTS", "DEVICE_RECOVERY_REQUIRED", "TRANSFER_PENDING",
            "READ_ONLY", "LEASE_POLICY_UNAPPROVED", "REFRESH_REUSED", "TOKEN_EXPIRED", "IDEMPOTENCY_CONFLICT",
            "RECOVERY_STALE", "NONCE_REQUIRED", "VALIDATION_ERROR", "TOO_MANY_ATTEMPTS", "DEPENDENCY_UNAVAILABLE"));
    public Result parse(String body) {
        try {
            JSONObject envelope = new JSONObject(body);
            if (envelope.optBoolean("ok", true) || !present(envelope.optString("request_id", null))) return null;
            JSONObject error = envelope.getJSONObject("error");
            String code=error.getString("code"), action=error.getString("action");
            if (!CODES.contains(code)) return null;
            return new Result(code, envelope.getString("request_id"), Action.valueOf(action), error.getBoolean("retryable"));
        } catch (Exception invalid) { return null; }
    }
    public boolean apply(String body, AuthorizationPolicy policy) {
        Result result=parse(body);
        if (result == null) { policy.lock(); return false; }
        if (result.action == Action.LOCK_LOCAL_DATA) policy.lock();
        return true;
    }
    private static boolean present(String v) { return v != null && !v.trim().isEmpty() && !"null".equals(v); }
}
