package com.rainwxrks.q50;

/**
 * Normalized vehicle data shared by the UI and data layer.
 *
 * A field is valid only when its corresponding hasX flag is true.
 * No demo/default vehicle values belong here.
 */
public final class VehicleData {
    public float speedRaw;
    public boolean hasSpeed;

    public float coolantC;
    public boolean hasCoolant;

    public float oilTempC;
    public boolean hasOilTemp;

    public float oilPressureRaw;
    public boolean hasOilPressure;

    public float throttleRaw;
    public boolean hasThrottle;

    public float rpm;
    public boolean hasRpm;

    public float boostPsi;
    public boolean hasBoost;

    public float iatC;
    public boolean hasIat;

    public float voltage;
    public boolean hasVoltage;

    public float mapKpa;
    public boolean hasMap;

    public float driveModeRaw;
    public boolean hasDriveMode;

    public boolean live;

    public void clear() {
        speedRaw = coolantC = oilTempC = oilPressureRaw = throttleRaw = rpm = 0f;
        boostPsi = iatC = voltage = mapKpa = driveModeRaw = 0f;
        hasSpeed = hasCoolant = hasOilTemp = hasOilPressure = hasThrottle = false;
        hasRpm = hasBoost = hasIat = hasVoltage = hasMap = hasDriveMode = false;
        live = false;
    }
}
