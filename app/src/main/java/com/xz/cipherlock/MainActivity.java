package com.xz.cipherlock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private static final long REMINDER_INTERVAL_MS = 2L * 60 * 60 * 1000; // 2 hours
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    private WebView webView;
    private volatile boolean backButtonEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        hideSystemBars();

        // Opening the app clears any pending "explicit exit" state from a
        // previous session so the accessibility service protects the new
        // session as expected.
        PrefsHelper.setExplicitExit(this, false);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AppInterface(), "AndroidApp");
        setContentView(webView);

        webView.loadUrl("file:///android_asset/index.html");

        checkAndRequestAccessibility();
        requestNotificationPermissionIfNeeded();
        scheduleReminder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onBackPressed() {
        if (backButtonEnabled) {
            super.onBackPressed();
        }
        // Otherwise: swallow the back press. The child cannot leave the
        // screen this way; only the in-app exit action (gated by a parent
        // PIN in index.html) can.
    }

    // ---- Accessibility permission -----------------------------------

    private void checkAndRequestAccessibility() {
        if (isAccessibilityServiceEnabled()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("অনুমতি প্রয়োজন")
                .setMessage("এই অ্যাপটি ঠিকমতো কাজ করতে Accessibility অনুমতি প্রয়োজন।")
                .setCancelable(false)
                .setPositiveButton("সেটিংসে যান", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String expected = getPackageName() + "/" + MyAccessibilityService.class.getName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        for (String service : enabledServices.split(":")) {
            if (service.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    // ---- Notification permission + scheduling -------------------------

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        String permission = "android.permission.POST_NOTIFICATIONS";
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{permission}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void scheduleReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = System.currentTimeMillis() + REMINDER_INTERVAL_MS;
        // Inexact repeating alarm: no SCHEDULE_EXACT_ALARM permission
        // needed, good enough for a periodic reminder.
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, triggerAt, REMINDER_INTERVAL_MS, pendingIntent);
    }

    // ---- JS bridge ------------------------------------------------------

    /**
     * Exposed to assets/index.html as window.AndroidApp. Keep this surface
     * small and non-destructive: it can only end the current session and
     * toggle whether the hardware back button is allowed to work, nothing
     * more privileged than that.
     */
    public class AppInterface {

        @JavascriptInterface
        public void exitApp() {
            PrefsHelper.setExplicitExit(MainActivity.this, true);
            runOnUiThread(MainActivity.this::finish);
        }

        @JavascriptInterface
        public void setBackButtonEnabled(boolean enabled) {
            backButtonEnabled = enabled;
        }
    }
}
