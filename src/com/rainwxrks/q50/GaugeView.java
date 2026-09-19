package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * RAINWX RKS live Q50 dashboard.
 *
 * No fake/demo values are rendered. A gauge displays "--" until a real
 * vehicle source supplies that value.
 */
public class GaugeView extends android.view.View {
    public static final int SPEED = 0;
    public static final int COOLANT = 1;
    public static final int OILT = 2;
    public static final int OILP = 3;
    public static final int RPM = 4;
    public static final int THROTTLE = 5;
    public static final int TRANS_TEMP = 6;
    public static final int FUEL = 7;

    public static final float SPEED_RAW_TO_MPH = 0.621371f;

    private final float[] v = new float[16];
    private final boolean[] have = new boolean[16];
    private final VehicleData data = new VehicleData();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RainUI.Listener nav;

    private android.graphics.Bitmap bg;
    private String status = "WAITING FOR VEHICLE DATA";
    private String driveMode = "UNKNOWN";

    public GaugeView(Context c, RainUI.Listener l) {
        super(c);
        nav = l;
        bg = RainUI.load(c, com.rainwxrks.q50.R.drawable.bg);
        setFocusable(true);
    }

    public boolean haveType(int t) {
        return t >= 0 && t < have.length && have[t];
    }

    public float valueOf(int t) {
        return t >= 0 && t < v.length ? v[t] : 0f;
    }

    public void setValue(int t, float x) {
        if (t >= 0 && t < v.length && finite(x)) {
            v[t] = x;
            have[t] = true;
        }
    }

    public void setStatus(String s) {
        status = s == null ? "" : s;
        invalidate();
    }

    public void setDriveMode(String mode) {
        driveMode = mode == null ? "UNKNOWN" : mode;
        invalidate();
    }

    public void setExtraData(
            float boostPsi, boolean hasBoost,
            float iatC, boolean hasIat,
            float voltage, boolean hasVoltage) {

        data.boostPsi = boostPsi;
        data.hasBoost = hasBoost;
        data.iatC = iatC;
        data.hasIat = hasIat;
        data.voltage = voltage;
        data.hasVoltage = hasVoltage;
    }

    private String f0(float x) {
        return String.valueOf(Math.round(x));
    }

    private String f1(float x) {
        return String.format(java.util.Locale.US, "%.1f", x);
    }

    private float f(float c) {
        return c * 9f / 5f + 32f;
    }

    private void card(Canvas c, float l, float t, float rr, float b) {
        RainUI.round(c, p, l, t, rr, b, 14, RainUI.PANEL, RainUI.EDGE);
    }

    private void metric(
            Canvas c, float l, float t, float w, float h,
            String label, String val, String unit, int accent) {

        card(c, l, t, l + w, t + h);
        RainUI.mono(c, p, label, l + 12, t + 21, 8, accent, true);
        RainUI.text(c, p, val, l + 12, t + 51, 23, RainUI.WHITE, true);

        if (unit != null) {
            RainUI.mono(c, p, unit, l + 12, t + h - 11, 8, RainUI.MUTED, false);
        }
    }

    private void button(Canvas c, float l, float t, float w, String s, int accent) {
        RainUI.round(c, p, l, t, l + w, t + 44, 10,
                RainUI.PANEL2, RainUI.EDGE);
        RainUI.text(c, p, s, l + 14, t + 28, 9, accent, true);
    }

    @Override protected void onDraw(Canvas c) {
        int W = getWidth();
        int H = getHeight();

        RainUI.background(c, bg);
        RainUI.header(c, p, W, "HOME", "LIVE Q50 DATA. NO FICTION.");
        RainUI.sidebar(c, p, W, H, 0);

        int navW = RainUI.navWidth(W);
        int left = navW + 16;
        int top = 72;
        int right = W - 18;
        int contentW = right - left;

        RainUI.text(c, p, "RAINWX RKS", left, top + 20, 16, RainUI.PURPLE, true);
        RainUI.mono(c, p, "DRIVE MODE  " + driveMode,
                left, top + 42, 8, RainUI.WHITE, true);
        RainUI.mono(c, p, status,
                left, top + 58, 7, RainUI.MUTED, false);

        int speedTop = top + 70;
        int bottomButtons = 58;
        int speedH = Math.max(230, H - speedTop - bottomButtons - 8);
        int speedW = (int) (contentW * 0.55f);

        int sideX = left + speedW + 12;
        int sideW = right - sideX;
        int gap = 9;
        int rowH = (speedH - gap * 2) / 3;
        int colW = (sideW - gap) / 2;

        card(c, left, speedTop, left + speedW, speedTop + speedH);
        RainUI.mono(c, p, "CURRENT SPEED",
                left + 20, speedTop + 28, 9, RainUI.PURPLE, true);

        String speed = haveType(SPEED)
                ? f0(valueOf(SPEED) * SPEED_RAW_TO_MPH)
                : "--";

        p.setTextSize(Math.min(108, speedW * 0.30f));
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setColor(RainUI.WHITE);

        float tw = p.measureText(speed);
        c.drawText(speed,
                left + (speedW - tw) / 2f,
                speedTop + speedH * 0.53f,
                p);

        RainUI.mono(c, p, "MPH",
                left + 20, speedTop + speedH - 22, 10, RainUI.MUTED, true);

        String rpm = haveType(RPM) ? f0(valueOf(RPM)) : "--";
        RainUI.mono(c, p, "RPM  " + rpm,
                left + 20, speedTop + speedH - 42, 9, RainUI.WHITE, true);

        metric(c, sideX, speedTop, colW, rowH,
                "BOOST",
                data.hasBoost ? f1(data.boostPsi) : "--",
                data.hasBoost ? "PSI" : null,
                RainUI.PURPLE);

        metric(c, sideX + colW + gap, speedTop, colW, rowH,
                "IAT",
                data.hasIat ? f0(f(data.iatC)) : "--",
                data.hasIat ? "°F" : null,
                RainUI.BLUE);

        metric(c, sideX, speedTop + rowH + gap, colW, rowH,
                "COOLANT",
                haveType(COOLANT) ? f0(f(valueOf(COOLANT))) : "--",
                haveType(COOLANT) ? "°F" : null,
                RainUI.ORANGE);

        metric(c, sideX + colW + gap, speedTop + rowH + gap, colW, rowH,
                "OIL TEMP",
                haveType(OILT) ? f0(f(valueOf(OILT))) : "--",
                haveType(OILT) ? "°F" : null,
                RainUI.PURPLE);

        metric(c, sideX, speedTop + (rowH + gap) * 2, colW, rowH,
                "OIL PRESS",
                haveType(OILP) ? f1(valueOf(OILP)) : "--",
                haveType(OILP) ? "PSI" : null,
                RainUI.ORANGE);

        metric(c, sideX + colW + gap, speedTop + (rowH + gap) * 2, colW, rowH,
                "VOLTAGE",
                data.hasVoltage ? f1(data.voltage) : "--",
                data.hasVoltage ? "V" : null,
                RainUI.GREEN);

        int by = H - 58;
        int bw = (contentW - 20) / 3;

        button(c, left, by, bw, "RAIN CHECK", RainUI.PURPLE);
        button(c, left + bw + 10, by, bw, "PERFORMANCE", RainUI.GREEN);
        button(c, left + 2 * (bw + 10), by, bw, "SETTINGS", RainUI.WHITE);

        RainUI.bottomTag(c, p, W, H,
                "LIVE VALUES ONLY   •   OIL + COOLANT RESTORED   ♛");
    }

    private static boolean finite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (RainUI.navTap(e, 0, nav, getWidth())) return true;

        if (e.getAction() == MotionEvent.ACTION_UP) {
            int W = getWidth();
            int H = getHeight();
            int navW = RainUI.navWidth(W);
            int left = navW + 16;
            int contentW = W - left - 18;
            int bw = (contentW - 20) / 3;
            int by = H - 58;

            if (e.getY() >= by && e.getY() <= by + 44) {
                if (e.getX() < left + bw) {
                    nav.rain();
                } else if (e.getX() < left + 2 * (bw + 10)) {
                    nav.performance();
                } else {
                    nav.settings();
                }
            }
        }

        return true;
    }
}
