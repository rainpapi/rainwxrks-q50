package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class PerformanceView extends View {
    public interface Listener { void onBack(); }
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private Listener listener;
    private static final int BG=Color.rgb(8,9,13), WHITE=Color.WHITE, MUTED=Color.rgb(116,118,130), PURPLE=Color.rgb(188,78,255);
    public PerformanceView(Context c, Listener l){super(c);listener=l;setBackgroundColor(BG);}
    private void t(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
    @Override protected void onDraw(Canvas c){c.drawColor(BG); int w=getWidth(); t(c,"PERFORMANCE",24,34,22,PURPLE,true); t(c,"TIMERS & DATA LOGGING",24,56,9,MUTED,false);
        String[] a={"0–60 MPH","0–100 MPH","1/8 MILE","1/4 MILE","G-FORCE","DRIVE RECORDER"};
        for(int i=0;i<a.length;i++){float y=88+i*52;p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(43,36,62));c.drawRoundRect(24,y,w-24,y+38,12,12,p);t(c,a[i],42,y+25,12,WHITE,true);} }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP&&e.getY()<70)listener.onBack();return true;}
}
