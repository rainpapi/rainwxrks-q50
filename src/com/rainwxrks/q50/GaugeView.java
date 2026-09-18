package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * RainWxrks Q50 V2 home dashboard.
 * MPH is the primary readout. RPM is intentionally not shown on HOME.
 */
public class GaugeView extends View {
    private static final int COOLANT=14, OILT=15, OILP=16, SPEED=17, THROTTLE=23;
    public static float OILP_RAW_TO_PSI=145.0377f;
    public static float SPEED_RAW_TO_MPH=0.621371f;

    private final float[] v=new float[64]; private final boolean[] have=new boolean[64];
    private final VehicleData data=new VehicleData();
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final RectF r=new RectF();
    private final Listener listener;
    private String status="";
    private static final int BG=Color.rgb(8,9,13), CARD=Color.rgb(15,16,23), EDGE=Color.rgb(43,36,62);
    private static final int WHITE=Color.WHITE, MUTED=Color.rgb(116,118,130), PURPLE=Color.rgb(188,78,255);
    private static final int BLUE=Color.rgb(103,164,255), GREEN=Color.rgb(39,210,143), ORANGE=Color.rgb(255,169,76);

    public interface Listener { void onRainCheck(); void onPerformance(); void onSettings(); }
    public GaugeView(Context c, Listener l){super(c);listener=l;setBackgroundColor(BG);}

    public void setName(int t,String n) {}
    public boolean haveType(int t){ return t>=0 && t<64 && have[t]; }
    public float valueOf(int t){ return t>=0 && t<64 ? v[t] : 0f; }
    public void setValue(int t,float val){if(t>=0&&t<64){v[t]=val;have[t]=true;}}
    public void setStatus(String s){status=s;}
    public void setExtraData(float boostPsi,boolean hb,float iatC,boolean hi,float voltage,boolean hv){data.speedMph=g(SPEED)*SPEED_RAW_TO_MPH;data.hasSpeed=h(SPEED);data.boostPsi=boostPsi;data.hasBoost=hb;data.iatC=iatC;data.hasIat=hi;data.voltage=voltage;data.hasVoltage=hv;}
    private float g(int t){return have[t]?v[t]:0f;} private boolean h(int t){return have[t];}

    public void seedDemo(){setValue(SPEED,65f/0.621371f);setValue(COOLANT,86f);setValue(OILT,93f);setValue(OILP,0.42f);setValue(THROTTLE,42f);setExtraData(12.4f,true,31f,true,14.2f,true);status="DEMO (no vehicle bus)";}

    private void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
    private void card(Canvas c,float l,float t,float rr,float b){r.set(l,t,rr,b);p.setStyle(Paint.Style.FILL);p.setColor(CARD);c.drawRoundRect(r,18,18,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(EDGE);c.drawRoundRect(r,18,18,p);p.setStyle(Paint.Style.FILL);}
    private void metric(Canvas c,int x,int y,int w,int h,String label,String value,String unit,int accent){card(c,x,y,x+w,y+h);txt(c,label,x+18,y+25,9,accent,true);txt(c,value,x+18,y+58,25,WHITE,true);if(unit!=null)txt(c,unit,x+18,y+78,9,MUTED,false);}
    private void button(Canvas c,int x,int y,int w,int h,String label,int accent){card(c,x,y,x+w,y+h);txt(c,label,x+18,y+h/2+5,11,accent,true);}

    @Override protected void onDraw(Canvas c){int W=getWidth(),H=getHeight();c.drawColor(BG);txt(c,"RAINWX RKS",22,29,20,PURPLE,true);txt(c,"// Q50 V2",132,29,8,MUTED,false);p.setColor(GREEN);c.drawCircle(W-62,24,3,p);txt(c,"LIVE",W-52,27,8,MUTED,false);p.setColor(Color.rgb(35,35,46));c.drawRect(0,49,W,50,p);
        int m=18,gap=12; int top=64; int speedW=Math.min(500,(W*54)/100); int rightX=m+speedW+gap; int rightW=W-rightX-m; int cardH=H-top-108;
        card(c,m,top,m+speedW,top+cardH);txt(c,"CURRENT SPEED",m+28,top+31,10,PURPLE,true);
        String speed=h(SPEED)?fmt(g(SPEED)*SPEED_RAW_TO_MPH):"--";p.setTypeface(Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD));p.setTextSize(Math.min(112f,speedW*.27f));p.setColor(WHITE);float tw=p.measureText(speed);c.drawText(speed,m+(speedW-tw)/2f,top+cardH*.58f,p);txt(c,"MPH",m+28,top+cardH-34,11,MUTED,true);
        int rh=(cardH-gap)/2; int half=(rightW-gap)/2;
        metric(c,rightX,top,half,rh,"BOOST",data.hasBoost?fmt1(data.boostPsi):"--",data.hasBoost?"PSI":null,PURPLE);
        metric(c,rightX+half+gap,top,half,rh,"IAT",data.hasIat?fmt0(data.iatC):"--",data.hasIat?"°C":null,BLUE);
        metric(c,rightX,top+rh+gap,half,rh,"COOLANT",h(COOLANT)?fmt0(g(COOLANT)):"--",h(COOLANT)?"°C":null,ORANGE);
        metric(c,rightX+half+gap,top+rh+gap,half,rh,"VOLTAGE",data.hasVoltage?fmt1(data.voltage):"--",data.hasVoltage?"V":null,GREEN);
        int y=top+cardH+14; int bw=(W-2*m-2*gap)/3; button(c,m,y,bw,42,"RAIN CHECK",PURPLE);button(c,m+bw+gap,y,bw,42,"PERFORMANCE",GREEN);button(c,m+2*(bw+gap),y,bw,42,"SETTINGS",WHITE);
    }
    private String fmt(float f){return String.valueOf(Math.round(f));} private String fmt0(float f){return String.valueOf(Math.round(f));} private String fmt1(float f){return String.format(java.util.Locale.US,"%.1f",f);}

    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();int W=getWidth(),m=18,gap=12,bw=(W-36-2*gap)/3;int top=64;int cardH=getHeight()-top-108;int by=top+cardH+14;if(y>=by&&y<=by+42){if(x<m+bw)listener.onRainCheck();else if(x<m+2*(bw+gap))listener.onPerformance();else listener.onSettings();}return true;}
}
