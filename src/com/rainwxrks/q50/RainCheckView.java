package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Rain Check V3 diagnostics screen. */
public class RainCheckView extends android.view.View {
    public interface Listener { void onScan(); void onClear(); }
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final RectF r=new RectF(); private final RainUI.Listener nav; private final Listener listener;
    private android.graphics.Bitmap bg; private String status="VEHICLE CONNECTION READY"; private int count=0;
    public RainCheckView(Context c,RainUI.Listener n,Listener l){super(c);nav=n;listener=l;bg=RainUI.load(c,com.rainwxrks.q50.R.drawable.rain_bg);}
    public void setResult(String s,int n){status=s;count=n;invalidate();}
    private void btn(Canvas c,float x,float y,float w,float h,String label,int accent){RainUI.round(c,p,x,y,x+w,y+h,11,RainUI.PANEL2,RainUI.EDGE);RainUI.text(c,p,label,x+18,y+h/2+5,10,accent,true);RainUI.text(c,p,"›",x+w-24,y+h/2+6,18,RainUI.MUTED,false);}
    @Override protected void onDraw(Canvas c){int W=getWidth(),H=getHeight();RainUI.background(c,bg);RainUI.header(c,p,W,"RAIN CHECK","SCAN. CLEAR. GET BACK TO IT.");RainUI.sidebar(c,p,W,H,1);int x=RainUI.navWidth(W)+18,right=W-18;
        RainUI.round(c,p,x,76,right,155,14,RainUI.PANEL, RainUI.EDGE);
        RainUI.text(c,p,count==0?"NO CODES REPORTED":count+" CODES FOUND",x+20,111,20,count==0?RainUI.GREEN:RainUI.RED,true);
        RainUI.mono(c,p,status,x+20,136,8,RainUI.MUTED,false);
        p.setColor(count==0?RainUI.GREEN:RainUI.RED);c.drawCircle(right-28,104,5,p);
        int w=right-x; btn(c,x,172,w/2-6,58,"SCAN FOR CODES",RainUI.PURPLE);btn(c,x+w/2+6,172,w/2-6,58,"CLEAR CODES",RainUI.RED);
        btn(c,x,240,w,58,"SCAN HISTORY",RainUI.WHITE);btn(c,x,308,w,58,"LIVE DATA",RainUI.BLUE);
        RainUI.mono(c,p,"DTC TRANSPORT STATUS",x,395,8,RainUI.PURPLE,true);RainUI.text(c,p,"Current vehicle API exposes live sensor data. Actual DTC read/write",x,418,9,RainUI.MUTED,false);RainUI.text(c,p,"commands will appear here when the InTouch transport is verified.",x,434,9,RainUI.MUTED,false);
        RainUI.bottomTag(c,p,W,H,"A CLEAN DRIVE HITS DIFFERENT   ♛");
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(RainUI.navTap(e,1,nav,getWidth()))return true;if(e.getAction()==MotionEvent.ACTION_UP){int x=RainUI.navWidth(getWidth())+18,w=getWidth()-x-18;if(e.getY()>=172&&e.getY()<230){if(e.getX()<x+w/2)listener.onScan();else listener.onClear();}else if(e.getY()<58)nav.home();}return true;}
}
