package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** V3 Rain Way home dashboard. */
public class GaugeView extends android.view.View {
    public static final int COOLANT=14,OILT=15,OILP=16,SPEED=17,THROTTLE=23;
    public static final float OILP_RAW_TO_PSI=145.0377f,SPEED_RAW_TO_MPH=0.621371f;
    private final float[] v=new float[64]; private final boolean[] have=new boolean[64];
    private final VehicleData data=new VehicleData(); private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final RectF r=new RectF();
    private final RainUI.Listener nav; private android.graphics.Bitmap bg; private String status="";
    public GaugeView(Context c,RainUI.Listener l){super(c);nav=l;bg=RainUI.load(c,com.rainwxrks.q50.R.drawable.bg);setFocusable(true);}
    public boolean haveType(int t){return t>=0&&t<64&&have[t];} public float valueOf(int t){return t>=0&&t<64?v[t]:0f;}
    public void setValue(int t,float x){if(t>=0&&t<64){v[t]=x;have[t]=true;}}
    public void setStatus(String s){status=s;invalidate();}
    public void setExtraData(float b,boolean hb,float i,boolean hi,float vol,boolean hv){data.boostPsi=b;data.hasBoost=hb;data.iatC=i;data.hasIat=hi;data.voltage=vol;data.hasVoltage=hv;}
    public void seedDemo(){setValue(SPEED,62f/SPEED_RAW_TO_MPH);setValue(COOLANT,86f);setValue(OILT,93f);setValue(OILP,.42f);setExtraData(18.2f,true,28.9f,true,14.2f,true);status="DEMO";}
    private String f0(float x){return String.valueOf(Math.round(x));} private String f1(float x){return String.format(java.util.Locale.US,"%.1f",x);} private float f(float c){return c*9f/5f+32f;}
    private void card(Canvas c,float l,float t,float rr,float b){RainUI.round(c,p,l,t,rr,b,14,RainUI.PANEL, RainUI.EDGE);}
    private void metric(Canvas c,float l,float t,float w,float h,String label,String val,String unit,int accent){card(c,l,t,l+w,t+h);RainUI.mono(c,p,label,l+12,t+22,8,accent,true);RainUI.text(c,p,val,l+12,t+52,24,RainUI.WHITE,true);if(unit!=null)RainUI.mono(c,p,unit,l+12,t+h-12,8,RainUI.MUTED,false);}
    private void button(Canvas c,float l,float t,float w,String s,int accent){RainUI.round(c,p,l,t,l+w,t+44,10,ColorCompat.panel(),ColorCompat.edge());RainUI.text(c,p,s,l+14,t+28,9,accent,true);}
    @Override protected void onDraw(Canvas c){int W=getWidth(),H=getHeight();RainUI.background(c,bg);RainUI.header(c,p,W,"HOME","MORE THAN A DRIVE. IT'S A RAIN WAY.");RainUI.sidebar(c,p,W,H,0);
        int navW=RainUI.navWidth(W), left=navW+16, top=72, right=W-18, contentW=right-left;
        // RainWxrks / Q50 image header
        if(bg!=null){p.setAlpha(115);c.drawBitmap(bg,null,new RectF(left,top,right,top+74),p);p.setAlpha(255);}
        RainUI.text(c,p,"RAINWX RKS",left+18,top+31,17,RainUI.PURPLE,true);RainUI.mono(c,p,"Q50S / INFINITI",right-106,top+31,7,RainUI.MUTED,true);
        int speedTop=top+84, speedH=Math.max(175,H-speedTop-74), speedW=(int)(contentW*.55f), sideX=left+speedW+12, sideW=right-sideX;
        card(c,left,speedTop,left+speedW,speedTop+speedH);
        RainUI.mono(c,p,"CURRENT SPEED",left+20,speedTop+28,9,RainUI.PURPLE,true);
        String speed=haveType(SPEED)?f0(valueOf(SPEED)*SPEED_RAW_TO_MPH):"--";
        p.setTextSize(Math.min(108,speedW*.30f));p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setColor(RainUI.WHITE);float tw=p.measureText(speed);c.drawText(speed,left+(speedW-tw)/2f,speedTop+speedH*.61f,p);
        RainUI.mono(c,p,"MPH",left+20,speedTop+speedH-20,10,RainUI.MUTED,true);
        int gap=10, mh=(speedH-gap)/2, mw=(sideW-gap)/2;
        metric(c,sideX,speedTop,mw,mh,"BOOST",data.hasBoost?f1(data.boostPsi):"--",data.hasBoost?"PSI":null,RainUI.PURPLE);
        metric(c,sideX+mw+gap,speedTop,mw,mh,"IAT",data.hasIat?f0(f(data.iatC)):"--",data.hasIat?"°F":null,RainUI.BLUE);
        metric(c,sideX,speedTop+mh+gap,mw,mh,"COOLANT",haveType(COOLANT)?f0(f(valueOf(COOLANT))):"--",haveType(COOLANT)?"°F":null,RainUI.ORANGE);
        metric(c,sideX+mw+gap,speedTop+mh+gap,mw,mh,"VOLTAGE",data.hasVoltage?f1(data.voltage):"--",data.hasVoltage?"V":null,RainUI.GREEN);
        int by=H-58,bw=(contentW-20)/3;button(c,left,by,bw,"RAIN CHECK",RainUI.PURPLE);button(c,left+bw+10,by,bw,"PERFORMANCE",RainUI.GREEN);button(c,left+2*(bw+10),by,bw,"SETTINGS",RainUI.WHITE);
        RainUI.bottomTag(c,p,W,H,"DISCIPLINE DRIVES DESTINY   ♛");
    }
    private static final class ColorCompat {static int panel(){return RainUI.PANEL2;}static int edge(){return RainUI.EDGE;}}
    @Override public boolean onTouchEvent(MotionEvent e){if(RainUI.navTap(e,0,nav,getWidth()))return true;if(e.getAction()==MotionEvent.ACTION_UP){int W=getWidth(),H=getHeight(),navW=RainUI.navWidth(W),left=navW+16,contentW=W-left-18,bw=(contentW-20)/3,by=H-58;if(e.getY()>=by&&e.getY()<=by+44){if(e.getX()<left+bw)nav.rain();else if(e.getX()<left+2*(bw+10))nav.performance();else nav.settings();}}return true;}
}
