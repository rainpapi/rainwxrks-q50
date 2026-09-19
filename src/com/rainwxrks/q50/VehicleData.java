package com.rainwxrks.q50;

/**
 * Normalized live vehicle data.
 *
 * A field is usable only when its hasX flag is true.
 * There are intentionally no demo/default vehicle values.
 */
public final class VehicleData {
    public float speedRaw;
    public boolean hasSpeed;

    public float coolantC;
    public boolean hasCoolant;

    public float oilTempC;
    public boolean hasOilTemp;

    public float oilPressurePsi;
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

    public float transmissionTempC;
    public boolean hasTransmissionTemp;

    public float fuelPercent;
    public boolean hasFuel;

    public float mapKpa;
    public boolean hasMap;

    public float driveModeRaw;
    public boolean hasDriveMode;

    public boolean live;

    public void clear() {
        speedRaw = coolantC = oilTempC = oilPressurePsi = 0f;
        throttleRaw = rpm = boostPsi = iatC = voltage = 0f;
        transmissionTempC = fuelPercent = mapKpa = driveModeRaw = 0f;

        hasSpeed = hasCoolant = hasOilTemp = hasOilPressure = false;
        hasThrottle = hasRpm = hasBoost = hasIat = false;
        hasVoltage = hasTransmissionTemp = hasFuel = false;
        hasMap = hasDriveMode = false;
        live = false;
    }
}
