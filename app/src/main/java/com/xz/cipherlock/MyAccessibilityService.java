package com.xz.cipherlock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Watches for the user switching to another app. If the in-app "exit"
 * action has not been used (see PrefsHelper), it brings MainActivity back
 * to the foreground. This is the mechanism that keeps a child inside the
 * controlled screen; it requires the user to grant the Accessibility
 * permission in Settings.
 */
public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg == null || pkg.toString().equals(getPackageName())) {
            return;
        }
        if (PrefsHelper.isExplicitExit(this)) {
            return;
        }

        Intent bringToFront = new Intent(this, MainActivity.class);
        bringToFront.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(bringToFront);
    }

    @Override
    public void onInterrupt() {
        // No-op.
    }
}
