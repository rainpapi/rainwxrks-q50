package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

/**
 * RAINWX RKS settings.
 *
 * Removed the synthetic Exhaust Note control. It was not actual vehicle
 * exhaust control and therefore did not belong in a diagnostic dashboard.
 */
public class SettingsView extends android.view.View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RainUI.Listener nav;

    private android.graphics.Bitmap bg;
    private boolean metric = false;
    private boolean alerts = true;
    private boolean autoStart = true;

    private String driveMode = "UNKNOWN";
    private float driveModeRaw = Float.NaN;

    public SettingsView(Context c, RainUI.Listener n) {
        super(c);
        nav = n;
        bg = RainUI.load(c, com.rainwxrks.q50.R.drawable.rain_bg);
    }

    public void setDriveMode(String mode, float raw) {
        driveMode = mode == null ? "UNKNOWN" : mode;
        driveModeRaw = raw;
        invalidate();
    }

    private void row(Canvas c, int x, int y, int w,
                     String label, String value) {
        RainUI.round(c, p, x, y, x + w, y + 48,
                10, RainUI.PANEL2, RainUI.EDGE);
        RainUI.text(c, p, label, x + 18, y + 30,
                11, RainUI.WHITE, true);
        RainUI.text(c, p, value, x + w - 160, y + 30,
                9, RainUI.MUTED, false);
        RainUI.text(c, p, "›", x + w - 24, y + 30,
                18, RainUI.MUTED, false);
    }

    @Override protected void onDraw(Canvas c) {
        int W = getWidth();
        int H = getHeight();

        RainUI.background(c, bg);
        RainUI.header(c, p, W, "SETTINGS", "MAKE IT YOURS.");
        RainUI.sidebar(c, p, W, H, 4);

        int x = RainUI.navWidth(W) + 18;
        int right = W - 18;
        int w = right - x;

        row(c, x, 70,  w, "UNITS", metric ? "KM/H / °C" : "MPH / °F");
        row(c, x, 126, w, "GAUGE CONFIGURATION", "HOME");
        row(c, x, 182, w, "ALERTS & WARNINGS", alerts ? "ENABLED" : "OFF");
        row(c, x, 238, w, "THEME", "RAIN WAY   ♛");
        row(c, x, 294, w, "AUTO-START", autoStart ? "APP GARAGE" : "OFF");
        row(c, x, 350, w, "DRIVE MODE", driveMode);
        row(c, x, 406, w, "VEHICLE DATA", "AUTO-DISCOVERY");
        row(c, x, 462, w, "ABOUT", "RAINWX RKS  V5");

        RainUI.mono(c, p,
                "LIVE DATA ONLY. UNAVAILABLE SIGNALS STAY --.",
                x, 518, 7, RainUI.MUTED, false);

        RainUI.bottomTag(c, p, W, H,
                "NO CAMERA   •   NO SYNTHETIC GAUGES   ♛");
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (RainUI.navTap(e, 4, nav, getWidth())) return true;

        if (e.getAction() == MotionEvent.ACTION_UP) {
            int x = RainUI.navWidth(getWidth()) + 18;

            for (int i = 0; i < 8; i++) {
                float y = 70 + i * 56;

                if (e.getY() >= y && e.getY() < y + 48) {
                    if (i == 0) {
                        metric = !metric;
                    } else if (i == 2) {
                        alerts = !alerts;
                    } else if (i == 4) {
                        autoStart = !autoStart;
                    }

                    invalidate();
                    return true;
                }
            }
        }

        return true;
    }
}
