package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** Rain Check diagnostic UI. Vehicle DTC transport is intentionally not faked. */
public class RainCheckView extends View {
    public interface Listener { void onBack(); void onScan(); void onClear(); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private Listener listener;
    private String status = "READY TO SCAN";
    private int codeCount = 0;

    private static final int BG=Color.rgb(8,9,13), CARD=Color.rgb(15,16,23), EDGE=Color.rgb(43,36,62);
    private static final int WHITE=Color.WHITE, MUTED=Color.rgb(116,118,130), PURPLE=Color.rgb(188,78,255);
    private static final int GREEN=Color.rgb(39,210,143), RED=Color.rgb(255,92,104);

    public RainCheckView(Context c, Listener l) { super(c); listener=l; setBackgroundColor(BG); }
    public void setResult(String s, int count) { status=s; codeCount=count; invalidate(); }

    private void text(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(s,x,y,p);
    }
    private void button(Canvas c, float x, float y, float w, float h, String label, int accent) {
        r.set(x,y,x+w,y+h); p.setStyle(Paint.Style.FILL); p.setColor(CARD); c.drawRoundRect(r,14,14,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(EDGE); c.drawRoundRect(r,14,14,p);
        text(c,label,x+22,y+h/2+6,14,accent,true); p.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas c) {
        int w=getWidth(), h=getHeight(); c.drawColor(BG);
        text(c,"RAIN CHECK",24,34,22,PURPLE,true);
        text(c,"VEHICLE DIAGNOSTICS",24,56,9,MUTED,false);
        r.set(24,78,w-24,188); p.setColor(CARD); c.drawRoundRect(r,18,18,p);
        text(c,codeCount==0 ? "NO CODES REPORTED" : (codeCount+" CODES FOUND"),48,121,22,codeCount==0?GREEN:RED,true);
        text(c,status,48,149,10,MUTED,false);
        button(c,24,210,(w-72)/2,58,"SCAN VEHICLE",PURPLE);
        button(c,48+(w-72)/2,210,(w-72)/2,58,"CLEAR CODES",RED);
        button(c,24,282,w-48,58,"SCAN HISTORY",WHITE);
        text(c,"Rain Check will only display codes actually returned by the vehicle interface.",24,h-30,8,MUTED,false);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=e.getX(), y=e.getY(); int w=getWidth();
        if(y<70) { listener.onBack(); return true; }
        if(y>=210 && y<268) {
            if(x < w/2) listener.onScan(); else listener.onClear();
            return true;
        }
        return true;
    }
}
