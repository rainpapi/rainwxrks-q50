package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

/** V3 Rain Way settings shell. */
public class SettingsView extends android.view.View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final RainUI.Listener nav; private android.graphics.Bitmap bg;
    private boolean metric=false,alerts=true,autoStart=true;
    public SettingsView(Context c,RainUI.Listener n){super(c);nav=n;bg=RainUI.load(c,com.rainwxrks.q50.R.drawable.rain_bg);}
    private void row(Canvas c,int x,int y,int w,String label,String value){RainUI.round(c,p,x,y,x+w,y+48,10,RainUI.PANEL2,RainUI.EDGE);RainUI.text(c,p,label,x+18,y+30,11,RainUI.WHITE,true);RainUI.text(c,p,value,x+w-150,y+30,9,RainUI.MUTED,false);RainUI.text(c,p,"›",x+w-24,y+30,18,RainUI.MUTED,false);}
    @Override protected void onDraw(Canvas c){int W=getWidth(),H=getHeight();RainUI.background(c,bg);RainUI.header(c,p,W,"SETTINGS","MAKE IT YOURS.");RainUI.sidebar(c,p,W,H,4);int x=RainUI.navWidth(W)+18,right=W-18,w=right-x;
        row(c,x,78,w,"UNITS",metric?"KM/H / °C":"MPH / °F");row(c,x,136,w,"GAUGE CONFIGURATION","HOME");row(c,x,194,w,"ALERTS & WARNINGS",alerts?"ENABLED":"OFF");row(c,x,252,w,"THEME","RAIN WAY   ♛");row(c,x,310,w,"AUTO-START","APP GARAGE");row(c,x,368,w,"ABOUT","RAINWX RKS  V3");
        RainUI.mono(c,p,"AUTO-START IS CONTROLLED BY THE HEAD UNIT'S APP GARAGE SETTING.",x,438,7,RainUI.MUTED,false);RainUI.bottomTag(c,p,W,H,"SAME STORM. HIGHER STANDARDS.   ♛");
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(RainUI.navTap(e,4,nav,getWidth()))return true;if(e.getAction()==MotionEvent.ACTION_UP){int x=RainUI.navWidth(getWidth())+18,w=getWidth()-x-18;for(int i=0;i<6;i++){float y=78+i*58;if(e.getY()>=y&&e.getY()<y+48){if(i==0)metric=!metric;else if(i==2)alerts=!alerts;else if(i==4)autoStart=!autoStart;invalidate();return true;}}}return true;}
}
