package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;

/** Shared Rain Way visual system for the Q50 screens. */
public final class RainUI {
    public static final int BG=Color.rgb(4,5,9);
    public static final int PANEL=Color.rgb(10,11,17);
    public static final int PANEL2=Color.rgb(16,16,25);
    public static final int EDGE=Color.rgb(54,43,78);
    public static final int WHITE=Color.WHITE;
    public static final int MUTED=Color.rgb(150,146,166);
    public static final int PURPLE=Color.rgb(184,72,255);
    public static final int PURPLE2=Color.rgb(122,43,220);
    public static final int BLUE=Color.rgb(95,155,255);
    public static final int GREEN=Color.rgb(48,220,150);
    public static final int RED=Color.rgb(255,82,96);
    public static final int ORANGE=Color.rgb(255,173,76);

    private RainUI() {}

    public static void text(Canvas c, Paint p, String s, float x, float y, float size, int color, boolean bold) {
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(s,x,y,p);
    }
    public static void mono(Canvas c, Paint p, String s, float x, float y, float size, int color, boolean bold) {
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(s,x,y,p);
    }
    public static void round(Canvas c, Paint p, float l,float t,float r,float b,float radius,int fill,int stroke) {
        p.setStyle(Paint.Style.FILL); p.setColor(fill); c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);
        if(stroke!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(stroke);c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);}
        p.setStyle(Paint.Style.FILL);
    }
    public static void background(Canvas c, Bitmap bitmap) {
        c.drawColor(BG);
        if(bitmap!=null){
            Paint bp=new Paint(Paint.ANTI_ALIAS_FLAG); bp.setAlpha(115);
            RectF dst=new RectF(0,0,c.getWidth(),c.getHeight()); c.drawBitmap(bitmap,null,dst,bp);
            pOverlay.setColor(Color.argb(150,0,0,8)); c.drawRect(dst,pOverlay);
        }
        pOverlay.setColor(Color.argb(65,112,35,170)); c.drawRect(0,0,c.getWidth(),c.getHeight(),pOverlay);
    }
    private static final Paint pOverlay=new Paint();

    public static Bitmap load(Context c,int resId){
        try{return BitmapFactory.decodeResource(c.getResources(),resId);}catch(Throwable t){return null;}
    }

    public static int navWidth(int w){return Math.max(142,Math.min(178,w/5));}
    public static void header(Canvas c, Paint p, int w, String title, String subtitle) {
        int nav=navWidth(w);
        text(c,p,"♛ RAINWXRKS",18,28,15,WHITE,true);
        text(c,p,"‹",nav+14,28,25,MUTED,false);
        text(c,p,title,nav+48,30,22,WHITE,true);
        if(subtitle!=null) mono(c,p,subtitle,nav+50,50,8,MUTED,false);
        p.setColor(Color.argb(110,170,60,255));c.drawRect(0,57,w,58,p);
        text(c,p,"6:24 PM",w-86,28,9,WHITE,false);
    }

    public static void sidebar(Canvas c, Paint p, int w, int h, int selected) {
        int nav=navWidth(w); p.setColor(Color.argb(210,3,4,8));c.drawRect(0,58,nav,h,p);
        p.setColor(Color.rgb(31,28,39));c.drawRect(nav,58,nav+1,h,p);
        String[] labels={"HOME","RAIN CHECK","PERFORMANCE","CAMERA","SETTINGS"};
        String[] glyph={"⌂","♙","↗","▣","⚙"};
        for(int i=0;i<labels.length;i++){
            float y=84+i*62;
            if(i==selected) round(c,p,10,y,nav-10,y+48,9,Color.rgb(57,19,93),Color.rgb(163,55,255));
            text(c,p,glyph[i],22,y+30,19,i==selected?WHITE:MUTED,true);
            text(c,p,labels[i],50,y+29,9,i==selected?WHITE:MUTED,true);
        }
        text(c,p,"♛",nav/2f-9,h-34,24,PURPLE,true);
    }

    public static void bottomTag(Canvas c, Paint p, int w, int h, String s){
        mono(c,p,s,w/2f-p.measureText(s)/2f,h-16,8,PURPLE,true);
    }

    public static boolean navTap(MotionEvent e,int selected,Listener l,int w){
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        float y=e.getY(); int nav=navWidth(w);
        if(e.getX()<nav && y>=75){
            int i=(int)((y-75)/62); if(i>=0&&i<5){
                if(i==0)l.home(); else if(i==1)l.rain(); else if(i==2)l.performance(); else if(i==3)l.camera(); else l.settings();
                return true;
            }
        }
        if(e.getX()>=nav && y<58){l.home();return true;}
        return false;
    }
    public interface Listener { void home(); void rain(); void performance(); void camera(); void settings(); void setExhaustEnabled(boolean enabled); void setDriveMode(String mode, float raw); }
}
