package com.rainwxrks.q50;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Camera presentation screen. The background is a visual fallback until a camera API is verified. */
public class CameraView extends android.view.View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final RainUI.Listener nav; private android.graphics.Bitmap road;
    public CameraView(Context c,RainUI.Listener n){super(c);nav=n;road=RainUI.load(c,com.rainwxrks.q50.R.drawable.camera_bg);}
    @Override protected void onDraw(Canvas c){int W=getWidth(),H=getHeight();RainUI.background(c,road);RainUI.header(c,p,W,"CAMERA","IN CONTROL, ALWAYS.");RainUI.sidebar(c,p,W,H,3);int x=RainUI.navWidth(W)+20,right=W-20;
        RainUI.round(c,p,x,80,right,H-92,14,android.graphics.Color.argb(100,4,5,9),RainUI.EDGE);if(road!=null){p.setAlpha(235);c.drawBitmap(road,null,new RectF(x+8,88,right-8,H-128),p);p.setAlpha(255);}
        RainUI.round(c,p,x+8,H-122,right-8,H-88,9,RainUI.PANEL2,RainUI.EDGE);RainUI.text(c,p,"CAMERA FEED / FALLBACK PREVIEW",x+24,H-99,9,RainUI.MUTED,true);
        RainUI.round(c,p,x+30,H-72,x+150,H-34,9,RainUI.PANEL2,RainUI.EDGE);RainUI.round(c,p,x+158,H-72,x+278,H-34,9,RainUI.PURPLE2,RainUI.PURPLE);RainUI.text(c,p,"REAR",x+72,H-48,9,RainUI.WHITE,true);RainUI.text(c,p,"LIVE",x+200,H-48,9,RainUI.WHITE,true);
        RainUI.mono(c,p,"CAMERA ACCESS DEPENDS ON WHAT THE INTOUCH PLATFORM EXPOSES TO APP GARAGE APPS.",x, H-15,7,RainUI.MUTED,false);
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(RainUI.navTap(e,3,nav,getWidth()))return true;if(e.getAction()==MotionEvent.ACTION_UP&&e.getY()<58)nav.home();return true;}
}
