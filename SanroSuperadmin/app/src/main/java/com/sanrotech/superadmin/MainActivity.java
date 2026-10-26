package com.sanrotech.superadmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;
import android.window.OnBackInvokedDispatcher;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Private native shell for the existing production SANRO Superadmin. No local account DB. */
public final class MainActivity extends Activity {
    private WebView webView;
    private LinearLayout errorView;
    private TextView errorDetails;
    private boolean pageLoadFailed;
    private boolean dashboardSessionReached;
    private boolean loginFormVisited;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.sanro_background));
        getWindow().setNavigationBarColor(getColor(R.color.sanro_background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.sanro_background));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        webView = new WebView(this);
        webView.setBackgroundColor(getColor(R.color.sanro_background));
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setupWebView();
        createErrorView(root);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
        webView.loadUrl(NavigationPolicy.START_URL);
    }

    private void setupWebView() {
        WebView.setWebContentsDebuggingEnabled(false);
        // Android's selected password manager may autofill the WebView HTML fields.
        // Never store or inspect credentials in native app code.
        webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Required by the production dashboard navigation guard.
        settings.setDomStorageEnabled(false); // No PWA/offline client state required.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return !NavigationPolicy.isInternal(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !NavigationPolicy.isInternal(url);
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (!NavigationPolicy.isInternal(url)) {
                    view.stopLoading();
                    showError(getString(R.string.ssl_error_detail));
                    return;
                }
                pageLoadFailed = false;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (pageLoadFailed || !NavigationPolicy.isInternal(url)) return;
                if (NavigationPolicy.isLogin(url)) {
                    loginFormVisited = true;
                    dashboardSessionReached = false;
                    view.clearHistory(); // Logout/expiry cannot navigate back into a protected page.
                } else if (NavigationPolicy.isDashboard(url) && !dashboardSessionReached) {
                    // Save is offered only after confirmed navigation to authenticated dashboard.
                    // The Autofill provider decides whether a save prompt is appropriate.
                    if (loginFormVisited) {
                        AutofillManager autofill = getSystemService(AutofillManager.class);
                        if (autofill != null && autofill.isEnabled()) autofill.commit();
                        loginFormVisited = false;
                    }
                    dashboardSessionReached = true;
                    view.clearHistory(); // Do not keep the login form in Android Back history.
                }
                errorView.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError(getString(R.string.network_error_detail));
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); // NEVER ignore certificate or hostname failures.
                showError(getString(R.string.ssl_error_detail));
            }
        });
    }

    private void createErrorView(FrameLayout root) {
        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        int pad = dp(24);
        errorView.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText(R.string.network_error_title);
        title.setTextSize(22);
        title.setTextColor(getColor(R.color.sanro_text));
        title.setGravity(Gravity.CENTER);
        errorView.addView(title, new LinearLayout.LayoutParams(-1, -2));
        errorDetails = new TextView(this);
        errorDetails.setText(R.string.network_error_detail);
        errorDetails.setGravity(Gravity.CENTER);
        errorDetails.setTextSize(16);
        errorDetails.setTextColor(getColor(R.color.sanro_text));
        LinearLayout.LayoutParams detailLayout = new LinearLayout.LayoutParams(-1, -2);
        detailLayout.topMargin = dp(16);
        errorView.addView(errorDetails, detailLayout);
        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setOnClickListener(v -> {
            pageLoadFailed = false;
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(NavigationPolicy.START_URL);
        });
        LinearLayout.LayoutParams retryLayout = new LinearLayout.LayoutParams(-2, -2);
        retryLayout.topMargin = dp(24);
        errorView.addView(retry, retryLayout);
        errorView.setVisibility(View.GONE);
        root.addView(errorView, new FrameLayout.LayoutParams(-1, -1));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void showError(String detail) {
        pageLoadFailed = true;
        errorDetails.setText(detail);
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }
    private void handleBack() {
        if (errorView.getVisibility() == View.VISIBLE) {
            showExitDialog();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            showExitDialog();
        }
    }
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() { handleBack(); }
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.exit, (dialog, which) -> finish())
                .show();
    }
    @Override protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }
    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
        }
        super.onDestroy();
    }
}
