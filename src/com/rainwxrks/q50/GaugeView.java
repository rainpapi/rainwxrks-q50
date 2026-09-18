package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * RainWxrks Q50 dashboard.
 *
 * The vehicle/sensor plumbing is intentionally unchanged. This class only
 * controls the on-screen presentation and reads the same VS_ID_* values that
 * MainActivity supplies.
 */
public class GaugeView extends View {

    // Existing vehicle sensor types. Keep these values unchanged.
    private static final int TORQUE=12, RPM=13, COOLANT=14, OILT=15, OILP=16, SPEED=17,
            GLAT=20, GLONG=21, GEAR=22, THROTTLE=23, POWER=32,
            TP_FR=36, TP_FL=37, TP_RR=38, TP_RL=39;

    public static float OILP_RAW_TO_PSI = 145.0377f;
    public static float POWER_RAW_TO_KW = 0.0001047f;
    public static float SPEED_RAW_TO_MPH = 0.621371f;

    private static final int N = 64;
    private final float[] v = new float[N];
    private final boolean[] have = new boolean[N];
    private String status = "";

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    // RainWxrks palette based on the requested reference UI.
    private static final int BG       = Color.rgb(8, 9, 13);
    private static final int CARD     = Color.rgb(15, 16, 23);
    private static final int CARD_EDGE= Color.rgb(43, 36, 62);
    private static final int WHITE    = Color.WHITE;
    private static final int MUTED    = Color.rgb(116, 118, 130);
    private static final int PURPLE   = Color.rgb(188, 78, 255);
    private static final int PURPLE2  = Color.rgb(133, 72, 214);
    private static final int BLUE     = Color.rgb(103, 164, 255);
    private static final int GREEN    = Color.rgb(39, 210, 143);

    public GaugeView(Context c) {
        super(c);
        setBackgroundColor(BG);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    }

    public void setName(int t, String n) {}
    public void setValue(int t, float val) {
        if (t >= 0 && t < N) { v[t] = val; have[t] = true; }
    }
    public void setStatus(String s) { status = s; }
    private float g(int t) { return have[t] ? v[t] : 0f; }
    private boolean h(int t) { return have[t]; }

    /** Demo values only when the head unit has no vehicle sensor bus. */
    public void seedDemo() {
        int[] t = {TORQUE,RPM,COOLANT,OILT,OILP,SPEED,GLAT,GLONG,GEAR,THROTTLE,POWER,TP_FR,TP_FL,TP_RR,TP_RL};
        float[] val = {180f,3120f,86f,93f,0.42f,65f,0.35f,-0.20f,4f,42f,3120f*180f,38.5f,38.5f,37f,36.8f};
        for (int i=0;i<t.length;i++) setValue(t[i], val[i]);
        status = "DEMO (no vehicle bus)";
    }

    @Override
    protected void onDraw(Canvas cv) {
        final int W = getWidth();
        final int H = getHeight();
        p.setStyle(Paint.Style.FILL);
        p.setColor(BG);
        cv.drawRect(0, 0, W, H, p);

        drawHeader(cv, W);

        // Main reference layout: large MPH card on the left, two live
        // temperature cards stacked on the right.
        int margin = Math.max(10, W / 80);
        int top = 62;
        int gap = 12;
        int leftW = Math.min(500, (W * 62) / 100);
        int rightX = margin + leftW + gap;
        int rightW = W - rightX - margin;
        int cardH = H - top - margin;

        drawSpeedCard(cv, margin, top, leftW, cardH);

        int rightH = (cardH - gap) / 2;
        drawTempCard(cv, rightX, top, rightW, rightH,
                "OIL TEMP", g(OILT), "°C", 40f, 150f, PURPLE, h(OILT));
        drawTempCard(cv, rightX, top + rightH + gap, rightW, rightH,
                "WATER TEMP", g(COOLANT), "°C", 40f, 130f, BLUE, h(COOLANT));
    }

    private void drawHeader(Canvas cv, int W) {
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        p.setTextSize(20f);
        p.setColor(PURPLE);
        cv.drawText("RainWxrks", 22, 29, p);

        p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        p.setTextSize(8f);
        p.setColor(MUTED);
        cv.drawText("// Q50 V37 DIGITAL", 122, 29, p);

        // Small live indicator, intentionally unobtrusive.
        p.setColor(GREEN);
        cv.drawCircle(W - 54, 24, 3f, p);
        p.setColor(MUTED);
        p.setTextSize(8f);
        cv.drawText("LIVE CAN", W - 46, 27, p);

        p.setColor(Color.rgb(35, 35, 46));
        cv.drawRect(0, 49, W, 50, p);
    }

    private void drawCard(Canvas cv, float left, float top, float right, float bottom) {
        r.set(left, top, right, bottom);
        p.setStyle(Paint.Style.FILL);
        p.setColor(CARD);
        cv.drawRoundRect(r, 18f, 18f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(CARD_EDGE);
        cv.drawRoundRect(r, 18f, 18f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawSpeedCard(Canvas cv, int x, int y, int w, int h) {
        drawCard(cv, x, y, x+w, y+h);

        p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        p.setTextSize(10f);
        p.setColor(PURPLE);
        cv.drawText("MPH", x+28, y+34, p);

        // Large central speed number.
        String speed = h(SPEED) ? fmt0(g(SPEED) * SPEED_RAW_TO_MPH) : "--";
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        p.setTextSize(Math.min(92f, w * 0.22f));
        p.setColor(WHITE);
        float tw = p.measureText(speed);
        cv.drawText(speed, x + (w-tw)/2f, y + h*0.55f, p);

        // Bottom vehicle identifier.
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        p.setTextSize(9f);
        p.setColor(MUTED);
        cv.drawCircle(x+30, y+h-31, 4f, p);
        cv.drawText("V37 • 3.7L", x+42, y+h-28, p);

        // Purple activity strip from the reference design.
        float barX = x + w - 210;
        float barY = y + h - 35;
        for (int i=0;i<10;i++) {
            p.setColor(i < 8 ? PURPLE : Color.rgb(45,45,55));
            cv.drawRoundRect(new RectF(barX+i*20, barY, barX+i*20+14, barY+3), 2f, 2f, p);
        }
    }

    private void drawTempCard(Canvas cv, int x, int y, int w, int h,
                              String label, float value, String unit,
                              float min, float max, int accent, boolean has) {
        drawCard(cv, x, y, x+w, y+h);

        p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        p.setTextSize(10f);
        p.setColor(accent);
        cv.drawText(label, x+28, y+34, p);

        String val = has ? fmt0(value) : "--";
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        p.setTextSize(Math.min(52f, w * 0.25f));
        p.setColor(WHITE);
        cv.drawText(val, x+28, y+h*0.57f, p);

        if (has) {
            p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            p.setTextSize(17f);
            p.setColor(MUTED);
            float vx = x+28+p.measureText(val)+8;
            cv.drawText(unit, vx, y+h*0.57f, p);
        }

        // Thin accent progress line, matching the reference.
        float frac = has ? Math.max(0f, Math.min(1f, (value-min)/(max-min))) : 0f;
        p.setColor(Color.rgb(45, 39, 57));
        cv.drawRoundRect(new RectF(x+28, y+h-31, x+w-28, y+h-27), 2f, 2f, p);
        if (has) {
            p.setColor(accent);
            cv.drawRoundRect(new RectF(x+28, y+h-31, x+28+(w-56)*frac, y+h-27), 2f, 2f, p);
        }
    }

    private String fmt0(float f) { return String.valueOf(Math.round(f)); }
}
