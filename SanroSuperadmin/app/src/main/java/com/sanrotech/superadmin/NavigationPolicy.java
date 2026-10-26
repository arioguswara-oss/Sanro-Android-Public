package com.sanrotech.superadmin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Only the production Superadmin subpath may render inside the privileged WebView. */
public final class NavigationPolicy {
    public static final String START_URL = "https://sanrotech.com/superadmin/login";
    private NavigationPolicy() { }

    public static boolean isInternal(String url) {
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getUserInfo() != null) return false;
            if (uri.getHost() == null || !"sanrotech.com".equals(uri.getHost().toLowerCase(Locale.ROOT))) return false;
            if (uri.getPort() != -1 && uri.getPort() != 443) return false;
            String path = uri.getRawPath();
            if (path == null || path.indexOf('%') >= 0) return false;
            if (!path.equals(uri.normalize().getRawPath())) return false;
            return "/superadmin".equals(path) || path.startsWith("/superadmin/");
        } catch (URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isLogin(String url) {
        if (!isInternal(url)) return false;
        try {
            String path = new URI(url).getRawPath();
            return "/superadmin/login".equals(path) || "/superadmin/login/".equals(path);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean isDashboard(String url) {
        if (!isInternal(url)) return false;
        try {
            String path = new URI(url).getRawPath();
            return "/superadmin/dashboard".equals(path) || "/superadmin/dashboard/".equals(path);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
