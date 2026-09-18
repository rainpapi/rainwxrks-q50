package com.rainwxrks.q50;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.WindowManager;
import java.util.List;
import java.util.Locale;

/** RainWxrks Q50 V5 controller. */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sm;
    private GaugeView home;
    private RainCheckView rainCheck;
    private PerformanceView performance;
    private SettingsView settings;
    private CameraView camera;
    private DiagnosticManager diagnostics;
    private int boostType=-1, iatType=-1, voltageType=-1, rpmType=-1, driveModeType=-1;
    private boolean registered=false;
    private boolean exhaustEnabled=false;
    private ExhaustSound exhaustSound;\n    private int driveMode=DriveMode.UNKNOWN;\n    private float driveModeRaw=Float.NaN;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sm=(SensorManager)getSystemService(Context.SENSOR_SERVICE); diagnostics=new DiagnosticManager(sm);
        exhaustSound=new ExhaustSound();
        showHome(); registerAll();
    }
    private final RainUI.Listener nav=new RainUI.Listener(){
        public void home(){showHome();} public void rain(){showRainCheck();} public void performance(){showPerformance();}
        public void camera(){showCamera();} public void settings(){showSettings();}
        public void setExhaustEnabled(boolean enabled){exhaustEnabled=enabled;if(exhaustSound!=null)exhaustSound.setEnabled(enabled);}\n        public void setDriveMode(String mode,float raw){if(settings!=null)settings.setDriveMode(mode,raw);}
    };
    private void showHome(){
        if(home==null)home=new GaugeView(this,nav); setContentView(home);
        if(performance!=null)performance.setSpeed(home.valueOf(17)*GaugeView.SPEED_RAW_TO_MPH,home.haveType(17));
    }
    private void showRainCheck(){
        if(rainCheck==null)rainCheck=new RainCheckView(this,nav,new RainCheckView.Listener(){
            public void onScan(){rainCheck.setResult(diagnostics.scan(),0);} public void onClear(){rainCheck.setResult(diagnostics.clearCodes(),0);}
        });
        setContentView(rainCheck);
    }
    private void showPerformance(){if(performance==null)performance=new PerformanceView(this,nav);setContentView(performance);}
    private void showCamera(){if(camera==null)camera=new CameraView(this,nav);setContentView(camera);}
    private void showSettings(){if(settings==null)settings=new SettingsView(this,nav);settings.setDriveMode(DriveMode.name(driveMode),driveModeRaw);setContentView(settings);}

    private void registerAll(){
        if(sm==null)return; int n=0; boolean hasBus=false;
        try{List<Sensor> all=sm.getSensorList(Sensor.TYPE_ALL); if(all!=null)for(Sensor s:all){
            discover(s); if(s.getType()>=12&&s.getType()<=53)hasBus=true;
            try{if(sm.registerListener(this,s,100000))n++;}catch(Throwable ignored){}
        }}catch(Throwable ignored){}
        registered=true;
        if(!hasBus&&home!=null)home.seedDemo(); else if(home!=null)home.setStatus(n+" CAN signals live");
        updateExtraData();
    }
    private void discover(Sensor s){
        String name=s.getName()==null?"":s.getName().toLowerCase(Locale.US);
        if(name.contains("boost")||name.contains("turbo"))boostType=s.getType();
        if(name.contains("iat")||name.contains("intake air")||name.contains("intake_air"))iatType=s.getType();
        if(name.contains("battery voltage")||name.contains("system voltage")||name.contains("charging voltage"))voltageType=s.getType();
        if(name.contains("rpm")||name.contains("engine speed")||name.contains("engine rpm"))rpmType=s.getType();\n        if(name.contains("drive mode")||name.contains("driving mode")||name.contains("infiniti mode"))driveModeType=s.getType();
    }
    private void updateExtraData(){if(home==null)return;
        boolean hb=boostType>=0&&boostType<64&&home.haveType(boostType), hi=iatType>=0&&iatType<64&&home.haveType(iatType), hv=voltageType>=0&&voltageType<64&&home.haveType(voltageType);
        home.setExtraData(hb?home.valueOf(boostType):0f,hb,hi?home.valueOf(iatType):0f,hi,hv?home.valueOf(voltageType):0f,hv);
    }
    @Override protected void onResume(){super.onResume();if(sm!=null&&!registered)registerAll();if(exhaustSound!=null)exhaustSound.setEnabled(exhaustEnabled);}
    @Override protected void onPause(){super.onPause();if(exhaustSound!=null)exhaustSound.setEnabled(false);try{if(sm!=null)sm.unregisterListener(this);}catch(Throwable ignored){}registered=false;}
    @Override public void onSensorChanged(SensorEvent e){
        try{
            float x=(e.values!=null&&e.values.length>0)?e.values[0]:0f;
            if(home!=null)home.setValue(e.sensor.getType(),x);
            updateExtraData();
            if(performance!=null && e.sensor.getType()==17)performance.setSpeed(x*GaugeView.SPEED_RAW_TO_MPH,true);
            if(exhaustSound!=null){
                if(e.sensor.getType()==rpmType)exhaustSound.setRpm(x);
                if(e.sensor.getType()==GaugeView.THROTTLE)exhaustSound.setThrottle(x);
                if(e.sensor.getType()==driveModeType){
                    driveModeRaw=x; driveMode=DriveMode.decode(x);
                    exhaustSound.setModeGain(DriveMode.soundGain(driveMode));
                    if(settings!=null)settings.setDriveMode(DriveMode.name(driveMode),driveModeRaw);
                    if(home!=null)home.setDriveMode(DriveMode.name(driveMode));
                }
            }
            if(performance!=null && e.sensor.getType()==Sensor.TYPE_ACCELEROMETER && e.values!=null && e.values.length>=2){
                float gx=e.values[0]/9.80665f, gy=e.values[1]/9.80665f; performance.setGForce(gx,gy);
            }
            if(home!=null)home.invalidate();
        }catch(Throwable ignored){}
    }
    @Override public void onAccuracyChanged(Sensor s,int a){}
    @Override protected void onDestroy(){if(exhaustSound!=null)exhaustSound.release();super.onDestroy();}
}
