package com.rainwxrks.q50;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import java.util.List;

/**
 * Diagnostic transport boundary for V2. The current base exposes CAN values as
 * Android sensors, but does not expose a documented DTC read/clear command.
 * This class deliberately reports that limitation instead of inventing codes.
 */
public class DiagnosticManager {
    private final SensorManager sm;
    public DiagnosticManager(SensorManager manager){sm=manager;}
    public String scan(){
        if(sm==null)return "Sensor service unavailable";
        try{
            List<Sensor> all=sm.getSensorList(Sensor.TYPE_ALL);
            if(all==null||all.isEmpty())return "No vehicle sensors available";
            return "CAN sensor interface detected. DTC transport is not exposed by the current API.";
        }catch(Throwable t){return "Diagnostic scan error: "+t;}
    }
    public String clearCodes(){return "Clear request unavailable: current interface exposes sensor data, not DTC write commands.";}
}
