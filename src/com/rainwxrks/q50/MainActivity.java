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

/** RainWxrks Q50 V2 main controller. */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sm;
    private GaugeView home;
    private RainCheckView rainCheck;
    private PerformanceView performance;
    private SettingsView settings;
    private DiagnosticManager diagnostics;
    private int boostType=-1, iatType=-1, voltageType=-1;
    private boolean registered=false;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sm=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        diagnostics=new DiagnosticManager(sm);
        showHome();
        registerAll();
    }

    private void showHome(){
        if(home==null) home=new GaugeView(this,new GaugeView.Listener(){
            public void onRainCheck(){showRainCheck();}
            public void onPerformance(){showPerformance();}
            public void onSettings(){showSettings();}
        });
        setContentView(home);
    }
    private void showRainCheck(){
        rainCheck=new RainCheckView(this,new RainCheckView.Listener(){
            public void onBack(){showHome();}
            public void onScan(){String result=diagnostics.scan();rainCheck.setResult(result,0);}
            public void onClear(){rainCheck.setResult(diagnostics.clearCodes(),0);}
        });
        setContentView(rainCheck);
    }
    private void showPerformance(){
        performance=new PerformanceView(this, new PerformanceView.Listener(){public void onBack(){showHome();}});
        setContentView(performance);
    }
    private void showSettings(){
        settings=new SettingsView(this, new SettingsView.Listener(){public void onBack(){showHome();}});
        setContentView(settings);
    }

    private void registerAll(){
        if(sm==null){if(home!=null)home.setStatus("SENSOR_SERVICE = null");return;}
        int n=0;boolean hasVehicleBus=false;
        try{
            List<Sensor> all=sm.getSensorList(Sensor.TYPE_ALL);
            if(all!=null)for(Sensor s:all){
                discover(s);
                if(s.getType()>=12&&s.getType()<=53)hasVehicleBus=true;
                try{if(sm.registerListener(this,s,200000))n++;}catch(Throwable ignored){}
            }
        }catch(Throwable t){if(home!=null)home.setStatus("getSensorList error: "+t);}
        registered=true;
        if(!hasVehicleBus&&home!=null)home.seedDemo();
        else if(home!=null)home.setStatus(n+" CAN signals live");
        updateExtraData();
    }

    private void discover(Sensor s){
        String name=s.getName()==null?"":s.getName().toLowerCase(Locale.US);
        if(name.contains("boost")||name.contains("turbo"))boostType=s.getType();
        if(name.contains("iat")||name.contains("intake air")||name.contains("intake_air"))iatType=s.getType();
        if(name.contains("battery voltage")||name.contains("system voltage")||name.contains("charging voltage"))voltageType=s.getType();
    }

    private void updateExtraData(){
        if(home==null)return;
        boolean hb=boostType>=0&&boostType<64&&home.haveType(boostType);
        boolean hi=iatType>=0&&iatType<64&&home.haveType(iatType);
        boolean hv=voltageType>=0&&voltageType<64&&home.haveType(voltageType);
        float b=hb?home.valueOf(boostType):0f;
        float i=hi?home.valueOf(iatType):0f;
        float v=hv?home.valueOf(voltageType):0f;
        home.setExtraData(b,hb,i,hi,v,hv);
    }

    @Override protected void onResume(){super.onResume();if(sm!=null&&!registered)registerAll();}
    @Override protected void onPause(){super.onPause();try{if(sm!=null)sm.unregisterListener(this);}catch(Throwable ignored){}registered=false;}

    @Override public void onSensorChanged(SensorEvent e){
        try{if(home!=null){float v=(e.values!=null&&e.values.length>0)?e.values[0]:0f;home.setValue(e.sensor.getType(),v);updateExtraData();home.invalidate();}}catch(Throwable ignored){}
    }
    @Override public void onAccuracyChanged(Sensor s,int a){}
}
