package com.sanrotech.pospublic.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class AuthErrorHandlerTest {
    @Test public void parsesLockLocalDataAsExplicitBackendAction() {
        String response="{\"ok\":false,\"request_id\":\"r1\",\"error\":{\"code\":\"REFRESH_REUSED\",\"retryable\":false,\"action\":\"LOCK_LOCAL_DATA\"}}";
        AuthErrorHandler.Result result=new AuthErrorHandler().parse(response);
        assertNotNull(result); assertEquals(AuthErrorHandler.Action.LOCK_LOCAL_DATA,result.action);
    }
    @Test public void unknownOrMalformedBackendErrorsFailClosed() {
        AuthErrorHandler h=new AuthErrorHandler();
        assertNull(h.parse("{\"ok\":false,\"request_id\":\"r\",\"error\":{\"code\":\"FUTURE\",\"retryable\":false,\"action\":\"NONE\"}}"));
        assertNull(h.parse("{}"));
    }
}
