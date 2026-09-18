package com.rainwxrks.q50;

/**
 * Shared vehicle values used by the V2 screens.
 * Sensor plumbing remains in MainActivity; this class only provides a clean
 * place for the UI to read the latest values.
 */
public final class VehicleData {
    public float speedMph;
    public float boostPsi;
    public float iatC;
    public float coolant;
    public float oilTemp;
    public float oilPressurePsi;
    public float throttle;
    public float voltage;
    public boolean hasSpeed;
    public boolean hasBoost;
    public boolean hasIat;
    public boolean hasCoolant;
    public boolean hasOilTemp;
    public boolean hasOilPressure;
    public boolean hasThrottle;
    public boolean hasVoltage;

    public void reset() {
        speedMph = boostPsi = iatC = coolant = oilTemp = oilPressurePsi = throttle = voltage = 0f;
        hasSpeed = hasBoost = hasIat = hasCoolant = hasOilTemp = hasOilPressure = hasThrottle = hasVoltage = false;
    }
}
