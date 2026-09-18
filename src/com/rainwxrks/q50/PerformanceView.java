package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import java.util.Locale;

/** V3 performance timer using the live vehicle speed sensor when available. */
public class PerformanceView extends android.view.View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final RectF r=new RectF(); private final RainUI.Listener nav; private android.graphics.Bitmap bg;
    private int tab=0; private boolean running=false, armed=false, hasSpeed=false; private float speed=0f,gx=0f,gy=0f,distanceFt=0f; private long startNs=0,lastNs=0; private float elapsed=0f; private final float[] best={0,0,0,0,0};
    public PerformanceView(Context c,RainUI.Listener n){super(c);nav=n;bg=RainUI.load(c,com.rainwxrks.q50.R.drawable.homecar);setFocusable(true);}
    public void setSpeed(float mph,boolean available){speed=Math.max(0,mph);hasSpeed=available;tick();invalidate();}
    public void setGForce(float x,float y){gx=x;gy=y;invalidate();}
    private void tick(){if(!running)return;long now=System.nanoTime();if(startNs==0){startNs=now;lastNs=now;return;}float dt=(now-lastNs)/1e9f;if(dt<0||dt>1)dt=.1f;lastNs=now;elapsed=(now-startNs)/1e9f;distanceFt+=speed*1.4666667f*dt;
        if(tab==0&&speed>=60){finish(elapsed,0);}
        else if(tab==1&&speed>=100){finish(elapsed,1);}
        else if(tab==2&&distanceFt>=660){finish(elapsed,2);}
        else if(tab==3&&distanceFt>=1320){finish(elapsed,3);}
    }
    private void finish(float t,int idx){running=false;if(idx>=0&&idx<best.length&&(best[idx]==0||t<best[idx]))best[idx]=t;invalidate();}
    private void start(){running=true;armed=true;startNs=System.nanoTime();lastNs=startNs;elapsed=0;distanceFt=0;}
    private void reset(){running=false;armed=false;startNs=lastNs=0;elapsed=0;distanceFt=0;}
    private String time(){return String.format(Locale.US,"%.2f",elapsed);}
    private void tab(Canvas c,float x,float y,float w,String s,boolean on){RainUI.round(c,p,x,y,x+w,y+38,9,on?RainUI.PURPLE2:RainUI.PANEL2,on?RainUI.PURPLE:RainUI.EDGE);RainUI.text(c,p,s,x+12,y+24,9,RainUI.WHITE,true);}
    @Override protected void onDraw(Canvas c){int W=getWidth(),H=getHeight();RainUI.background(c,bg);RainUI.header(c,p,W,"PERFORMANCE","MEASURE. IMPROVE. REPEAT.");RainUI.sidebar(c,p,W,H,2);int x=RainUI.navWidth(W)+18,right=W-18,w=right-x;
        String[] tabs={"0 - 60","0 - 100","1/8 MILE","1/4 MILE"};int gap=7,tw=(w-3*gap)/4;for(int i=0;i<4;i++)tab(c,x+i*(tw+gap),76,tw,tabs[i],tab==i);
        RainUI.round(c,p,x,126,x+w*.67f,H-74,14,RainUI.PANEL,RainUI.EDGE);
        String big=running?time():(elapsed>0?time():"0.00");RainUI.mono(c,p,big,x+32,224,Math.min(72,w*.13f),RainUI.WHITE,true);
        String unit=tab<2?"SECONDS":"DISTANCE / TIME";RainUI.mono(c,p,unit,x+36,248,9,RainUI.MUTED,true);
        RainUI.text(c,p,hasSpeed?Math.round(speed)+" MPH":"-- MPH",x+36,286,14,RainUI.PURPLE,true);
        RainUI.text(c,p,""+Math.round(distanceFt)+" FT",x+170,286,10,RainUI.MUTED,false);
        float bw=w*.58f;RainUI.round(c,p,x+28,315,x+28+bw,365,12,running?RainUI.RED:RainUI.PURPLE2,running?RainUI.RED:RainUI.PURPLE);RainUI.text(c,p,running?"STOP":"START",x+28+bw/2f-25,347,12,RainUI.WHITE,true);
        RainUI.round(c,p,x+28,375,x+28+bw,417,10,RainUI.PANEL2,RainUI.EDGE);RainUI.text(c,p,"RESET",x+45,401,9,RainUI.MUTED,true);
        int rx=(int)(x+w*.70f),rw=right-rx;RainUI.round(c,p,rx,126,right,H-74,14,RainUI.PANEL,RainUI.EDGE);RainUI.text(c,p,"BEST RUNS",rx+18,153,10,RainUI.PURPLE,true);
        String[] labels={"0-60 MPH","0-100 MPH","1/8 MILE","1/4 MILE"};for(int i=0;i<4;i++){String val=best[i]==0?"--":String.format(Locale.US,"%.2fs",best[i]);RainUI.text(c,p,labels[i],rx+18,190+i*46,9,RainUI.MUTED,false);RainUI.mono(c,p,val,right-70,190+i*46,11,RainUI.WHITE,true);}
        RainUI.text(c,p,"G",rx+18,380,9,RainUI.MUTED,true);RainUI.mono(c,p,String.format(Locale.US,"%.2f / %.2f g",gx,gy),rx+32,380,10,RainUI.WHITE,true);
        RainUI.bottomTag(c,p,W,H,"FASTER THAN YESTERDAY   ♛");
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(RainUI.navTap(e,2,nav,getWidth()))return true;if(e.getAction()!=MotionEvent.ACTION_UP)return true;int x=RainUI.navWidth(getWidth())+18,w=getWidth()-x-18,gap=7,tw=(w-3*gap)/4;
        if(e.getY()>=76&&e.getY()<114){for(int i=0;i<4;i++)if(e.getX()>=x+i*(tw+gap)&&e.getX()<x+i*(tw+gap)+tw){tab=i;reset();invalidate();return true;}}
        if(e.getY()>=315&&e.getY()<365){if(running)reset();else start();return true;}if(e.getY()>=375&&e.getY()<417){reset();return true;}return true;}
}
